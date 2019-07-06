package com.guichaguri.wriketrellosync;

import com.guichaguri.wriketrellosync.history.History;
import com.guichaguri.wriketrellosync.history.HistoryCard;
import com.guichaguri.wriketrellosync.history.IRelationHolder;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class WrikeTrelloSync {

    private static <T extends IRelationHolder<Y>, Y extends IBoardObject> void processAdded(
            String slug, List<T> before, List<Y> after, Function<Y, T> addFunc) {

        for (Y column : after) {
            String id = column.getId();
            boolean found = false;

            for (T c : before) {
                String cId = c.getId(slug);

                if (cId != null && cId.equals(id)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Process the addition
                before.add(addFunc.apply(column));
            }
        }
    }

    private static <T extends IRelationHolder<Y>, Y extends IBoardObject> void processRemoved(
            String slug, List<T> before, List<Y> after, Consumer<T> removeFunc) {

        Iterator<T> beforeIt = before.iterator();

        while (beforeIt.hasNext()) {
            T c = beforeIt.next();

            String id = c.getId(slug);
            if (id == null) continue;

            boolean found = false;

            for (Y column : after) {
                if (id.equals(column.getId())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                removeFunc.accept(c);
                beforeIt.remove();
            }
        }

    }

    private static <T extends IRelationHolder<Y>, Y extends IBoardObject> void processUpdated(
            String slug, List<T> history, List<Y> after, BiConsumer<Y, T> updateFunc) {

        for (Y column : after) {
            String id = column.getId();

            for (T c : history) {
                String historyId = c.getId(slug);

                if (historyId != null && historyId.equals(id)) {

                    // Check whether both columns are not the same
                    if (!c.isEquals(column)) {
                        updateFunc.accept(column, c);
                    }

                    break;
                }
            }
        }

    }

    private static void processAddedCards(ISyncManager[] managers, ISyncManager manager,
                                          List<HistoryCard> history, List<Card> cards) {

        String slug = manager.getSlug();

        processAdded(slug, history, cards, (Card add) -> {

            System.out.println("Card Added: " + add.name);

            HistoryCard card = new HistoryCard();
            card.copyFrom(add);
            card.ids.put(slug, add.getId());

            for (ISyncManager manager2 : managers) {
                if (manager2 != manager) {
                    String slug2 = manager2.getSlug();
                    String id = manager2.addCard(add);

                    card.ids.put(slug2, id);
                }
            }

            return card;

        });
    }

    private static void processRemovedCards(ISyncManager[] managers, ISyncManager manager,
                                            List<HistoryCard> history, List<Card> columns) {

        String slug = manager.getSlug();

        processRemoved(slug, history, columns, (HistoryCard card) -> {

            System.out.println("Card Removed: " + card.name);

            // Remove from all managers
            for (ISyncManager manager2 : managers) {
                if (manager2 != manager) {
                    String slug2 = manager2.getSlug();

                    String id = card.getId(slug2);

                    if (id != null) {
                        manager2.removeCard(id);
                    }
                }
            }

        });
    }

    private static void processUpdatedCards(ISyncManager[] managers, ISyncManager manager,
                                            List<HistoryCard> history, List<Card> columns) {

        String slug = manager.getSlug();

        processUpdated(slug, history, columns, (Card card, HistoryCard c) -> {

            System.out.println("Card Updated: " + card.name);

            c.copyFrom(card);

            // Propagate the change to the other managers
            for (ISyncManager manager2 : managers) {
                if (manager2 != manager) {

                    String slug2 = manager2.getSlug();

                    if (c.ids.containsKey(slug2)) {
                        manager2.updateCard(c.ids.get(slug2), card);
                    }

                }
            }

        });

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

}
