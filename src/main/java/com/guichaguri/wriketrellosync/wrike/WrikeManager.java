package com.guichaguri.wriketrellosync.wrike;

import com.guichaguri.wriketrellosync.Card;
import com.guichaguri.wriketrellosync.ColumnType;
import com.guichaguri.wriketrellosync.ISyncManager;
import com.guichaguri.wriketrellosync.Utils;
import fi.iki.elonen.NanoHTTPD;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WrikeManager implements ISyncManager {

    public static final String API_BASE = "https://www.wrike.com/api/v4";

    private final String slug, apiToken, folder;
    private final Map<ColumnType, String> customStatuses;

    public WrikeManager(String slug, String apiToken, String folder, Map<ColumnType, String> customStatuses) {
        this.slug = slug;
        this.apiToken = apiToken;
        this.folder = folder;
        this.customStatuses = customStatuses;
    }

    @Override
    public String getSlug() {
        return slug;
    }

    @Override
    public List<Card> getCards() {
        HttpResponse<JsonNode> res = Unirest.get(API_BASE + "/folders/{id}/tasks")
                .routeParam("id", folder)
                .header("Authorization", "Bearer " + apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while retrieving the tasks from Wrike");
        }

        JSONArray array = res.getBody().getObject().getJSONArray("data");
        List<Card> cards = new ArrayList<>();

        for(int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            WrikeCard card = new WrikeCard();

            card.id = obj.getString("id");
            card.name = obj.getString("title");
            card.description = obj.optString("description");
            card.type = Utils.getColumnType(customStatuses, obj.optString("customStatusId"));
            card.priority = obj.optString("priority");
            card.index = i;

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

            // Task Removed
            return null;

        } catch(Exception ex) {

            // Task Removed
            return null;

        }
    }

    @Override
    public String addCard(Card card) {
        HttpResponse<JsonNode> res = Unirest.post(API_BASE + "/folders/{id}/tasks")
                .routeParam("id", folder)
                .field("title", card.name)
                .field("description", card.description)
                .field("customStatus", customStatuses.getOrDefault(card.type, null))
                //.field("responsibles", ) TODO
                .header("Authorization", "Bearer " + apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while adding a card");
        }

        JSONObject data = res.getBody().getObject().getJSONArray("data").getJSONObject(0);

        return data.getString("id");
    }

    @Override
    public void removeCard(String cardId) {
        HttpResponse<JsonNode> res = Unirest.delete(API_BASE + "/tasks/{id}")
                .routeParam("id", cardId)
                .header("Authorization", "Bearer " + apiToken)
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while removing a card");
        }
    }

    @Override
    public void updateCard(String cardId, Card card) {
        HttpResponse<JsonNode> res = Unirest.put(API_BASE + "/tasks/{id}")
                .routeParam("id", cardId)
                .field("title", card.name)
                .field("description", card.description)
                .field("customStatus", customStatuses.getOrDefault(card.type, null))
                .header("Authorization", "Bearer " + apiToken)
                //.field("responsibles", ) TODO
                .asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while updating a card: " + res.getBody().toString());
        }
    }

    @Override
    public String handleWebhook(String request) {
        JSONTokener tokener = new JSONTokener(request);
        JSONObject obj = new JSONObject(tokener);

        String type = obj.optString("eventType", null);
        if (type == null || !type.startsWith("Task")) return null;

        return obj.optString("taskId", null);
    }

}
