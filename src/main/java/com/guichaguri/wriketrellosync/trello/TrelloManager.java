package com.guichaguri.wriketrellosync.trello;

import com.guichaguri.wriketrellosync.Card;
import com.guichaguri.wriketrellosync.ColumnType;
import com.guichaguri.wriketrellosync.ISyncManager;
import com.guichaguri.wriketrellosync.Utils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

public class TrelloManager implements ISyncManager {

    public static final String API_BASE = "https://api.trello.com/1";

    private final String slug, apiKey, apiToken, board;
    private final Map<ColumnType, String> lists;

    public TrelloManager(String slug, String apiKey, String apiToken, String board, Map<ColumnType, String> lists) {
        this.slug = slug;
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        this.board = board;
        this.lists = lists;
    }

    @Override
    public String getSlug() {
        return slug;
    }

    @Override
    public List<Card> getCards() {
        HttpResponse<JsonNode> res = Unirest.get(API_BASE + "/lists/{id}/cards")
                .queryString("cards", "visible") // Filter only cards not archived
                .queryString("filter", "visible")
                .queryString("key", apiKey)
                .queryString("token", apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while retrieving cards from Trello");
        }

        JSONArray array = res.getBody().getArray();
        List<Card> cards = new ArrayList<>();

        for(int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            TrelloCard card = new TrelloCard();

            card.id = obj.getString("id");
            card.name = obj.getString("name");
            card.description = obj.optString("desc");
            card.type = Utils.getColumnType(lists, obj.optString("idList"));
            card.index = obj.optInt("pos");

            cards.add(card);
        }

        Utils.sortAndNormalizeCards(cards);

        return cards;
    }

    @Override
    public Card getCard(String cardId) {
        try {
            // Retrieve all cards instead so we can have the correct task index calculated
            List<Card> cards = getCards();

            for(Card card : cards) {
                if (cardId.equals(card.getId())) return card;
            }

            // Card Removed
            return null;

        } catch(Exception ex) {

            // Card Removed
            return null;

        }
    }

    @Override
    public String addCard(Card card) {
        HttpResponse<JsonNode> res = Unirest.post(API_BASE + "/cards")
                .queryString("name", card.name)
                .queryString("desc", card.description)
                .queryString("idList", lists.get(card.type))
                .queryString("pos", "bottom")
                .queryString("key", apiKey)
                .queryString("token", apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while creating a new Trello card");
        }

        JSONObject data = res.getBody().getObject();
        return data.getString("id");
    }

    @Override
    public void updateCard(String cardId, Card card) {
        HttpResponse<JsonNode> res = Unirest.put(API_BASE + "/cards/{id}")
                .routeParam("id", cardId)
                .queryString("name", card.name)
                .queryString("desc", card.description)
                .queryString("idList", lists.get(card.type))
                .queryString("idBoard", board)
                .queryString("key", apiKey)
                .queryString("token", apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while updating a Trello card\n" + res.getBody().toString());
        }
    }

    @Override
    public void removeCard(String cardId) {
        HttpResponse<JsonNode> res = Unirest.put(API_BASE + "/cards/{id}")
                .routeParam("id", cardId)
                .queryString("closed", true)
                .queryString("key", apiKey)
                .queryString("token", apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while archiving a Trello card");
        }
    }

    @Override
    public String handleWebhook(String request) {
        // Docs on webhooks can be found unofficially on
        // https://github.com/fiatjaf/trello-webhooks

        JSONTokener tokener = new JSONTokener(request);
        JSONObject obj = new JSONObject(tokener);

        // Since trello webhooks are a little bit messy and undocumented
        // We'll just check for the changes by ourselves
        String type = obj.optString("type", null);
        if (type == null || !type.toLowerCase().contains("card")) return null;

        obj = obj.optJSONObject("data");
        if (obj == null) return null;

        obj = obj.optJSONObject("card");
        if (obj == null) return null;

        return obj.optString("id", null);
    }

}
