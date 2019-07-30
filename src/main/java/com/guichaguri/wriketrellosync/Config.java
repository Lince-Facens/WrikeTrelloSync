package com.guichaguri.wriketrellosync;

import com.guichaguri.wriketrellosync.trello.TrelloManager;
import com.guichaguri.wriketrellosync.wrike.WrikeManager;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    /**
     * Loads the configuration file
     */
    public static ISyncManager[] load(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JSONTokener tokener = new JSONTokener(reader);

            return loadManagers(new JSONArray(tokener));
        }
    }

    private static ISyncManager[] loadManagers(JSONArray array) {
        ISyncManager[] managers = new ISyncManager[array.length()];

        for(int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            String type = obj.getString("type");
            String slug = obj.getString("slug");

            if (type.equals("trello")) {

                managers[i] = new TrelloManager(slug,
                        obj.getString("apiKey"),
                        obj.getString("apiToken"),
                        obj.getString("board"),
                        loadMapping(obj.getJSONObject("lists")),
                        loadMapping(obj.getJSONObject("users")));

            } else if (type.equals("wrike")) {

                managers[i] = new WrikeManager(slug,
                        obj.getString("apiToken"),
                        obj.getString("folder"),
                        loadMapping(obj.getJSONObject("customStatuses")),
                        loadMapping(obj.getJSONObject("contacts")));

            } else {

                throw new RuntimeException("Unknown type: " + type);

            }
        }

        return managers;
    }

    private static Map<String, String> loadMapping(JSONObject mapping) {
        HashMap<String, String> map = new HashMap<>();

        for(String id : mapping.keySet()) {
            map.put(id, mapping.getString(id));
        }

        return map;
    }

    /**
     * Runs a configuration wizard
     */
    public static ISyncManager[] wizard(File file) throws IOException {
        Scanner scanner = new Scanner(System.in);
        JSONArray array = new JSONArray();
        char c = 'Y';

        do {
            JSONObject obj = new JSONObject();

            System.out.println("Which project management platform you want to configure? (trello/wrike) ");
            String type = scanner.nextLine().trim().toLowerCase();
            obj.put("type", type);

            System.out.println("Choose a unique, short slug for this platform. It'll be used as the internal identifier. ");
            obj.put("slug", scanner.nextLine().trim().toLowerCase());

            if (type.equals("trello")) {

                trelloWizard(scanner, obj);

            } else if (type.equals("wrike")) {

                wrikeWizard(scanner, obj);

            } else {

                System.out.println("Platform type '" + type + "' is invalid!");
                continue;

            }

            array.put(obj);

            if (array.length() >= 2) {
                System.out.println("Configure one more platform? (Y/N) ");
                c = scanner.nextLine().toUpperCase().charAt(0);
            }

        } while (c == 'Y');

        try(FileWriter writer = new FileWriter(file)) {
            array.write(writer);
        }

        return loadManagers(array);
    }

    private static void trelloWizard(Scanner scanner, JSONObject obj) {
        Pattern url = Pattern.compile("trello\\.com\\/b\\/(\\w+)\\/");

        while(true) {
            System.out.println("Trello API Key: ");
            String key = scanner.nextLine().trim();

            System.out.println("Trello API Token: ");
            String token = scanner.nextLine().trim();

            System.out.println("Trello Board ID or URL: ");
            String id = scanner.nextLine().trim();

            Matcher m = url.matcher(id);
            if (m.find()) id = m.group(1);

            System.out.println("Validating...");

            HttpResponse<JsonNode> res = Unirest.get(TrelloManager.API_BASE + "/boards/{id}/lists")
                    .routeParam("id", id)
                    .queryString("key", key)
                    .queryString("token", token)
                    .queryString("filter", "open")
                    .queryString("cards", "none")
                    .asJson();

            if (!res.isSuccess()) {
                System.out.println("An error occurred: " + res.getBody().toString());
                continue;
            }

            JSONObject lists = trelloWizardLists(scanner, res.getBody().getArray());

            System.out.println("Fetching users...");

            res = Unirest.get(TrelloManager.API_BASE + "/boards/{id}/members")
                    .routeParam("id", id)
                    .queryString("key", key)
                    .queryString("token", token)
                    .asJson();

            if (!res.isSuccess()) {
                System.out.println("An error occurred: " + res.getBody().toString());
                continue;
            }

            JSONObject users = trelloWizardUsers(scanner, res.getBody().getArray());

            obj.put("apiKey", key);
            obj.put("apiToken", token);
            obj.put("board", id);
            obj.put("lists", lists);
            obj.put("users", users);
            break;
        }
    }

    private static JSONObject trelloWizardLists(Scanner scanner, JSONArray array) {
        JSONObject lists = new JSONObject();

        System.out.println("The board was found. Classify each list in the board: (Leave empty to ignore the list)");

        for(int i = 0; i < array.length(); i++) {
            JSONObject list = array.getJSONObject(i);
            System.out.println("Type from list '" + list.getString("name") + "': ");
            String typeName = scanner.nextLine().trim();

            if (typeName.isEmpty()) continue;

            lists.put(typeName, list.getString("id"));
        }

        return lists;
    }

    private static JSONObject trelloWizardUsers(Scanner scanner, JSONArray array) {
        JSONObject users = new JSONObject();

        System.out.println("Give each user a unique username. (It has to match between platforms to sync correctly)");
        System.out.println("Leave it empty to skip a user");

        for(int i = 0; i < array.length(); i++) {
            JSONObject data = array.getJSONObject(i);
            System.out.println("Username for '" + data.getString("fullName") + "': ");
            String username = scanner.nextLine().trim();

            if (username.isEmpty()) continue;

            users.put(username, data.getString("id"));
        }

        return users;
    }

    private static void wrikeWizard(Scanner scanner, JSONObject obj) {
        while(true) {
            System.out.println("Wrike API Token: ");
            String token = scanner.nextLine().trim();

            System.out.println("Validating...");

            HttpResponse<JsonNode> res = Unirest.get(WrikeManager.API_BASE + "/folders")
                    .header("Authorization", "Bearer " + token)
                    .asJson();

            if (!res.isSuccess()) {
                System.out.println("An error occurred: " + res.getBody().toString());
                continue;
            }

            System.out.println("\nFolder ID List\n");

            JSONArray data = res.getBody().getObject().getJSONArray("data");

            // Lists all folders
            for(int i = 0; i < data.length(); i++) {
                JSONObject folderObj = data.getJSONObject(i);
                if (!folderObj.getJSONArray("childIds").isEmpty()) continue;
                System.out.println(folderObj.getString("id") + "\t" + folderObj.getString("title"));
            }

            System.out.println("\nWrike Folder ID: ");
            String folder = scanner.nextLine().trim();

            System.out.println("Validating...");

            // Query the folder
            res = Unirest.get(WrikeManager.API_BASE + "/folders/{id}")
                    .routeParam("id", folder)
                    .header("Authorization", "Bearer " + token)
                    .asJson();

            if (!res.isSuccess()) {
                System.out.println("An error occurred: " + res.getBody().toString());
                continue;
            }

            data = res.getBody().getObject().getJSONArray("data");
            JSONObject folderData = data.getJSONObject(0);

            String workflowId = folderData.optString("workflowId", null);

            JSONObject statuses = wrikeWizardStatuses(scanner, token, workflowId);
            JSONObject contacts = wrikeWizardContacts(scanner, token, folderData.getJSONArray("sharedIds"));

            obj.put("apiToken", token);
            obj.put("folder", folder);
            obj.put("customStatuses", statuses);
            obj.put("contacts", contacts);
            break;
        }
    }

    private static JSONObject wrikeWizardStatuses(Scanner scanner, String token, String workflowId) {
        HttpResponse<JsonNode> res = Unirest.get(WrikeManager.API_BASE + "/workflows")
                .header("Authorization", "Bearer " + token)
                .asJson();

        JSONArray workflows = res.getBody().getObject().getJSONArray("data");
        JSONArray customStatuses = null;

        for(int i = 0; i < workflows.length(); i++) {
            JSONObject workflow = workflows.getJSONObject(i);
            if (!workflow.getString("id").equals(workflowId)) continue;

            customStatuses = workflow.getJSONArray("customStatuses");
            break;
        }

        JSONObject statuses = new JSONObject();

        System.out.println();
        System.out.println("The folder was found. Classify each status in the folder: (Leave it empty to ignore the status)");

        for(int i = 0; i < customStatuses.length(); i++) {
            JSONObject status = customStatuses.getJSONObject(i);
            System.out.println("Type from status '" + status.getString("name") + "': ");
            String typeName = scanner.nextLine().trim();

            if (typeName.isEmpty()) continue;

            statuses.put(typeName, status.getString("id"));
        }

        return statuses;
    }

    private static JSONObject wrikeWizardContacts(Scanner scanner, String token, JSONArray array) {
        JSONObject contacts = new JSONObject();

        if (array.isEmpty()) return contacts;

        System.out.println();
        System.out.println("Fetching...");

        StringBuilder ids = new StringBuilder();

        for(int i = 0; i < array.length(); i++) {
            if (ids.length() > 0) ids.append(',');
            ids.append(array.getString(i));
        }

        HttpResponse<JsonNode> res = Unirest.get(WrikeManager.API_BASE + "/contacts/{ids}")
                .routeParam("ids", ids.toString())
                .header("Authorization", "Bearer " + token)
                .asJson();

        if (!res.isSuccess()) {
            System.out.println("An error occurred while fetching the users: " + res.getStatusText());
            return contacts;
        }

        System.out.println("Give each user a unique username. (It has to match between platforms to sync correctly)");
        System.out.println("Leave it empty to skip a user");

        array = res.getBody().getArray();

        for(int i = 0; i < array.length(); i++) {
            JSONObject data = array.getJSONObject(i);
            System.out.println("Username for '" + data.getString("firstName") + " " + data.getString("lastName") + "': ");
            String username = scanner.nextLine().trim();

            if (username.isEmpty()) continue;

            contacts.put(username, data.getString("id"));
        }

        return contacts;
    }

}
