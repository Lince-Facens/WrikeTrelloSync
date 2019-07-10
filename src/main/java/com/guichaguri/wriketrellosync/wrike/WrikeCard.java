package com.guichaguri.wriketrellosync.wrike;

import com.guichaguri.wriketrellosync.Card;

public class WrikeCard extends Card {

    public String id;
    public String priority;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int compareTo(Card o) {
        if (o instanceof WrikeCard) {
            return priority.compareTo(((WrikeCard) o).priority);
        }
        return super.compareTo(o);
    }

}
