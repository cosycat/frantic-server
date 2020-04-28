package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.entity.Card;
import ch.uzh.ifi.seal.soprafs20.entity.Chat;
import ch.uzh.ifi.seal.soprafs20.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeEvent implements Event {

    private final List<Player> listOfPlayers;

    public EarthquakeEvent(List<Player> listOfPlayers) {
        this.listOfPlayers = listOfPlayers;
    }

    public String getName() {
        return "earthquake";
    }

    public List<Chat> performEvent() {
        List<Card> temp = new ArrayList<>();
        int tempSize = this.listOfPlayers.get(0).getHandSize();
        for (int i = 0; i < tempSize; i++) {
            temp.add(this.listOfPlayers.get(0).popCard());
        }

        int numOfPlayers = this.listOfPlayers.size();
        for (int i = numOfPlayers - 1; i > 0; i--) {
            Player fromPlayer = this.listOfPlayers.get(i);
            int toPlayerIndex = (i + 1) % numOfPlayers;
            Player toPlayer = this.listOfPlayers.get(toPlayerIndex);

            int numOfCards = fromPlayer.getHandSize();
            for (int j = 0; j < numOfCards; j++) {
                toPlayer.pushCardToHand(fromPlayer.popCard());
            }
        }

        for (int i = 0; i < tempSize; i++) {
            this.listOfPlayers.get(1).pushCardToHand(temp.remove(0));
        }
        List<Chat> chat = new ArrayList<>();
        chat.add(new Chat("event", "event:earthquake", this.getMessage()));
        return chat;
    }

    public String getMessage() {
        return "Oh no! Everything is shaken up! Good luck with your new cards!";
    }
}
