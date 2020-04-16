package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.Card;
import ch.uzh.ifi.seal.soprafs20.entity.DiscardPile;
import ch.uzh.ifi.seal.soprafs20.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FantasticActionTest {

    @Test
    public void wishColor() {
        DiscardPile discardPile = new DiscardPile();
        Player initiator = new Player();

        FantasticAction fantastic = new FantasticAction(initiator, Color.BLUE, discardPile);
        fantastic.perform();

        Card wished = discardPile.peek();
        assertEquals(Color.BLUE, wished.getColor());
        assertEquals(Type.WISH, wished.getType());
    }

    @Test
    public void wishValue() {
        DiscardPile discardPile = new DiscardPile();
        Player initiator = new Player();

        FantasticAction fantastic = new FantasticAction(initiator, 3, discardPile);
        fantastic.perform();

        Card wished = discardPile.peek();
        assertEquals(Value.THREE, wished.getValue());
        assertEquals(Type.WISH, wished.getType());
    }

}