package com.guichaguri.wriketrellosync.history;

import com.guichaguri.wriketrellosync.Card;
import org.json.JSONObject;

import java.util.HashMap;

public class HistoryCard extends Card implements IRelationHolder<Card> {

    public static HistoryCard parse(JSONObject obj) {
        HistoryCard card = new HistoryCard();
        card.fromJson(obj);
        return card;
    }

    public HashMap<String, String> ids = new HashMap<>();

    @Override
    protected void fromJson(JSONObject obj) {
        JSONObject idsObj = obj.getJSONObject("ids");
        ids = new HashMap<>();

        for(String key : idsObj.keySet()) {
            ids.put(key, idsObj.get(key).toString());
        }

        super.fromJson(obj);
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = super.toJson();

        obj.put("ids", ids);

        return obj;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getId(String slug) {
        return ids.getOrDefault(slug, null);
    }

    @Override
    public void setId(String slug, String id) {
        ids.put(slug, id);
    }
}
