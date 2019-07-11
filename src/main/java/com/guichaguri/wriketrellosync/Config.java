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
                        loadColumnMapping(obj.getJSONArray("lists")));

            } else if (type.equals("wrike")) {

                managers[i] = new WrikeManager(slug,
                        obj.getString("apiToken"),
                        obj.getString("folder"),
                        loadColumnMapping(obj.getJSONArray("customStatuses")));

            } else {

                throw new RuntimeException("Unknown type: " + type);

            }
        }

        return managers;
    }

    private static Map<String, String> loadColumnMapping(JSONArray mapping) {
        HashMap<String, String> map = new HashMap<>();

        for(int i = 0; i < mapping.length(); i++) {
            JSONObject obj = mapping.getJSONObject(i);

            String type = obj.getString("type");
            String id = obj.getString("id");

            map.put(type, id);
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

            JSONArray lists = trelloWizardLists(scanner, res.getBody().getArray());

            obj.put("apiKey", key);
            obj.put("apiToken", token);
            obj.put("board", id);
            obj.put("lists", lists);
            break;
        }
    }

    private static JSONArray trelloWizardLists(Scanner scanner, JSONArray array) {
        JSONArray lists = new JSONArray();

        System.out.println("The board was found. Classify each list in the board: (Leave empty to ignore the list)");

        for(int i = 0; i < array.length(); i++) {
            JSONObject list = array.getJSONObject(i);
            System.out.println("Type from list '" + list.getString("name") + "': ");
            String typeName = scanner.nextLine().trim();

            if (typeName.isEmpty()) continue;

            JSONObject item = new JSONObject();
            item.put("type", typeName);
            item.put("id", list.getString("id"));
            lists.put(item);
        }

        return lists;
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
            String workflowId = data.getJSONObject(0).optString("workflowId", null);

            JSONArray statuses = wrikeWizardStatuses(scanner, token, workflowId);

            obj.put("apiToken", token);
            obj.put("folder", folder);
            obj.put("customStatuses", statuses);
            break;
        }
    }

    private static JSONArray wrikeWizardStatuses(Scanner scanner, String token, String workflowId) {
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

        JSONArray statuses = new JSONArray();

        System.out.println("The folder was found. Classify each status in the folder: (Leave it empty to ignore the status)");

        for(int i = 0; i < customStatuses.length(); i++) {
            JSONObject status = customStatuses.getJSONObject(i);
            System.out.println("Type from status '" + status.getString("name") + "': ");
            String typeName = scanner.nextLine().trim();

            if (typeName.isEmpty()) continue;

            JSONObject item = new JSONObject();
            item.put("type", typeName);
            item.put("id", status.getString("id"));
            statuses.put(item);
        }

        return statuses;
    }

}
