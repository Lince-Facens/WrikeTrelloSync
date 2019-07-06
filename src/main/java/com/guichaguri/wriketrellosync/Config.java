package com.guichaguri.wriketrellosync;

import com.guichaguri.wriketrellosync.trello.TrelloManager;
import com.guichaguri.wriketrellosync.wrike.WrikeManager;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

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

    private static Map<ColumnType, String> loadColumnMapping(JSONArray mapping) {
        HashMap<ColumnType, String> map = new HashMap<>();

        for(int i = 0; i < mapping.length(); i++) {
            JSONObject obj = mapping.getJSONObject(i);

            ColumnType type = obj.getEnum(ColumnType.class, "type");
            String id = obj.getString("id");

            map.put(type, id);
        }

        return map;
    }

    public static ISyncManager[] wizard(File file) {
        Scanner scanner = new Scanner(System.in);
        JSONArray array = new JSONArray();
        char c;

        do {
            JSONObject obj = new JSONObject();

            System.out.println("Which project management platform you want to configure? (trello/wrike) ");
            String type = scanner.next("trello|wrike").toLowerCase();
            obj.put("type", type);

            System.out.println("Choose a unique, short slug for this platform. It'll be the identifier. ");
            obj.put("slug", scanner.nextLine().trim().toLowerCase());

            if (type.equals("trello")) {

                trelloWizard(scanner, obj);

            } else if (type.equals("wrike")) {

                System.out.println("Wrike API Token: ");
                obj.put("apiToken", scanner.nextLine().trim());


            }

            System.out.println("Configure one more platform? (Y/N) ");
            c = scanner.next("[YNyn]").toUpperCase().charAt(0);

        } while (array.length() < 2 || c == 'Y');

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

            ColumnType[] types = ColumnType.values();

            System.out.print("The board was found. Classify each list in the board: ");
            System.out.print("(Ignore");

            for(ColumnType type : types) {
                System.out.print(", " + type.name());
            }

            System.out.println(")");

            JSONArray array = res.getBody().getArray();
            JSONArray lists = new JSONArray();

            for(int i = 0; i < array.length();) {
                JSONObject list = array.getJSONObject(i);
                System.out.println("Type of " + list.getString("name") + ": ");
                String typeName = scanner.nextLine().trim();

                if (typeName.equalsIgnoreCase("Ignore")) {
                    i++;
                    continue;
                }

                for(ColumnType type : types) {
                    if (type.name().equalsIgnoreCase(typeName)) {
                        JSONObject item = new JSONObject();
                        item.put("type", type);
                        item.put("id", list.getString("id"));
                        lists.put(item);
                        i++;
                        break;
                    }
                }

            }

            obj.put("apiKey", key);
            obj.put("apiToken", token);
            obj.put("board", id);
            obj.put("lists", lists);
            break;
        }
    }

}
