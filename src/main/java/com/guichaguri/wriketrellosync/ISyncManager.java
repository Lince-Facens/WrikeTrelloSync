package com.guichaguri.wriketrellosync;

import java.util.List;

/**
 * Represents a project management platform
 */
public interface ISyncManager {

    /**
     * The manager's slug. It will be used as a key to store each cards id.
     */
    String getSlug();

    /**
     * Returns a list of all cards inside the configured columns
     */
    List<Card> getCards();

    /**
     * Retrieves a card
     * @param cardId The card id
     * @return The card object or {@code null} if the card doesn't exist
     */
    Card getCard(String cardId);

    /**
     * Creates a card
     * @param card The card to be added
     * @return The card id
     */
    String addCard(Card card);

    /**
     * Updates a card
     * @param cardId The card id to update
     * @param card The card data
     */
    void updateCard(String cardId, Card card);

    /**
     * Removes a card
     * @param cardId The card id to remove
     */
    void removeCard(String cardId);

    /**
     * Handles a webhook
     * @param requestBody
     * @return The card id to investigate
     */
    String handleWebhook(String requestBody);

}
