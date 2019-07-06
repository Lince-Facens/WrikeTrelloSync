package com.guichaguri.wriketrellosync;

import org.json.JSONObject;

import java.util.Objects;

public abstract class Card implements IBoardObject {

    public String name;
    public String description;
    public ColumnType type;
    public int index;

    @Override
    public abstract String getId();

    protected void fromJson(JSONObject obj) {
        name = obj.getString("name");
        description = obj.optString("description");
        index = obj.optInt("index");
        type = obj.optEnum(ColumnType.class, "type");
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
                type == c.type &&
                Objects.equals(name, c.name) &&
                Objects.equals(description, c.description);
    }

}
