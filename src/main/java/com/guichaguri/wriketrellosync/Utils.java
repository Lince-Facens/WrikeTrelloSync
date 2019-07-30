package com.guichaguri.wriketrellosync;

import org.json.JSONArray;

import java.util.*;

public class Utils {

    public static final String CONFIG_FILE = "config.json";
    public static final String DATABASE_FILE = "history.json";

    public static String findKey(Map<String, String> map, String id) {
        for(String type : map.keySet()) {
            if (id.equals(map.get(type))) return type;
        }
        return null;
    }

    public static void sortAndNormalizeCards(Map<String, String> columns, List<Card> cards) {
        Collections.sort(cards);

        for(String type : columns.keySet()) {
            int i = 0;
            for(Card c : cards) {
                if (type == c.type) {
                    c.index = i++;
                }
            }
        }
    }

    public static JSONArray toJsonArray(List<String> list) {
        JSONArray array = new JSONArray();
        if (list == null) return array;

        for(String item : list) {
            array.put(item);
        }
        return array;
    }

    public static List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;

        for(int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }


}
