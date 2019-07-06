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
            card.index = obj.optInt("pos", i);

            cards.add(card);
        }

        Utils.sortAndNormalizeCards(cards);

        return cards;
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
}
