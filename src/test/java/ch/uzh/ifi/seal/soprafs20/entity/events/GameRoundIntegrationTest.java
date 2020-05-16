package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.entity.actions.Action;
import ch.uzh.ifi.seal.soprafs20.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameRoundIntegrationTest {

    @Mock
    private Game game;

    @Mock
    private GameService gameService;

    private GameRound testRound;
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        List<Player> playerList = new ArrayList<>();
        this.player1 = new Player();
        player1.setUsername("player1");
        player1.setIdentity("id1");
        player1.pushCardToHand(new Card(Color.BLACK, Type.NUMBER, Value.EIGHT, true, 0));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 1));
        player1.pushCardToHand(new Card(Color.YELLOW, Type.NUMBER, Value.NINE, false, 2));
        player1.setPoints(42);
        playerList.add(player1);
        this.player2 = new Player();
        player2.setUsername("player2");
        player2.setIdentity("id2");
        player2.pushCardToHand(new Card(Color.GREEN, Type.SPECIAL, Value.GIFT, true, 3));
        player2.pushCardToHand(new Card(Color.YELLOW, Type.NUMBER, Value.NINE, false, 4));
        player2.setPoints(104);
        playerList.add(player2);
        this.player3 = new Player();
        player3.setUsername("player3");
        player3.setIdentity("id3");
        player3.pushCardToHand(new Card(Color.BLUE, Type.SPECIAL, Value.EXCHANGE, true, 5));
        player3.pushCardToHand(new Card(Color.MULTICOLOR, Type.SPECIAL, Value.FANTASTICFOUR, true, 6));
        player3.setPoints(44);
        playerList.add(player3);
        this.player4 = new Player();
        player4.setUsername("player4");
        player4.setIdentity("id4");
        player4.pushCardToHand(new Card(Color.RED, Type.SPECIAL, Value.EXCHANGE, true, 7));
        player4.pushCardToHand(new Card(Color.MULTICOLOR, Type.SPECIAL, Value.NICETRY, false, 8));
        player4.pushCardToHand(new Card(Color.BLACK, Type.NUMBER, Value.ONE, false, 9));
        player4.setPoints(21);
        playerList.add(player4);

        this.testRound = new GameRound(this.game, "lobbyId", playerList, player1);
        this.testRound.setGameService(this.gameService);
    }

    @Test
    public void startGameRoundTest() {
        List<Player> playerList = new ArrayList<>();
        Player emptyPlayer1 = new Player();
        emptyPlayer1.setUsername("emptyPlayer1");
        playerList.add(emptyPlayer1);
        Player emptyPlayer2 = new Player();
        playerList.add(emptyPlayer2);
        GameRound gameRound = new GameRound(this.game, "lobbyId", playerList, emptyPlayer1);
        gameRound.setGameService(this.gameService);
        Card expectedDiscardPileCard = gameRound.getDrawStack().peekN(15);

        gameRound.startGameRound();

        Mockito.verify(this.gameService).sendHand("lobbyId", emptyPlayer1);
        Mockito.verify(this.gameService).sendHand("lobbyId", emptyPlayer2);
        Mockito.verify(this.gameService).sendGameState("lobbyId", expectedDiscardPileCard, playerList, false);
        Mockito.verify(this.gameService).sendStartTurn("lobbyId", emptyPlayer1.getUsername(), 0);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", emptyPlayer1, emptyPlayer1.getPlayableCards(expectedDiscardPileCard), true, false);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 30);

        assertEquals(7, emptyPlayer1.getHandSize());
        assertEquals(7, emptyPlayer2.getHandSize());
        assertEquals(expectedDiscardPileCard, gameRound.getDiscardPile().peek());
    }

    @Test
    public void playerFinishesTurn_notMadeMoveBefore_success() {
        assertEquals(3, player1.getHandSize());
        Card cardOnDiscardPile = new Card(Color.GREEN, Type.NUMBER, Value.TWO, false, 10);
        testRound.getDiscardPile().push(cardOnDiscardPile);

        testRound.startTurnTimer(30);
        testRound.playerFinishesTurn("id1");

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendGameState("lobbyId", cardOnDiscardPile, this.testRound.getListOfPlayers(), false);
        Mockito.verify(this.gameService).sendStartTurn("lobbyId", player2.getUsername(), 0);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player2, player2.getPlayableCards(cardOnDiscardPile), true, false);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 30);

        assertEquals(4, player1.getHandSize());
        assertEquals(player2, testRound.getCurrentPlayer());
    }

    @Test
    public void playerFinishesTurn_madeMoveBefore_success() {
        assertEquals(3, player1.getHandSize());
        Card cardOnDiscardPile = new Card(Color.GREEN, Type.NUMBER, Value.TWO, false, 10);
        testRound.getDiscardPile().push(cardOnDiscardPile);

        testRound.startTurnTimer(30);
        testRound.currentPlayerDrawCard("id1");
        assertEquals(4, player1.getHandSize());

        testRound.playerFinishesTurn("id1");

        assertEquals(4, player1.getHandSize());
        assertEquals(player2, testRound.getCurrentPlayer());
    }

    @Test
    public void playerFinishesTurn_wrongPlayer_unsuccessful() {
        testRound.startTurnTimer(30);
        testRound.playerFinishesTurn("id2");

        assertEquals(3, player1.getHandSize());
        assertEquals(2, player2.getHandSize());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playCard_playerNotAllowedToPlay() {
        Card pileCard = new Card(Color.YELLOW, Type.NUMBER, Value.EIGHT, false, 10);
        testRound.getDiscardPile().push(pileCard);
        testRound.startTurnTimer(30);
        testRound.playCard("id2", 1);

        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService, Mockito.never()).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        assertEquals(2, player2.getHandSize());
        assertEquals(pileCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playNumberCard_success() {
        Card cardToPlay = player1.peekCard(1);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 10));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 1);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        assertEquals(2, player1.getHandSize());
        assertEquals(cardToPlay, testRound.getDiscardPile().peek());
        assertEquals(player2, testRound.getCurrentPlayer());
    }

    @Test
    public void playBlackNumberCard_success_prepareEvent() {
        Card cardToPlay = player1.peekCard(0);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 10));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 0);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendGameState("lobbyId", cardToPlay, this.testRound.getListOfPlayers(), false);
        Mockito.verify(this.gameService).sendEvent(Mockito.any(), Mockito.any());
        Mockito.verify(this.gameService).sendTimer("lobbyId", 11);

        assertEquals(2, player1.getHandSize());
        assertEquals(cardToPlay, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playNumberCard_cardNotPlayable() {
        Card pileCard = new Card(Color.YELLOW, Type.NUMBER, Value.EIGHT, false, 10);
        testRound.getDiscardPile().push(pileCard);
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 1);

        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService, Mockito.never()).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        assertEquals(3, player1.getHandSize());
        assertEquals(pileCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playFuckYou_success() {
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 10));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 11));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 12));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 13));
        player1.pushCardToHand(new Card(Color.BLACK, Type.SPECIAL, Value.FUCKYOU, false, 14));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 15));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 16));
        assertEquals(10, player1.getHandSize());

        Card fuckYou = player1.peekCard(7);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 10));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 7);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        assertEquals(9, player1.getHandSize());
        assertEquals(fuckYou, testRound.getDiscardPile().peek());
        assertEquals(player2, testRound.getCurrentPlayer());
    }

    @Test
    public void playFuckYou_not10Cards() {
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 10));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 11));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 12));
        player1.pushCardToHand(new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 13));
        player1.pushCardToHand(new Card(Color.BLACK, Type.SPECIAL, Value.FUCKYOU, false, 14));

        Card pileCard = new Card(Color.YELLOW, Type.NUMBER, Value.EIGHT, false, 10);
        testRound.getDiscardPile().push(pileCard);
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 7);

        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService, Mockito.never()).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        assertEquals(8, player1.getHandSize());
        assertEquals(pileCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void play2ChanceCard_success() {
        Card secondChance = new Card(Color.BLUE, Type.SPECIAL, Value.SECONDCHANCE, false, 10);
        player1.pushCardToHand(secondChance);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 11));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 3);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, player1.getPlayableCards(secondChance), true, false);
        assertEquals(3, player1.getHandSize());
        assertEquals(secondChance, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playFantasticFourCard_success_45sec() {
        Card fantasticFour = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.FANTASTICFOUR, true, 10);
        player1.pushCardToHand(fantasticFour);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 11));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 3);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        Mockito.verify(this.gameService).sendTimer("lobbyId", 45);
        Mockito.verify(this.gameService).sendActionResponse("lobbyId", player1, fantasticFour);
        assertEquals(3, player1.getHandSize());
        assertEquals(fantasticFour, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playEqualityCard_success_30sec() {
        Card equality = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.EQUALITY, false, 10);
        player1.pushCardToHand(equality);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 11));
        testRound.startTurnTimer(30);
        testRound.playCard("id1", 3);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        Mockito.verify(this.gameService).sendTimer("lobbyId", 30);
        Mockito.verify(this.gameService).sendActionResponse("lobbyId", player1, equality);
        assertEquals(3, player1.getHandSize());
        assertEquals(equality, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playNiceTry_niceTryOpportunity_success() {
        player4.popCard();
        player4.popCard();
        player4.popCard();
        assertEquals(0, player4.getHandSize());

        Card niceTry = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.NICETRY, false, 10);
        player3.pushCardToHand(niceTry);
        testRound.getDiscardPile().push(new Card(Color.BLUE, Type.NUMBER, Value.EIGHT, false, 11));
        testRound.startTurnTimer(30);
        testRound.playCard("id3", 2);

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.times(2)).sendChatMessage(Mockito.any(), (Chat) Mockito.any()); //invocation for card played & drawn cards
        Mockito.verify(this.gameService, Mockito.times(2)).sendHand("lobbyId", player4); //invocation for card played & drawn cards
        Mockito.verify(this.gameService).sendActionResponse("lobbyId", player3, niceTry);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 30);

        assertEquals(2, player3.getHandSize());
        assertEquals(3, player4.getHandSize());
        assertEquals(niceTry, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playCounterattack_niceTryOpportunity_unsuccessful() {
        player4.popCard();
        player4.popCard();
        player4.popCard();
        assertEquals(0, player4.getHandSize());

        Card counterattack = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.COUNTERATTACK, false, 10);
        player3.pushCardToHand(counterattack);
        Card pileCard = new Card(Color.YELLOW, Type.NUMBER, Value.EIGHT, false, 11);
        testRound.getDiscardPile().push(pileCard);
        testRound.startTurnTimer(30);
        testRound.playCard("id3", 2);

        Mockito.verify(this.gameService, Mockito.never()).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);

        assertEquals(3, player3.getHandSize());
        assertEquals(0, player4.getHandSize());
        assertEquals(pileCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playCounterAttack_counterAttackOpportunity_success() {
        testRound.startTurnTimer(30);
        testRound.storeSkipAction("id1", "player3");

        Card counterattack = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.COUNTERATTACK, false, 10);
        player3.pushCardToHand(counterattack);
        Card actionCard = new Card(Color.BLUE, Type.SPECIAL, Value.SKIP, true, 11);
        testRound.getDiscardPile().push(actionCard);
        testRound.playCard("id3", 2);

        Mockito.verify(this.gameService, Mockito.times(2)).sendPlayable("lobbyId", player3, new int[0], false, false); //once in prepareCounterattack & playCounterattack
        Mockito.verify(this.gameService).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.times(2)).sendChatMessage(Mockito.any(), (Chat) Mockito.any()); //once in prepareCounterattack & playCounterattack
        Mockito.verify(this.gameService).sendGameState("lobbyId", counterattack, this.testRound.getListOfPlayers(), false);
        Mockito.verify(this.gameService).sendAttackTurn("lobbyId", "player3");
        Mockito.verify(this.gameService).sendActionResponse("lobbyId", player3, actionCard);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 30);

        assertEquals(2, player3.getHandSize());
        assertEquals(counterattack, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void playCounterAttack_counterAttackOpportunity_notTarget() {
        testRound.startTurnTimer(30);
        testRound.storeSkipAction("id1", "player2");

        Card counterattack = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.COUNTERATTACK, false, 10);
        player3.pushCardToHand(counterattack);
        Card actionCard = new Card(Color.BLUE, Type.SPECIAL, Value.SKIP, true, 11);
        testRound.getDiscardPile().push(actionCard);
        testRound.playCard("id3", 2);

        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);

        assertEquals(3, player3.getHandSize());
        assertEquals(actionCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    //if someone tries to play a nice try to block an attack
    @Test
    public void playNiceTry_counterAttackOpportunity_unsuccessful() {
        testRound.startTurnTimer(30);
        testRound.storeSkipAction("id1", "player3");

        Card niceTry = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.NICETRY, false, 10);
        player3.pushCardToHand(niceTry);
        Card actionCard = new Card(Color.BLUE, Type.SPECIAL, Value.SKIP, true, 11);
        testRound.getDiscardPile().push(actionCard);
        testRound.playCard("id3", 2);

        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);

        assertEquals(3, player3.getHandSize());
        assertEquals(actionCard, testRound.getDiscardPile().peek());
        assertEquals(player1, testRound.getCurrentPlayer());
    }

    @Test
    public void storeSkipAction_prepareCounterattack_noCounterattack() {
        testRound.startTurnTimer(30);
        testRound.storeSkipAction("id1", "player3");

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player3, "special:skip", "skip", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService, Mockito.never()).sendOverlay("lobbyId", player1, "special:skip", "skip", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService, Mockito.never()).sendOverlay("lobbyId", player2, "special:skip", "skip", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService, Mockito.never()).sendOverlay("lobbyId", player4, "special:skip", "skip", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 6);

        Action skip = testRound.getCurrentAction();
        assertEquals(player1, skip.getInitiator());
        assertEquals(1, skip.getTargets().length);
        assertEquals(player3, Array.get(skip.getTargets(), 0));
    }

    @Test
    public void storeSkipAction_prepareCounterattack_hasCounterattack() {
        player3.pushCardToHand(new Card(Color.MULTICOLOR, Type.SPECIAL, Value.COUNTERATTACK, false, 10));

        testRound.startTurnTimer(30);
        testRound.storeSkipAction("id1", "player3");

        int[] index = {2};
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, index, false, false);
    }

    @Test
    public void storeGiftAction_prepareCounterattack() {
        int[] cards = {0, 1};
        testRound.startTurnTimer(30);
        testRound.storeGiftAction("id1", cards, "player3");

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player3, "special:gift", "gift", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 6);

        Action gift = testRound.getCurrentAction();
        assertEquals(player1, gift.getInitiator());
        assertEquals(1, gift.getTargets().length);
        assertEquals(player3, Array.get(gift.getTargets(), 0));
    }

    @Test
    public void storeExchangeAction_prepareCounterattack() {
        int[] cards = {0, 1};
        testRound.startTurnTimer(30);
        testRound.storeExchangeAction("id1", cards, "player3");

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player3, "special:exchange", "exchange", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 6);

        Action exchange = testRound.getCurrentAction();
        assertEquals(player1, exchange.getInitiator());
        assertEquals(1, exchange.getTargets().length);
        assertEquals(player3, Array.get(exchange.getTargets(), 0));
    }

    @Test
    public void storeFantasticAction_colorWish_performAction() {
        testRound.startTurnTimer(30);
        testRound.storeFantasticAction("id1", 7, Color.BLUE);

        //in case prepareCounterAttack would be (mistakenly) invoked, this assertion would fail
        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        //no targets so no other player should get a hand package
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player2);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player4);

        Action fantastic = testRound.getCurrentAction();
        assertEquals(player1, fantastic.getInitiator());
        Card wishCard = testRound.getDiscardPile().peek();
        assertEquals(Color.BLUE, wishCard.getColor());
        assertEquals(Value.NONE, wishCard.getValue());
        assertEquals(Type.WISH, wishCard.getType());
    }

    @Test
    public void storeFantasticAction_valueWish_performAction() {
        testRound.startTurnTimer(30);
        testRound.storeFantasticAction("id1", 7, null);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        //no targets so no other player should get a hand package
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player2);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player4);

        Action fantastic = testRound.getCurrentAction();
        assertEquals(player1, fantastic.getInitiator());
        Card wishCard = testRound.getDiscardPile().peek();
        assertEquals(Color.NONE, wishCard.getColor());
        assertEquals(Value.SEVEN, wishCard.getValue());
        assertEquals(Type.WISH, wishCard.getType());
    }

    @Test
    public void storeFantasticFourAction_prepareCounterattack() {
        Map<String, Integer> map = new HashMap<>();
        map.put(player2.getUsername(), 2);
        map.put(player3.getUsername(), 2);
        testRound.startTurnTimer(30);
        testRound.storeFantasticFourAction("id1", 7, Color.YELLOW, map);

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player2, "special:fantastic-four", "fantastic-four", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player3, "special:fantastic-four", "fantastic-four", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService, Mockito.never()).sendOverlay("lobbyId", player4, "special:fantastic-four", "fantastic-four", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 6);

        Action fantasticFour = testRound.getCurrentAction();
        assertEquals(player1, fantasticFour.getInitiator());
        assertEquals(2, fantasticFour.getTargets().length);
        assertTrue(Arrays.asList(fantasticFour.getTargets()).contains(player2));
        assertTrue(Arrays.asList(fantasticFour.getTargets()).contains(player3));
    }

    @Test
    public void storeEqualityAction_noTargets_performAction() {
        testRound.startTurnTimer(30);
        testRound.storeEqualityAction("id1", Color.RED, "");

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        //no targets so no other player should get a hand package
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player2);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player4);

        Action equality = testRound.getCurrentAction();
        assertEquals(player1, equality.getInitiator());
        Card wishCard = testRound.getDiscardPile().peek();
        assertEquals(Color.RED, wishCard.getColor());
        assertEquals(Value.COLORWISH, wishCard.getValue());
        assertEquals(Type.WISH, wishCard.getType());
    }

    @Test
    public void storeEqualityAction_prepareCounterattack() {
        testRound.startTurnTimer(30);
        testRound.storeEqualityAction("id1", Color.YELLOW, "player3");

        Mockito.verify(this.gameService).sendPlayable("lobbyId", player1, new int[0], false, false);
        Mockito.verify(this.gameService).sendPlayable("lobbyId", player3, new int[0], false, false);
        Mockito.verify(this.gameService).sendOverlay("lobbyId", player3, "special:equality", "equality", "You are being attacked by player1", 2);
        Mockito.verify(this.gameService).sendTimer("lobbyId", 6);

        Action equality = testRound.getCurrentAction();
        assertEquals(player1, equality.getInitiator());
        assertEquals(1, equality.getTargets().length);
        assertEquals(player3, Array.get(equality.getTargets(), 0));
    }

    @Test
    public void storeCounterattackAction_performAction() {
        testRound.startTurnTimer(30);
        testRound.storeCounterAttackAction("id1", Color.GREEN);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        //no targets so no other player should get a hand package
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player2);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player4);

        Action equality = testRound.getCurrentAction();
        assertEquals(player1, equality.getInitiator());
        Card wishCard = testRound.getDiscardPile().peek();
        assertEquals(Color.GREEN, wishCard.getColor());
        assertEquals(Value.COLORWISH, wishCard.getValue());
        assertEquals(Type.WISH, wishCard.getType());
    }

    @Test
    public void storeNiceTryAction_performAction() {
        testRound.startTurnTimer(30);
        testRound.storeNiceTryAction("id1", Color.GREEN);

        Mockito.verify(this.gameService).sendHand("lobbyId", player1);
        //no targets so no other player should get a hand package
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player2);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player3);
        Mockito.verify(this.gameService, Mockito.never()).sendHand("lobbyId", player4);

        Action equality = testRound.getCurrentAction();
        assertEquals(player1, equality.getInitiator());
        Card wishCard = testRound.getDiscardPile().peek();
        assertEquals(Color.GREEN, wishCard.getColor());
        assertEquals(Value.COLORWISH, wishCard.getValue());
        assertEquals(Type.WISH, wishCard.getType());
    }

    @Test
    public void onRoundOver_unblockPlayer() {
        player2.setBlocked(true);
        player4.setBlocked(true);

        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        assertFalse(player2.isBlocked());
        assertFalse(player4.isBlocked());
    }

    @Test
    public void onRoundOver_calculatePoints_noTimeBomb_1winner() {
        player1.popCard();
        player1.popCard();
        player1.popCard();
        assertEquals(42, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("player1 played his/her last card!"));
        assertEquals(42, player1.getPoints());
        assertEquals(120, player2.getPoints());
        assertEquals(58, player3.getPoints());
        assertEquals(36, player4.getPoints());
    }

    @Test
    public void onRoundOver_calculatePoints_noTimeBomb_2winners() {
        player1.popCard();
        player1.popCard();
        player1.popCard();
        player4.popCard();
        player4.popCard();
        player4.popCard();
        assertEquals(42, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("player1, player4 played their last card!"));
        assertEquals(42, player1.getPoints());
        assertEquals(120, player2.getPoints());
        assertEquals(58, player3.getPoints());
        assertEquals(21, player4.getPoints());
    }

    @Test
    public void onRoundOver_calculatePoints_TimeBomb_1winner() {
        player1.popCard();
        player1.popCard();
        player1.popCard();
        assertEquals(42, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.setTimeBomb();
        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("player1 defused the bomb!"));
        assertEquals(32, player1.getPoints());
        assertEquals(130, player2.getPoints());
        assertEquals(68, player3.getPoints());
        assertEquals(46, player4.getPoints());
    }

    @Test
    public void onRoundOver_calculatePoints_TimeBomb_2winners() {
        player1.popCard();
        player1.popCard();
        player1.popCard();
        player4.popCard();
        player4.popCard();
        player4.popCard();
        assertEquals(42, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.setTimeBomb();
        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("player1, player4 defused the bomb!"));
        assertEquals(32, player1.getPoints());
        assertEquals(130, player2.getPoints());
        assertEquals(68, player3.getPoints());
        assertEquals(11, player4.getPoints());
    }

    @Test
    public void onRoundOver_calculatePoints_TimeBomb_belowZero() {
        player1.popCard();
        player1.popCard();
        player1.popCard();
        player1.setPoints(3);
        assertEquals(3, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.setTimeBomb();
        testRound.startTurnTimer(30);
        testRound.onRoundOver(false);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("player1 defused the bomb!"));
        assertEquals(-7, player1.getPoints());
        assertEquals(130, player2.getPoints());
        assertEquals(68, player3.getPoints());
        assertEquals(46, player4.getPoints());
    }

    @Test
    public void onRoundOver_stackEmpty() {
        assertEquals(42, player1.getPoints());
        assertEquals(104, player2.getPoints());
        assertEquals(44, player3.getPoints());
        assertEquals(21, player4.getPoints());

        testRound.startTurnTimer(30);
        testRound.onRoundOver(true);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("The card stack is empty!"));
        assertEquals(62, player1.getPoints());
        assertEquals(120, player2.getPoints());
        assertEquals(58, player3.getPoints());
        assertEquals(36, player4.getPoints());
    }

    @Test
    public void drawCardFromStack_stackNotEmpty() {
        assertEquals(2, player2.getHandSize());
        Card card1 =new Card(Color.YELLOW, Type.NUMBER, Value.EIGHT, false, 10);
        Card card2 = new Card(Color.RED, Type.NUMBER, Value.EIGHT, false, 11);
        testRound.getDrawStack().push(card1);
        testRound.getDrawStack().push(card2);
        int drawStackSize = testRound.getDrawStack().size();

        testRound.drawCardFromStack(player2, 2);

        Mockito.verify(this.gameService).sendChatMessage(Mockito.any(), (Chat) Mockito.any());
        Mockito.verify(this.gameService).sendDrawAnimation("lobbyId", 2);
        Mockito.verify(this.gameService).sendHand("lobbyId", player2);

        assertEquals(drawStackSize - 2, testRound.getDrawStack().size());
        assertEquals(4, player2.getHandSize());
        assertEquals(card1, player2.peekCard(2));
        assertEquals(card2, player2.peekCard(3));
    }

    @Test
    public void drawCardFromStack_stackEmpty_onRoundOver() {
        assertEquals(2, player2.getHandSize());

        for (int i=1; i<=124; i++) {
            testRound.getDrawStack().pop();
        }
        assertEquals(1, testRound.getDrawStack().size());

        testRound.startTurnTimer(30);
        testRound.drawCardFromStack(player2, 2);

        Mockito.verify(this.game).endGameRound(Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.eq("The card stack is empty!"));
        assertEquals(0, testRound.getDrawStack().size());
        assertEquals(3, player2.getHandSize());
    }
}
