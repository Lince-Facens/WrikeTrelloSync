package com.guichaguri.wriketrellosync.trello;

import com.guichaguri.wriketrellosync.Card;
import com.guichaguri.wriketrellosync.ISyncManager;
import com.guichaguri.wriketrellosync.Utils;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrelloManager implements ISyncManager {

    public static final String API_BASE = "https://api.trello.com/1";

    private final String slug, apiKey, apiToken, board;
    private final Map<String, String> lists;
    private final Map<String, String> users;

    public TrelloManager(String slug, String apiKey, String apiToken, String board,
                         Map<String, String> lists, Map<String, String> users) {
        this.slug = slug;
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        this.board = board;
        this.lists = lists;
        this.users = users;
    }

    @Override
    public String getSlug() {
        return slug;
    }

    @Override
    public List<Card> getCards() {
        HttpResponse<JsonNode> res = Unirest.get(API_BASE + "/boards/{id}/cards")
                .routeParam("id", board)
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

            String type = Utils.findKey(lists, obj.optString("idList"));

            // We'll ignore cards from unknown lists
            if (type == null) continue;

            TrelloCard card = new TrelloCard();

            card.id = obj.getString("id");
            card.name = obj.getString("name");
            card.description = obj.optString("desc");
            card.type = type;
            card.index = obj.optInt("pos");

            if (obj.has("due"))
                card.dueDate = LocalDate.parse(obj.getString("due"));

            card.dueComplete = obj.optBoolean("dueComplete", false);

            JSONArray members = obj.optJSONArray("idMembers");
            List<String> assignedUsers = new ArrayList<>();
            for(int o = 0; o < members.length(); o++) {
                String userId = Utils.findKey(users, members.getString(o));
                if (userId != null) assignedUsers.add(userId);
            }
            card.assignedUsers = assignedUsers;

            cards.add(card);
        }

        Utils.sortAndNormalizeCards(lists, cards);

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
        if (!lists.containsKey(card.type)) {
            System.out.println("No matching Trello column for " + card.type);
            return null;
        }

        HttpRequestWithBody req = Unirest.post(API_BASE + "/cards")
                .queryString("name", card.name)
                .queryString("desc", card.description)
                .queryString("idList", lists.get(card.type))
                .queryString("pos", "bottom")
                .queryString("dueComplete", card.dueComplete)
                .queryString("idMembers", toMemberIds(card.assignedUsers))
                .queryString("key", apiKey)
                .queryString("token", apiToken);

        if (card.dueDate != null)
            req.queryString("due", card.dueDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        HttpResponse<JsonNode> res = req.asJson();

        if (!res.isSuccess()) {
            JsonNode content = res.getBody();
            throw new RuntimeException("An error occurred while creating a new Trello card: " +
                    (content == null ? res.getStatusText() : content));
        }

        JSONObject data = res.getBody().getObject();
        return data.getString("id");
    }

    @Override
    public void updateCard(String cardId, Card card, Card previous) {
        HttpRequestWithBody req = Unirest.put(API_BASE + "/cards/{id}")
                .routeParam("id", cardId)
                .queryString("name", card.name)
                .queryString("desc", card.description)
                .queryString("dueComplete", card.dueComplete)
                .queryString("idMembers", toMemberIds(card.assignedUsers))
                .queryString("key", apiKey)
                .queryString("token", apiToken);

        if (card.dueDate != null)
            req.queryString("due", card.dueDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        if (lists.containsKey(card.type))
            req.queryString("idList", lists.get(card.type));

        HttpResponse<String> res = req.asString();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while updating a Trello card: " + res.getBody());
        }
    }

    @Override
    public void removeCard(String cardId) {
        HttpResponse<String> res = Unirest.put(API_BASE + "/cards/{id}")
                .routeParam("id", cardId)
                .queryString("closed", true)
                .queryString("key", apiKey)
                .queryString("token", apiToken)
                .asString();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while archiving a Trello card: " + res.getBody());
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

    private String toMemberIds(List<String> userIds) {
        JSONArray array = new JSONArray();

        for(String userId : userIds) {
            if (!users.containsKey(userId)) continue;
            array.put(users.get(userId));
        }

        return array.toString();
    }

}
