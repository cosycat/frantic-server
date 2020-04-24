package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.entity.*;

import java.util.List;

public class TornadoEvent implements Event {

    private final List<Player> listOfPlayers;
    private final Pile<Card> tornadoStack;

    public TornadoEvent(GameRound round) {
        this.listOfPlayers = round.getListOfPlayers();
        this.tornadoStack = new DiscardPile();
    }

    public String getName() {
        return "tornado";
    }

    public void performEvent() {
        // collect cards
        for (Player player : this.listOfPlayers) {
            if (player.getHandSize() != 0) {
                this.tornadoStack.push(player.popCard());
            }
        }
        this.tornadoStack.shuffle();

        // redistribute cards
        int i = 0;
        while (this.tornadoStack.size() != 0) {
            this.listOfPlayers.get(i).pushCardToHand(this.tornadoStack.pop());
            int m = ++i % this.listOfPlayers.size();
        }
    }

    public String getMessage() {
        return "Oh no! A tornado whirled all the cards around! Looks like we have to redistribute them!";
    }
}
