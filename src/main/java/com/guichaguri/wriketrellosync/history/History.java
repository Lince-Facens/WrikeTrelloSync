package com.guichaguri.wriketrellosync.history;

import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class History {

    public List<HistoryCard> cards = new ArrayList<>();

    public void load(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONArray array = new JSONArray(tokener);

            for (int i = 0; i < array.length(); i++) {
                cards.add(HistoryCard.parse(array.getJSONObject(i)));
            }
        }
    }

    public void save(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            JSONArray array = new JSONArray();

            for (HistoryCard card : cards) {
                array.put(card.toJson());
            }

            array.write(writer);
        }
    }

}
