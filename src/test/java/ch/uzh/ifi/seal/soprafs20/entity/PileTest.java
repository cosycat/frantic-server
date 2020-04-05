package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.entity.cards.NumberCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Pile methods are tested using the DrawStack as a concrete implementation.
 */

class PileTest {

    @Test
    public void createPile_getEmptyStack() {
        DrawStack drawStack = new DrawStack();
        assertEquals(0, drawStack.size());
    }

    @Test
    public void createStack_getStackWithElements() {
        NumberCard blue1 = new NumberCard(Color.BLUE, Type.NUMBER, 1);
        NumberCard red4 = new NumberCard(Color.RED, Type.NUMBER, 4);
        Card[] cards = {blue1, red4};

        DrawStack drawStack = new DrawStack(cards);

        assertEquals(2, drawStack.size());
    }

    @Test
    public void pushCardsintoStack() {
        NumberCard blue1 = new NumberCard(Color.BLUE, Type.NUMBER, 1);

        DrawStack drawStack = new DrawStack();
        drawStack.push(blue1);

        assertEquals(1, drawStack.size());
    }

    @Test
    public void peek1stCard() {
        NumberCard blue1 = new NumberCard(Color.BLUE, Type.NUMBER, 1);

        DrawStack drawStack = new DrawStack();
        drawStack.push(blue1);

        Card peeked = drawStack.peek();

        assertEquals(Color.BLUE, peeked.getColor());
    }

    @Test
    public void peek2ndCard() {
        NumberCard blue1 = new NumberCard(Color.BLUE, Type.NUMBER, 1);
        NumberCard red4 = new NumberCard(Color.RED, Type.NUMBER, 4);
        Card[] cards = {blue1, red4};

        DrawStack drawStack = new DrawStack(cards);

        Card peeked = drawStack.peekSecond();
        assertEquals(Color.BLUE, peeked.getColor());

    }

    @Test
    public void popCard() {
        NumberCard blue1 = new NumberCard(Color.BLUE, Type.NUMBER, 1);
        NumberCard red4 = new NumberCard(Color.RED, Type.NUMBER, 4);
        Card[] cards = {blue1, red4};

        DrawStack drawStack = new DrawStack(cards);

        assertEquals(2, drawStack.size());

        Card popped = drawStack.pop();

        assertEquals(Color.RED, popped.getColor());
        assertEquals(1, drawStack.size());

    }
}