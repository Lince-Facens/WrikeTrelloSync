package com.guichaguri.wriketrellosync;

import java.util.*;

public class Utils {

    public static final String CONFIG_FILE = "config.json";
    public static final String DATABASE_FILE = "history.json";

    public static String getColumnType(Map<String, String> map, String id) {
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

}
