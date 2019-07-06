package com.guichaguri.wriketrellosync;

import com.guichaguri.wriketrellosync.history.History;
import com.guichaguri.wriketrellosync.history.HistoryCard;
import kong.unirest.Unirest;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class WrikeTrelloSync {

    private static void processAddedCards(ISyncManager[] managers, ISyncManager manager,
                                          List<HistoryCard> before, List<Card> after) {
        String slug = manager.getSlug();

        for (Card add : after) {
            String id = add.getId();
            boolean found = false;

            for (HistoryCard c : before) {
                String cId = c.ids.getOrDefault(slug, null);

                if (cId != null && cId.equals(id)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Process the addition
                before.add(addCard(managers, manager, add));
            }
        }
    }

    private static HistoryCard addCard(ISyncManager[] managers, ISyncManager manager, Card add) {
        System.out.println("Card Added: " + add.name);

        HistoryCard card = new HistoryCard();
        card.copyFrom(add);
        card.ids.put(manager.getSlug(), add.getId());

        for (ISyncManager manager2 : managers) {
            if (manager2 != manager) {
                card.ids.put(manager2.getSlug(), manager2.addCard(add));
            }
        }

        return card;
    }

    private static void processRemovedCards(ISyncManager[] managers, ISyncManager manager,
                                            List<HistoryCard> before, List<Card> after) {
        String slug = manager.getSlug();
        Iterator<HistoryCard> beforeIt = before.iterator();

        while (beforeIt.hasNext()) {
            HistoryCard c = beforeIt.next();

            String id = c.ids.getOrDefault(slug, null);
            if (id == null) continue;

            boolean found = false;

            for (Card card : after) {
                if (id.equals(card.getId())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Process the deletion
                removeCard(managers, manager, c);
                beforeIt.remove();
            }
        }

    }

    private static void removeCard(ISyncManager[] managers, ISyncManager manager, HistoryCard card) {
        System.out.println("Card Removed: " + card.name);

        // Remove from all managers
        for (ISyncManager manager2 : managers) {
            if (manager2 != manager) {
                String slug2 = manager2.getSlug();

                String id = card.ids.getOrDefault(slug2, null);

                if (id != null) {
                    manager2.removeCard(id);
                }
            }
        }
    }

    private static void processUpdatedCards(ISyncManager[] managers, ISyncManager manager,
                                            List<HistoryCard> history, List<Card> after) {
        String slug = manager.getSlug();

        for (Card card : after) {
            String id = card.getId();

            for (HistoryCard c : history) {
                String historyId = c.ids.getOrDefault(slug, null);

                if (historyId != null && historyId.equals(id)) {

                    // Check whether both columns are not the same
                    if (!c.isEquals(card)) {
                        updateCard(managers, manager, card, c);
                        c.copyFrom(card);
                    }

                    break;
                }
            }
        }

    }

    private static void updateCard(ISyncManager[] managers, ISyncManager manager, Card card, HistoryCard c) {
        System.out.println("Card Updated: " + card.name);

        // Propagate the change to the other managers
        for (ISyncManager manager2 : managers) {
            if (manager2 != manager) {

                String slug2 = manager2.getSlug();

                if (c.ids.containsKey(slug2)) {
                    manager2.updateCard(c.ids.get(slug2), card);
                }

            }
        }
    }

    public static void process(ISyncManager[] managers, History history) {
        for (ISyncManager manager : managers) {
            List<Card> cards = manager.getCards();

            // Add all new cards
            processAddedCards(managers, manager, history.cards, cards);

            // Remove all missing cards
            processRemovedCards(managers, manager, history.cards, cards);

            // Updates all changed cards
            processUpdatedCards(managers, manager, history.cards, cards);
        }
    }

    public static void main(String[] args) throws IOException {
        Unirest.config().enableCookieManagement(false);

        File config = new File("config.json");
        File database = new File("history.json");

        ISyncManager[] managers = null;

        if (config.exists()) {
            managers = Config.load(config);
        }

        if (managers == null || managers.length == 0) {
            managers = Config.wizard(config);
        }

        History history = new History();

        if (database.exists()) {
            history.load(database);
        }

        process(managers, history);

        history.save(database);
    }

}
