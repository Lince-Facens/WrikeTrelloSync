package com.guichaguri.wriketrellosync.wrike;

import com.guichaguri.wriketrellosync.Card;
import com.guichaguri.wriketrellosync.ISyncManager;
import com.guichaguri.wriketrellosync.Utils;
import kong.unirest.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WrikeManager implements ISyncManager {

    public static final String API_BASE = "https://www.wrike.com/api/v4";

    private final String slug, apiToken, folder;
    private final Map<String, String> customStatuses;
    private final Map<String, String> users;

    public WrikeManager(String slug, String apiToken, String folder,
                        Map<String, String> customStatuses, Map<String, String> users) {
        this.slug = slug;
        this.apiToken = apiToken;
        this.folder = folder;
        this.customStatuses = customStatuses;
        this.users = users;
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

            String type = Utils.findKey(customStatuses, obj.optString("customStatusId"));

            // We'll ignore cards from unknown lists
            if (type == null) continue;

            WrikeCard card = new WrikeCard();

            card.id = obj.getString("id");
            card.name = obj.getString("title");
            card.description = obj.optString("description");
            card.type = type;
            card.priority = obj.optString("priority");
            card.index = i;

            JSONObject dates = obj.optJSONObject("dates");
            if (dates != null) {
                card.dueDate = LocalDate.parse(dates.getString("due"));
            }

            card.dueComplete = obj.optString("status", "Active").equals("Completed");

            JSONArray responsible = obj.optJSONArray("responsibleIds");
            List<String> assignedUsers = new ArrayList<>();
            for(int o = 0; o < responsible.length(); o++) {
                String userId = Utils.findKey(users, responsible.getString(o));
                if (userId != null) assignedUsers.add(userId);
            }
            card.assignedUsers = assignedUsers;

            cards.add(card);
        }

        Utils.sortAndNormalizeCards(customStatuses, cards);

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
        MultipartBody req = Unirest.post(API_BASE + "/folders/{id}/tasks")
                .routeParam("id", folder)
                .field("title", card.name)
                .field("description", card.description)
                .field("customStatus", customStatuses.getOrDefault(card.type, ""))
                .header("Authorization", "Bearer " + apiToken);

        if (card.dueComplete)
            req.field("status", "Completed");

        if (card.dueDate != null)
            req.field("dates", toDatesObject(card.dueDate));

        req.field("addResponsibles", toResponsibleIds(card.assignedUsers));

        HttpResponse<JsonNode> res = req.asJson();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while adding a card");
        }

        JSONObject data = res.getBody().getObject().getJSONArray("data").getJSONObject(0);

        return data.getString("id");
    }

    @Override
    public void removeCard(String cardId) {
        HttpResponse<String> res = Unirest.delete(API_BASE + "/tasks/{id}")
                .routeParam("id", cardId)
                .header("Authorization", "Bearer " + apiToken)
                .asString();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while removing a card: " + res.getBody());
        }
    }

    @Override
    public void updateCard(String cardId, Card card, Card previous) {
        MultipartBody req = Unirest.put(API_BASE + "/tasks/{id}")
                .routeParam("id", cardId)
                .field("title", card.name)
                .field("description", card.description)
                .field("customStatus", customStatuses.getOrDefault(card.type, ""))
                .header("Authorization", "Bearer " + apiToken);

        if (card.dueComplete)
            req.field("status", "Completed");

        if (card.dueDate != null)
            req.field("dates", toDatesObject(card.dueDate));

        List<String> addedUsers = new ArrayList<>();
        List<String> removedUsers = new ArrayList<>();

        for(String user : card.assignedUsers) {
            if (!previous.assignedUsers.contains(user)) {
                // User Added
                addedUsers.add(user);
            }
        }

        for(String user : previous.assignedUsers) {
            if (!card.assignedUsers.contains(user)) {
                // User Removed
                removedUsers.add(user);
            }
        }

        req.field("addResponsibles", toResponsibleIds(addedUsers));
        req.field("removeResponsibles", toResponsibleIds(removedUsers));

        HttpResponse<String> res = req.asString();

        if (!res.isSuccess()) {
            throw new RuntimeException("An error occurred while updating a card: " + res.getBody());
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

    private String toDatesObject(LocalDate due) {
        JSONObject obj = new JSONObject();
        obj.put("due", due.format(DateTimeFormatter.ISO_DATE));
        obj.put("type", "Milestone");
        return obj.toString();
    }

    private String toResponsibleIds(List<String> userIds) {
        JSONArray array = new JSONArray();

        for(String userId : userIds) {
            if (!users.containsKey(userId)) continue;
            array.put(users.get(userId));
        }

        return array.toString();
    }

}
