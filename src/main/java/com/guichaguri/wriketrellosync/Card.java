package com.guichaguri.wriketrellosync;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Card implements Comparable<Card> {

    public String name;
    public String description;
    public String type;
    public int index;
    public LocalDate dueDate;
    public boolean dueComplete;
    public List<String> assignedUsers = new ArrayList<>();

    public abstract String getId();

    protected void fromJson(JSONObject obj) {
        name = obj.getString("name");
        description = obj.optString("description");
        index = obj.optInt("index");
        type = obj.optString("type");
        assignedUsers = Utils.toStringList(obj.optJSONArray("users"));

        if (obj.has("dueDate")) {
            dueDate = LocalDate.ofEpochDay(obj.getLong("dueDate"));
        } else {
            dueDate = null;
        }

        dueComplete = obj.optBoolean("dueComplete", false);
    }

    protected JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.put("name", name);
        obj.put("description", description);
        obj.put("index", index);
        obj.put("type", type);
        obj.put("dueDate", dueDate == null ? null : dueDate.toEpochDay());
        obj.put("dueComplete", dueComplete);
        obj.put("users", Utils.toJsonArray(assignedUsers));

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
                dueComplete == c.dueComplete &&
                Objects.equals(name, c.name) &&
                Objects.equals(description, c.description) &&
                Objects.equals(type, c.type) &&
                Objects.equals(dueDate, c.dueDate) &&
                assignedUsers.equals(c.assignedUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, index, dueDate, dueComplete, assignedUsers);
    }

    @Override
    public int compareTo(Card o) {
        return Integer.compare(index, o.index);
    }

}
