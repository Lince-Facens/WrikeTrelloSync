package com.guichaguri.wriketrellosync;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Utils {

    public static ColumnType getColumnType(Map<ColumnType, String> map, String id) {
        for(ColumnType type : map.keySet()) {
            if (id.equals(map.get(type))) return type;
        }
        return null;
    }

    public static void sortAndNormalizeCards(List<Card> cards) {
        cards.sort(Comparator.comparingInt(c -> c.index));

        for(ColumnType type : ColumnType.values()) {
            int i = 0;
            for(Card c : cards) {
                if (type == c.type) {
                    c.index = i++;
                }
            }
        }
    }

}
