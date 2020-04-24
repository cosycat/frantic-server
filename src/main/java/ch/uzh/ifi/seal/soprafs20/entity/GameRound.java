package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.actions.*;
import ch.uzh.ifi.seal.soprafs20.entity.events.*;
import ch.uzh.ifi.seal.soprafs20.service.GameService;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;

import java.util.*;

public class GameRound {

    private final GameService gameService;
    private final Game game;
    private final String lobbyId;
    private final List<Player> listOfPlayers;
    private Player currentPlayer;
    private boolean hasCurrentPlayerMadeMove;
    private Timer timer;
    private int turnNumber;
    private boolean timeBomb; // indicates if the timeBomb-event is currently running
    private final HashMap<Player, Integer> bombMap;
    private final List<Event> events;
    private Pile<Card> drawStack;
    private Pile<Card> discardPile;
    private Action currentAction;
    private boolean isProcessing;
    private boolean attackState;


    public GameRound(Game game, String lobbyId, List<Player> listOfPlayers, Player firstPlayer) {
        this.game = game;
        this.lobbyId = lobbyId;
        this.listOfPlayers = listOfPlayers;
        this.currentPlayer = firstPlayer;
        this.gameService = GameService.getInstance();
        this.turnNumber = 0;
        this.timeBomb = false;
        this.bombMap = new HashMap<>();
        this.currentAction = null;
        this.events = new ArrayList<>();
        this.isProcessing = false;
        this.attackState = false;
    }

    //creates Piles & player hands
    public void initializeGameRound() {
        initEvents();
        this.drawStack = new DrawStack();
        this.discardPile = new DiscardPile();

        //move 7 initial cards to player hands
        for (Player player : this.listOfPlayers) {
            for (int i = 1; i <= 7; i++) {
                Card card = this.drawStack.pop();
                player.pushCardToHand(card);
            }
        }

        //move initial card to discardPile
        this.discardPile.push(this.drawStack.pop());
    }

    private void sendInitialGameState() {
        for (Player player : this.listOfPlayers) {
            this.gameService.sendHand(this.lobbyId, player);
        }
        sendGameState();
    }

    private void sendGameState() {
        this.gameService.sendGameState(this.lobbyId, this.discardPile.peek(), this.listOfPlayers);
    }

    public void startGameRound() {
        initializeGameRound();
        sendInitialGameState();
        startTurn();
    }

    private void prepareNewTurn() {
        if (!isRoundOver()) {
            changePlayer();
            endProcess(); //makes sure that the previous player can not invoke methods until the current player has changed
            if (timeBomb) {
                this.bombMap.put(this.currentPlayer, bombMap.get(this.currentPlayer) + 1);
                if (isTimeBombExploding()) {
                    bombExploded();
                    return;
                }
            }
            sendGameState();
            this.hasCurrentPlayerMadeMove = false;
            startTurn();
        }
        else {
            prepareNiceTry();
        }
    }

    private void startTurn() {
        this.turnNumber += 1;
        this.gameService.sendStartTurn(this.lobbyId, this.currentPlayer.getUsername(), 30, turnNumber);
        this.gameService.sendPlayableCards(this.lobbyId, this.currentPlayer, getPlayableCards(this.currentPlayer));
        startTurnTimer(30);
    }

    public void playerFinishesTurn(String identity) {
        Player player = getPlayerByIdentity(identity);
        //the process is only started when no process is running
        if (player != null && player == this.currentPlayer && startProcess()) {
            finishTurn();
        }
    }

    public void finishTurn() {
        if (!this.hasCurrentPlayerMadeMove) {
            drawCardFromStack(this.currentPlayer, 1);
        }
        this.timer.cancel();
        //return empty playable cards after turn finished
        this.gameService.sendPlayableCards(this.lobbyId, this.currentPlayer, new int[0]);
        prepareNewTurn();
    }

    private void finishSecondChance() {
        this.hasCurrentPlayerMadeMove = false;
        this.gameService.sendHand(this.lobbyId, this.currentPlayer);
        sendGameState();
        this.gameService.sendPlayableCards(this.lobbyId, this.currentPlayer, this.getPlayableCards(this.currentPlayer));
    }

    public void playCard(String identity, int index) {
        Player player = getPlayerByIdentity(identity);
        if (player != null) {
            Card relevantCard = getRelevantCardOnDiscardPile();
            Card cardToPlay = player.peekCard(index);
            if (relevantCard != null && cardToPlay != null) {
                if (attackState) {
                    playCounterattack(player, relevantCard, cardToPlay, index);
                }
                else if (cardToPlay.isPlayableOn(relevantCard) &&
                        (relevantCard.getValue() != Value.FUCKYOU || player.getHandSize() == 10)) {
                    if (player == this.currentPlayer) {
                        cardToPlay = player.popCard(index);
                        this.discardPile.push(cardToPlay);
                        this.gameService.sendHand(this.lobbyId, player);
                        this.hasCurrentPlayerMadeMove = true;

                        if (cardToPlay.getType() == Type.NUMBER) {
                            Chat chat = new Chat("event", "avatar:" + this.currentPlayer.getUsername(),
                                    this.currentPlayer.getUsername() + " played " + FranticUtils.getStringRepresentationOfNumberCard(cardToPlay) + ".");
                            this.gameService.sendChatMessage(this.lobbyId, chat);
                            if (cardToPlay.getColor() == Color.BLACK) {
                                //performEvent();
                            }
                            finishTurn();
                        }
                        else if (cardToPlay.getType() == Type.SPECIAL) {
                            Chat chat = new Chat("event", "avatar:" + this.currentPlayer.getUsername(),
                                    this.currentPlayer.getUsername() + " played " + FranticUtils.getStringRepresentation(cardToPlay.getValue()) + ".");
                            this.gameService.sendChatMessage(this.lobbyId, chat);
                            if (cardToPlay.getValue() == Value.FUCKYOU) {
                                finishTurn();
                            }
                            else if (cardToPlay.getValue() == Value.SECONDCHANCE) {
                                finishSecondChance();
                            }
                            else {
                                sendGameState();
                                this.gameService.sendActionResponse(this.lobbyId, player, cardToPlay);
                            }
                        }

                    }

                    //nice try case
                    else if (getHandSizes().containsValue(0) && cardToPlay.getValue() == Value.NICETRY) {
                        cardToPlay = player.popCard(index);
                        this.discardPile.push(cardToPlay);
                        this.gameService.sendHand(this.lobbyId, player);
                        for (Player potentialWinner : this.listOfPlayers) {
                            if (potentialWinner.getHandSize() == 0) {
                                this.timer.cancel();
                                drawCardFromStack(potentialWinner, 3);
                                this.gameService.sendHand(this.lobbyId, potentialWinner);
                            }
                        }
                        prepareNewTurn();
                    }
                }
            }
        }
    }

    private void playCounterattack(Player counterAttacker, Card relevantCard, Card cardToPlay, int index) {
        if (this.currentAction != null && this.currentAction.isCounterable() && cardToPlay.getValue() == Value.COUNTERATTACK) {
            for (Player target : this.currentAction.getTargets()) {
                if (counterAttacker.equals(target)) {
                    cardToPlay = counterAttacker.popCard(index);
                    this.discardPile.push(cardToPlay);
                    this.gameService.sendHand(this.lobbyId, counterAttacker);
                    Chat chat = new Chat("event", "avatar:" + this.currentPlayer.getUsername(),
                            counterAttacker.getUsername() + " played " + FranticUtils.getStringRepresentation(cardToPlay.getValue()) + ".");
                    this.gameService.sendChatMessage(this.lobbyId, chat);
                    sendGameState();

                    this.gameService.sendActionResponse(this.lobbyId, counterAttacker, relevantCard);

                    this.timer.cancel();
                    startCounterAttackTimer(30);
                    break;
                }
            }
        }

    }

    // in a turn, the current player can choose to draw a card
    public void currentPlayerDrawCard(String identity) {
        Player player = getPlayerByIdentity(identity);
        if (player != null && player == currentPlayer && !this.hasCurrentPlayerMadeMove) {
            drawCardFromStack(this.currentPlayer, 1);
            this.gameService.sendPlayableCards(this.lobbyId, this.currentPlayer, getPlayableCards(this.currentPlayer));
            this.hasCurrentPlayerMadeMove = true;
        }
    }

    // moves #amount cards from Stack to players hand
    private void drawCardFromStack(Player player, int amount) {
        for (int i = 1; i <= amount; i++) {
            //if the drawStack is empty and a player has to draw a card, the gameround is over
            if (this.drawStack.size() <= 0) {
                this.timer.cancel();
                onRoundOver();
            }
            player.pushCardToHand(this.drawStack.pop());
        }
        Chat chat;
        if (amount == 1) {
            chat = new Chat("event", "avatar:" + player.getUsername(),
                    player.getUsername() + " drew a card.");
        }
        else {
            chat = new Chat("event", "avatar:" + player.getUsername(),
                    player.getUsername() + " drew " + amount + " cards");
        }
        this.gameService.sendChatMessage(this.lobbyId, chat);
        this.gameService.sendDrawAnimation(this.lobbyId, amount);
        this.gameService.sendHand(this.lobbyId, player);
    }

    //if the fuck-you card is the uppermost card, then the second card is considered to evaluate
    private Card getRelevantCardOnDiscardPile() {
        Card card = this.discardPile.peek();
        if (card == null) {
            return null;
        }
        if (card.getValue() == Value.FUCKYOU && this.discardPile.size() > 1) {
            return this.discardPile.peekN(2);
        }
        if (card.getValue() == Value.COUNTERATTACK) {
            //search card that performed action
            for (int n = 2; n <= 5; n++) {
                if (this.discardPile.size() >= n && this.discardPile.peekN(n).getValue() != Value.COUNTERATTACK) {
                    return this.discardPile.peekN(n);
                }
            }
        }
        return this.discardPile.peek();
    }

    public void storeSkipAction(String identity, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new SkipAction(initiator, target);
        timer.cancel();
        prepareCounterAttack();
    }

    public void storeGiftAction(String identity, int[] cards, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new GiftAction(initiator, target, cards);
        timer.cancel();
        prepareCounterAttack();
    }

    public void storeExchangeAction(String identity, int[] cards, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new ExchangeAction(initiator, target, cards);
        timer.cancel();
        prepareCounterAttack();
    }

    public void storeFantasticAction(String identity, int value, Color color) {
        Player initiator = getPlayerByIdentity(identity);
        if (color == null) {
            this.currentAction = new FantasticAction(initiator, value, (DiscardPile) this.discardPile);
        }
        else {
            this.currentAction = new FantasticAction(initiator, color, (DiscardPile) this.discardPile);
        }
        timer.cancel();
        performAction();
    }

    public void storeFantasticFourAction(String identity, int value, Color color, Map<String, Integer> players) {
        Player initiator = getPlayerByIdentity(identity);
        Map<Player, Integer> distribution = new HashMap<>();
        for (Map.Entry<String, Integer> entry : players.entrySet()) {
            distribution.put(getPlayerByUsername(entry.getKey()), entry.getValue());
        }
        if (color == null) {
            this.currentAction = new FantasticFourAction(initiator, distribution, value,
                    (DiscardPile) this.discardPile, (DrawStack) this.drawStack);
        }
        else {
            this.currentAction = new FantasticFourAction(initiator, distribution, color,
                    (DiscardPile) this.discardPile, (DrawStack) this.drawStack);
        }
        timer.cancel();
        prepareCounterAttack();
    }

    public void storeEqualityAction(String identity, Color color, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new EqualityAction(initiator, target, color, (DiscardPile) this.discardPile, (DrawStack) this.drawStack);
        timer.cancel();
        if (this.currentAction.getTargets() != null) {
            prepareCounterAttack();
        }
        else {
            performAction();
        }
    }

    //Case where CounterAttack is played as color-wish
    public void storeCounterAttackAction(String identity, Color color) {
        Player initiator = getPlayerByIdentity(identity);
        this.currentAction = new CounterAttackAction(initiator, color, (DiscardPile) this.discardPile);
        timer.cancel();
        performAction();
    }

    //Case where NiceTry is played as color-wish
    public void storeNiceTryAction(String identity, Color color) {
        Player initiator = getPlayerByIdentity(identity);
        this.currentAction = new NiceTryAction(initiator, color, (DiscardPile) this.discardPile);
        timer.cancel();
        performAction();
    }

    private void performAction() {
        this.timer.cancel();
        this.attackState = false;
        List<Chat> chat = this.currentAction.perform();
        this.gameService.sendChatMessage(this.lobbyId, chat);
        Player initiator = currentAction.getInitiator();
        Player[] targets = currentAction.getTargets();
        sendGameState();
        this.gameService.sendHand(this.lobbyId, initiator);
        if (targets != null) {
            for (Player target : targets) {
                this.gameService.sendHand(this.lobbyId, target);
            }
        }
        finishTurn();
    }

    private void prepareCounterAttack() {
        this.gameService.sendPlayableCards(this.lobbyId, this.currentPlayer, new int[0]);
        for (Player player : this.getListOfPlayers()) {
            int[] cards = player.hasCounterAttack();
            this.gameService.sendAttackWindow(this.lobbyId, player, cards, 5);
        }
        this.attackState = true;
        startCounterAttackTimer(5);
    }

    private void prepareNiceTry() {
        for (Player player : this.listOfPlayers) {
            int[] card = player.hasNiceTry();
            this.gameService.sendAttackWindow(this.lobbyId, player, card, 5);
        }
        startNiceTryTimer(5);
    }

    private void performEvent() {
        Event event = this.events.remove(0);
        this.gameService.sendEvent(this.lobbyId, event);
        event.performEvent();
    }

    private int[] getPlayableCards(Player player) {
        return player.getPlayableCards(getRelevantCardOnDiscardPile());
    }

    private Map<Player, Integer> getHandSizes() {
        Map<Player, Integer> mappedPlayers = new HashMap<>();
        for (Player player : this.listOfPlayers) {
            int handSize = player.getHandSize();
            mappedPlayers.put(player, handSize);
        }
        return mappedPlayers;
    }

    private void incrementCurrentPlayerMap(Player player) {
        bombMap.put(player, bombMap.get(player) + 1);
    }

    public void setTimeBomb() {
        this.timeBomb = true;
        for (Player player : this.listOfPlayers) {
            this.bombMap.put(player, 0);
        }
    }

    private void changePlayer() {
        int playersIndex = this.listOfPlayers.indexOf(this.currentPlayer);
        playersIndex = (playersIndex + 1) % this.listOfPlayers.size();
        this.currentPlayer = this.listOfPlayers.get(playersIndex);

        //go to the next player, if the current player is skipped
        if (this.currentPlayer.isBlocked()) {
            Chat chat = new Chat("event", "special:skip", this.currentPlayer.getUsername()
                    + " is skipped.");
            this.gameService.sendChatMessage(this.lobbyId, chat);
            this.currentPlayer.setBlocked(false);
            changePlayer();
        }
    }

    //a Gameround is over, if someone has 0 cards in his hand (and no nice-try was played)
    // or in case of the time-bomb event, if the 3 rounds are played
    private boolean isRoundOver() {
        return (getHandSizes().containsValue(0));
    }

    private boolean isTimeBombExploding() {
        return this.bombMap.get(this.currentPlayer) >= 4;
    }

    private void onRoundOver() {
        int maxPoints = 0;
        Player playerWithMaxPoints = this.currentPlayer; //to make sure playerWithMaxPoints is initialized in all cases
        for (Player player : listOfPlayers) {
            int playersPoints = player.calculatePoints();
            if (!this.timeBomb) {
                player.setPoints(player.getPoints() + playersPoints);
            }
            else {
                if (playersPoints == 0) {
                    player.setPoints(player.getPoints() - 10);
                }
                else {
                    player.setPoints(player.getPoints() + playersPoints + 10);
                }
            }

            if (playersPoints >= maxPoints) {
                maxPoints = playersPoints;
                playerWithMaxPoints = player;
            }
        }
        this.game.endGameRound(playerWithMaxPoints);
    }

    private void bombExploded() {
        int maxPoints = 0;
        Player playerWithMaxPoints = this.currentPlayer;
        for (Player player : listOfPlayers) {
            int playersPoints = player.calculatePoints();
            player.setPoints(player.getPoints() + 2 * playersPoints);

            if (playersPoints >= maxPoints) {
                maxPoints = playersPoints;
                playerWithMaxPoints = player;
            }
        }
        this.game.endGameRound(playerWithMaxPoints);
    }

    public void playerLostConnection(Player player) {
        if (this.listOfPlayers.size() > 1) {
            if (player == this.currentPlayer) {
                this.timer.cancel();
                prepareNewTurn();
            }
            this.bombMap.remove(player);
            this.listOfPlayers.remove(getPlayerByIdentity(player.getIdentity()));
            sendGameState();
        }
        else {
            this.timer.cancel();
        }
    }

    public void startTurnTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                finishTurn();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startCounterAttackTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                performAction();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startNiceTryTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                onRoundOver();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    //Helper method to make sure only one action (invoked by a player) is processed at a time
    private synchronized boolean startProcess() {
        if (this.isProcessing) {
            return false;
        }
        else {
            this.isProcessing = true;
            return true;
        }
    }

    private synchronized void endProcess() {
        this.isProcessing = false;
    }

    private Player getPlayerByIdentity(String identity) {
        for (Player p : listOfPlayers) {
            if (p.getIdentity().equals(identity)) {
                return p;
            }
        }
        return null;
    }

    private Player getPlayerByUsername(String username) {
        for (Player p : listOfPlayers) {
            if (p.getUsername().equals(username)) {
                return p;
            }
        }
        return null;
    }

    public List<Player> getListOfPlayers() {
        return this.listOfPlayers;
    }

    private void initEvents() {
        //initialize all Events
        Event charity = new CharityEvent();
        Event communism = new CommunismEvent();
        Event doomsday = new DoomsdayEvent(this.game, this.listOfPlayers, this.currentPlayer);
        Event earthquake = new EarthquakeEvent();
        Event expansion = new ExpansionEvent();
        Event finishLine = new FinishLineEvent(game, this.listOfPlayers);
        Event fridayTheThirteenth = new FridayTheThirteenthEvent();
        Event gamblingMan = new GamblingManEvent();
        Event market = new MarketEvent();
        Event matingSeason = new MatingSeasonEvent();
        Event merryChristmas = new MerryChristmasEvent();
        Event mexicanStandoff = new MexicanStandoffEvent();
        Event recession = new RecessionEvent();
        Event robinHood = new RobinHoodEvent();
        Event surpriseParty = new SurprisePartyEvent();
        Event theAllSeeingEye = new TheAllSeeingEyeEvent();
        Event thirdTimeLucky = new ThirdTimeLuckyEvent(this, this.drawStack);
        Event timeBomb = new TimeBombEvent(this);
        Event tornado = new TornadoEvent(this);
        Event vandalism = new VandalismEvent();

        //add them to the list of all events
        this.events.add(charity);
        this.events.add(communism);
        this.events.add(doomsday);
        this.events.add(earthquake);
        this.events.add(expansion);
        this.events.add(finishLine);
        this.events.add(fridayTheThirteenth);
        this.events.add(gamblingMan);
        this.events.add(market);
        this.events.add(merryChristmas);
        this.events.add(matingSeason);
        this.events.add(mexicanStandoff);
        this.events.add(recession);
        this.events.add(robinHood);
        this.events.add(surpriseParty);
        this.events.add(theAllSeeingEye);
        this.events.add(thirdTimeLucky);
        this.events.add(timeBomb);
        this.events.add(tornado);
        this.events.add(vandalism);

        Collections.shuffle(this.events);
    }
}