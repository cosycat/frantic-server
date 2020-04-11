package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.Value;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal Player Representation
 * This class composes the internal representation of the player and defines how the player is stored in the database.
 */
@Entity
@Table(name = "Player")
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(unique = true)
    private String identity;

    @Column
    private boolean admin;

    @Column
    private String lobbyId;

    private int points;

    @Transient
    private Hand hand;

    @Transient
    private boolean blocked;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Card popCard(int index) {
        return hand.pop(index);
    }

    public void pushCardToHand(Card card) {
        this.hand.push(card);
    }

    public Card peekCard(int index) {
        return hand.peek(index);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getHandSize() {return hand.size(); }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public List<Integer> hasNiceTry() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.pop(i);
            if (card.value == Value.NICETRY) {
                result.add(i);
                return result;
            };
            hand.push(card);
        }
        return result;
    }

    public List<Integer> hasCounterAttack() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.pop(i);
            if (card.value == Value.COUNTERATTACK) {
                result.add(i);
            }
            hand.push(card);
        }
        return result;
    }

    public int calculatePoints(){
        int handPoints = 0;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.pop(i);
            if (card.getValue().ordinal() < 9) {
                handPoints += card.getValue().ordinal();
            } else if (card.getValue().ordinal() < 17){
                handPoints += 7;
            } else {
                handPoints += 42;
            }
            hand.push(card);
        }
        return handPoints;
    }

    public void clearHand() {
        this.hand.clearHand();
    }

    public int[] getPlayableCards(Card peek) {
        List<Integer> playable = new ArrayList<>();
        List<Card> cards = hand.getCards();
        for (int i = 0; i < hand.size(); i++) {
            Card card = cards.get(i);
            if (peek.isPlayable(card)) {
                playable.add(i);
            }
        }
        return playable.stream().mapToInt(i->i).toArray();
    }
}
