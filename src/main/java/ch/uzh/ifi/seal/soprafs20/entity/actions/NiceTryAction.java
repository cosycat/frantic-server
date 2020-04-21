package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;

public class NiceTryAction implements Action {
    private Player initiator;
    private Player[] targets;
    private Color color;
    private DiscardPile discardPile;
    private DrawStack drawStack;

    public NiceTryAction(Player initiator, Color color, DiscardPile discardPile) {
        this.initiator = initiator;
        this.color = color;
        this.discardPile = discardPile;
    }

    @Override
    public Chat perform() {
        this.discardPile.push(new Card(this.color, Type.WISH, Value.COLORWISH, false, 0));
        return new Chat();
    }

    @Override
    public Player[] getTargets() {
        return this.targets;
    }

    @Override
    public Player getInitiator() {
        return this.initiator;
    }

    @Override
    public boolean isCounterable() {
        return false;
    }
}
