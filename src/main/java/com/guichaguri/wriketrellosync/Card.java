package com.guichaguri.wriketrellosync;

import org.json.JSONObject;

import java.util.Objects;

public abstract class Card implements Comparable<Card> {

    public String name;
    public String description;
    public String type;
    public int index;

    public abstract String getId();

    protected void fromJson(JSONObject obj) {
        name = obj.getString("name");
        description = obj.optString("description");
        index = obj.optInt("index");
        type = obj.optString("type");
    }

    protected JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.put("name", name);
        obj.put("description", description);
        obj.put("index", index);
        obj.put("type", type);

        return obj;
    }

    public void copyFrom(Card card) {
        name = card.name;
        description = card.description;
        index = card.index;
        type = card.type;
    }

    public boolean isEquals(Card c) {
        if (c == this) return true;
        if (c == null) return false;

        return index == c.index &&
                type.equals(c.type) &&
                Objects.equals(name, c.name) &&
                Objects.equals(description, c.description);
    }

    @Override
    public int compareTo(Card o) {
        return Integer.compare(index, o.index);
    }

}
