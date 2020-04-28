package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.entity.Card;
import ch.uzh.ifi.seal.soprafs20.entity.Chat;
import ch.uzh.ifi.seal.soprafs20.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RobinHoodEvent implements Event {

    private final List<Player> listOfPlayers;
    private final Player currentPlayer;

    public RobinHoodEvent(List<Player> listOfPlayers, Player currentPlayer) {
        this.listOfPlayers = listOfPlayers;
        this.currentPlayer = currentPlayer;
    }

    public String getName() {
        return "robin-hood";
    }

    public List<Chat> performEvent() {
        int numOfPlayers = this.listOfPlayers.size();
        int currentPlayerIndex = this.listOfPlayers.indexOf(currentPlayer);
        int nextPlayerIndex = (currentPlayerIndex + 1) % numOfPlayers;

        //if two player has equal number of cards, the one closer to the current player is affected
        Player minCardsPlayer = this.listOfPlayers.get(nextPlayerIndex);
        Player maxCardsPlayer = this.listOfPlayers.get(nextPlayerIndex);

        for (int i = 1; i < numOfPlayers; i++) {
            Player playerOfInterest = this.listOfPlayers.get((nextPlayerIndex + i) % numOfPlayers);
            if (playerOfInterest.getHandSize() > maxCardsPlayer.getHandSize()) {
                maxCardsPlayer = playerOfInterest;
            }
            if (playerOfInterest.getHandSize() < minCardsPlayer.getHandSize()) {
                minCardsPlayer = playerOfInterest;
            }
        }

        if (!minCardsPlayer.equals(maxCardsPlayer)) {
            List<Card> temp = new ArrayList<>();
            int maxCards = maxCardsPlayer.getHandSize();
            int minCards = minCardsPlayer.getHandSize();

            for (int i = 0; i < maxCards; i++) {
                temp.add(maxCardsPlayer.popCard());
            }
            for (int i = 0; i < minCards; i++) {
                maxCardsPlayer.pushCardToHand(minCardsPlayer.popCard());
            }
            int tempSize = temp.size();
            for (int i = 0; i < tempSize; i++) {
                minCardsPlayer.pushCardToHand(temp.remove(0));
            }
        }
        List<Chat> chat = new ArrayList<>();
        chat.add(new Chat("event", "event:robin-hood", this.getMessage()));
        chat.add(new Chat("event", "event:robin-hood", maxCardsPlayer.getUsername() + " and " + minCardsPlayer.getUsername() + " swapped cards"));
        return chat;
    }

    public String getMessage() {
        return "Some call him a hero, some call him a thief! The player with the least cards has to swap cards with the player holding the most!";
    }
}
