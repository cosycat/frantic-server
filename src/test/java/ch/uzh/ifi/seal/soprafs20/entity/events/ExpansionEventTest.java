package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.Card;
import ch.uzh.ifi.seal.soprafs20.entity.DrawStack;
import ch.uzh.ifi.seal.soprafs20.entity.Pile;
import ch.uzh.ifi.seal.soprafs20.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpansionEventTest {

    private List<Player> listOfPlayers = new ArrayList<>();
    private final Pile<Card> drawStack = new DrawStack();

    @Test
    public void getNameTest() {
        ExpansionEvent expansion = new ExpansionEvent(this.listOfPlayers, new Player(), this.drawStack);
        assertEquals("expansion", expansion.getName());
    }

    @Test
    public void getMessageTest() {
        ExpansionEvent expansion = new ExpansionEvent(this.listOfPlayers, new Player(), this.drawStack);
        assertEquals("Cards are selling like hot cakes! Grab one or two or three ...", expansion.getMessage());
    }

    @Test
    public void performEventTest() {
        Player player1 = new Player();
        player1.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 0));
        player1.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 1));
        player1.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 2));
        player1.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 3));
        this.listOfPlayers.add(player1);

        Player player2 = new Player();
        player2.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 4));
        player2.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 5));
        this.listOfPlayers.add(player2);

        Player player3 = new Player();
        player3.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 6));
        player3.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 7));
        player3.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 8));
        this.listOfPlayers.add(player3);

        Player player4 = new Player();
        player4.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 9));
        player4.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 10));
        player4.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 11));
        player4.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 12));
        player4.pushCardToHand(new Card(Color.GREEN, Type.NUMBER, Value.EIGHT, false, 13));
        this.listOfPlayers.add(player4);

        ExpansionEvent e = new ExpansionEvent(this.listOfPlayers, player2, this.drawStack);
        e.performEvent();

        assertEquals(7, player1.getHandSize());
        assertEquals(6, player2.getHandSize());
        assertEquals(4, player3.getHandSize());
        assertEquals(7, player4.getHandSize());
    }
}