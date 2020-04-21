package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.ChatDTO;

public class EqualityAction implements Action {
    private Player initiator;
    private Player target;
    private Color color;
    private DiscardPile discardPile;
    private DrawStack drawStack;
    private int cardsDrawn;

    public EqualityAction(Player initiator, Player target, Color color, DiscardPile discardPile, DrawStack drawStack) {
        this.initiator = initiator;
        this.target = target;
        this.color = color;
        this.discardPile = discardPile;
        this.drawStack = drawStack;
        this.cardsDrawn = 0;
    }

    @Override
    public Chat perform() {
        if (this.target != null) {
            while (this.initiator.getHandSize() > this.target.getHandSize()) {
                this.target.pushCardToHand(drawStack.pop());
                this.cardsDrawn += 1;
            }
            discardPile.pop();
            discardPile.push(new Card(this.color, Type.WISH, Value.COLORWISH, false, 0));
            return new Chat("event", "equality", this.target.getUsername()
                    + " drew " + this.cardsDrawn + " cards.");
        }
        else {
            discardPile.pop();
            discardPile.push(new Card(this.color, Type.WISH, Value.COLORWISH, false, 0));
            return new Chat("event", "equality", this.initiator.getUsername()
                    + " wished " + FranticUtils.getStringRepresentation(this.color));
        }
    }

    @Override
    public Player[] getTargets() {
        if (this.target == null) {
            return null;
        }
        return new Player[]{this.target};
    }

    @Override
    public Player getInitiator() {
        return this.initiator;
    }

    @Override
    public boolean isCounterable() {
        return true;
    }
}
