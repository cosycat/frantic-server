package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.actions.*;
import ch.uzh.ifi.seal.soprafs20.entity.events.*;
import ch.uzh.ifi.seal.soprafs20.service.GameService;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;

import java.util.*;
import java.util.stream.Collectors;

public class GameRound {

    private final GameService gameService;
    private final Game game;
    private final String lobbyId;
    private final List<Player> listOfPlayers;
    private Player currentPlayer;
    private boolean hasCurrentPlayerMadeMove;
    private Timer timer;
    private boolean timeBomb; // indicates if the timeBomb-event is currently running
    private final HashMap<Player, Integer> bombMap;
    private final List<Event> events;
    private final Pile<Card> drawStack;
    private final Pile<Card> discardPile;
    private Action currentAction;
    private boolean isProcessing;
    private boolean turnIsRunning;
    private boolean attackState;
    private boolean showCards;
    private List<Player> eventResponses;
    private Map<Player, Card> surprisePartyMap;
    private Map<Player, List<Card>> christmasMap;
    private Map<Player, Integer> gamblingManMap;
    private List<Card> marketList;

    public GameRound(Game game, String lobbyId, List<Player> listOfPlayers, Player firstPlayer) {
        this.game = game;
        this.lobbyId = lobbyId;
        this.listOfPlayers = listOfPlayers;
        this.currentPlayer = firstPlayer;
        this.gameService = GameService.getInstance();
        this.drawStack = new DrawStack();
        this.discardPile = new DiscardPile();
        this.timeBomb = false;
        this.bombMap = new HashMap<>();
        this.currentAction = null;
        this.events = new ArrayList<>();
        this.isProcessing = false;
        this.turnIsRunning = false;
        this.attackState = false;
        this.showCards = false;
        this.eventResponses = new ArrayList<>();
        this.surprisePartyMap = new HashMap<>();
        this.christmasMap = new HashMap<>();
        this.gamblingManMap = new HashMap<>();
        this.marketList = new ArrayList<>();
    }

    //creates Piles & player hands
    public void initializeGameRound() {
        initEvents();

        //move 7 initial cards to player hands
        for (Player player : this.listOfPlayers) {
            for (int i = 1; i <= 7; i++) {
                Card card = this.drawStack.pop();
                player.pushCardToHand(card);
            }
        }

        //move initial card to discardPile
        this.discardPile.push(this.drawStack.pop());

        for (Player player : this.listOfPlayers) {
            this.bombMap.put(player, 0);
        }
    }

    public void sendCompleteGameState() {
        for (Player player : this.listOfPlayers) {
            this.gameService.sendHand(this.lobbyId, player);
        }
        sendGameState();
    }

    public void sendGameState() {
        this.gameService.sendGameState(this.lobbyId, this.discardPile.peek(), this.listOfPlayers, this.showCards);
    }

    public void startGameRound() {
        initializeGameRound();
        sendCompleteGameState();
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
            endProcess();
            sendGameState();
            prepareNiceTry();
        }
    }

    private void startTurn() {
        this.turnIsRunning = true;
        int timeBomb = Collections.max(this.bombMap.values());
        if (timeBomb > 0) {
            timeBomb = - timeBomb + 4;
        }
        this.gameService.sendStartTurn(this.lobbyId, this.currentPlayer.getUsername(), timeBomb);
        this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, getPlayableCards(this.currentPlayer), true, false);
        this.gameService.sendTimer(this.lobbyId, 30);
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
        if (this.showCards) {
            setShowCards(false);
        }
        this.turnIsRunning = false;
        if (!this.hasCurrentPlayerMadeMove) {
            drawCardFromStack(this.currentPlayer, 1);
        }
        this.timer.cancel();
        //return empty playable cards after turn finished
        this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, new int[0], false, false);
        prepareNewTurn();
    }

    private void finishSecondChance() {
        this.hasCurrentPlayerMadeMove = false;
        this.gameService.sendHand(this.lobbyId, this.currentPlayer);
        sendGameState();
        this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, getPlayableCards(this.currentPlayer), true, false);
    }

    public void playCard(String identity, int index) {
        Player player = getPlayerByIdentity(identity);
        if (player != null && startProcess()) {
            Card relevantCard = getRelevantCardOnDiscardPile();
            Card cardToPlay = player.peekCard(index);
            if (relevantCard != null && cardToPlay != null) {
                if (attackState) {
                    playCounterattack(player, relevantCard, cardToPlay, index);
                }
                else if (cardToPlay.isPlayableOn(relevantCard) &&
                        (cardToPlay.getValue() != Value.FUCKYOU || player.getHandSize() == 10)) {
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
                                prepareEvent();
                            }
                            else {
                                finishTurn();
                            }
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
                                this.timer.cancel();
                                this.gameService.sendTimer(this.lobbyId, 30);
                                this.gameService.sendActionResponse(this.lobbyId, player, cardToPlay);
                                startTurnTimer(30);
                            }
                        }
                    }

                    //nice try case
                    else if (getHandSizes().containsValue(0) && cardToPlay.getValue() == Value.NICETRY) {
                        playNiceTry(player, index);
                    }
                }
            }
            endProcess();
        }
    }

    private void playCounterattack(Player counterAttacker, Card relevantCard, Card cardToPlay, int index) {
        if (this.currentAction != null && this.currentAction.isCounterable() && cardToPlay.getValue() == Value.COUNTERATTACK) {
            for (Player target : this.currentAction.getTargets()) {
                if (counterAttacker.equals(target)) {
                    this.timer.cancel();
                    cardToPlay = counterAttacker.popCard(index);
                    this.discardPile.push(cardToPlay);
                    this.gameService.sendPlayable(this.lobbyId, counterAttacker, new int[0], false, false);
                    this.gameService.sendHand(this.lobbyId, counterAttacker);
                    Chat chat = new Chat("event", "avatar:" + counterAttacker.getUsername(),
                            counterAttacker.getUsername() + " played " + FranticUtils.getStringRepresentation(cardToPlay.getValue()) + ".");
                    this.gameService.sendChatMessage(this.lobbyId, chat);
                    sendGameState();

                    this.gameService.sendActionResponse(this.lobbyId, counterAttacker, relevantCard);
                    this.gameService.sendTimer(this.lobbyId, 30);
                    startCounterAttackTimer(30);
                    break;
                }
            }
        }
    }

    private void playNiceTry(Player niceTryPlayer, int index) {
        this.timer.cancel();
        Card cardToPlay = niceTryPlayer.popCard(index);
        this.discardPile.push(cardToPlay);
        this.gameService.sendPlayable(this.lobbyId, niceTryPlayer, new int[0], false, false);
        this.gameService.sendHand(this.lobbyId, niceTryPlayer);
        Chat chat = new Chat("event", "avatar:" + niceTryPlayer.getUsername(),
                niceTryPlayer.getUsername() + " played " + FranticUtils.getStringRepresentation(cardToPlay.getValue()) + ".");
        this.gameService.sendChatMessage(this.lobbyId, chat);

        for (Player potentialWinner : this.listOfPlayers) {
            if (potentialWinner.getHandSize() == 0) {
                drawCardFromStack(potentialWinner, 3);
                this.gameService.sendHand(this.lobbyId, potentialWinner);
            }
        }
        sendGameState();
        this.gameService.sendActionResponse(this.lobbyId, niceTryPlayer, cardToPlay);
        this.gameService.sendTimer(this.lobbyId, 30);
        startInterTurnTimer(30);
    }

    // in a turn, the current player can choose to draw a card
    public void currentPlayerDrawCard(String identity) {
        Player player = getPlayerByIdentity(identity);
        if (player != null && player == currentPlayer && !this.hasCurrentPlayerMadeMove) {
            drawCardFromStack(this.currentPlayer, 1);
            sendGameState();
            this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, getPlayableCards(this.currentPlayer), false, true);
            this.hasCurrentPlayerMadeMove = true;
        }
    }

    // moves #amount cards from Stack to players hand
    public void drawCardFromStack(Player player, int amount) {
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
        prepareCounterAttack("skip");
    }

    public void storeGiftAction(String identity, int[] cards, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new GiftAction(initiator, target, cards);
        timer.cancel();
        prepareCounterAttack("gift");
    }

    public void storeExchangeAction(String identity, int[] cards, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new ExchangeAction(initiator, target, cards);
        timer.cancel();
        prepareCounterAttack("exchange");
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
        prepareCounterAttack("fantastic-four");
    }

    public void storeEqualityAction(String identity, Color color, String username) {
        Player initiator = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(username);
        this.currentAction = new EqualityAction(initiator, target, color, (DiscardPile) this.discardPile, (DrawStack) this.drawStack);
        timer.cancel();
        if (this.currentAction.getTargets().length != 0) {
            prepareCounterAttack("equality");
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
        if (this.turnIsRunning) {
            finishTurn();
        }
        else {
            prepareNewTurn();
        }
    }

    private void prepareCounterAttack(String attackType) {
        this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, new int[0], false, false);
        List<Player> targets = new ArrayList<>();
        Collections.addAll(targets, this.currentAction.getTargets());
        List<String> targetUsernames = targets.stream().map(Player::getUsername).collect(Collectors.toList());
        String attacker = this.currentAction.getInitiator().getUsername();

        if (!targets.isEmpty()) {
            Chat chat = new Chat("event", "special:" + attackType,
                    attacker + " is attacking " + String.join(", ", targetUsernames) + ".");
            this.gameService.sendChatMessage(this.lobbyId, chat);
        }

        for (Player player : this.listOfPlayers) {
            if (targets.contains(player)) {
                int[] cards = player.hasCounterAttack();
                this.gameService.sendPlayable(this.lobbyId, player, cards, false, false);

                this.gameService.sendOverlay(this.lobbyId, player, "special:" + attackType, attackType,
                        "You are being attacked by " + attacker, 2);
            }
        }
        this.gameService.sendTimer(this.lobbyId, 6);

        this.attackState = true;
        startCounterAttackTimer(6);
    }

    private void prepareNiceTry() {
        for (Player player : this.listOfPlayers) {
            int[] card = player.hasNiceTry();
            this.gameService.sendPlayable(this.lobbyId, player, card, false, false);
        }
        this.gameService.sendTimer(this.lobbyId, 5);
        startNiceTryTimer(5);
    }

    private void prepareEvent() {
        this.timer.cancel();
        this.gameService.sendPlayable(this.lobbyId, this.currentPlayer, new int[0], false, false);
        sendGameState();

        FranticUtils.wait(1000);

        Event event = this.events.get(0);
        this.gameService.sendEvent(this.lobbyId, event);
        startAnimationTimer(11);
    }

    private void performEvent() {
        Event event = this.events.remove(0);
        Chat chat = new Chat("event", "event:" + event.getName(), event.getMessage());
        this.gameService.sendChatMessage(this.lobbyId, chat);
        event.performEvent();
    }

    public void performRecession(String identity, int[] cards) {
        Player player = getPlayerByIdentity(identity);
        if (player != null && cards.length > 0) {
            this.eventResponses.add(player);
            for (int i = cards.length - 1; i >= 0; i--) {
                player.popCard(cards[i]);
            }
            Chat chat;
            if (cards.length == 1) {
                chat = new Chat("event", "event:recession", player.getUsername() + " discards 1 card.");
            }
            else {
                chat = new Chat("event", "event:recession", player.getUsername() + " discards " + cards.length + " cards.");
            }
            this.gameService.sendChatMessage(this.lobbyId, chat);
            sendCompleteGameState();
        }
        if (this.eventResponses.size() == this.listOfPlayers.size()) {
            this.timer.cancel();
            finishTurn();
            this.eventResponses = new ArrayList<>();
        }
    }

    public void prepareSurpriseParty(String identity, int card, String targetUsername) {
        Player player = getPlayerByIdentity(identity);
        Player target = getPlayerByUsername(targetUsername);
        if (player != null && target != null) {
            this.eventResponses.add(player);
            this.surprisePartyMap.put(target, player.popCard(card));
        }
        if (this.eventResponses.size() == this.listOfPlayers.size()) {
            performSurpriseParty();
        }
    }

    private void performSurpriseParty() {
        timer.cancel();
        for (Map.Entry<Player, Card> entry : this.surprisePartyMap.entrySet()) {
            entry.getKey().pushCardToHand(entry.getValue());
        }
        sendCompleteGameState();
        this.eventResponses = new ArrayList<>();
        this.surprisePartyMap = new HashMap<>();
        finishTurn();
    }

    public void prepareMerryChristmas(String identity, Map<String, Integer[]> targets) {
        Player player = getPlayerByIdentity(identity);
        if (player != null) {
            this.eventResponses.add(player);
            for (Map.Entry<String, Integer[]> entry : targets.entrySet()) {
                Player target = getPlayerByUsername(entry.getKey());
                List<Card> cards = new ArrayList<>();
                for (int i = 0; i < entry.getValue().length; i++) {
                    cards.add(player.peekCard(entry.getValue()[i]));
                }
                this.christmasMap.put(target, cards);
            }
            player.clearHand();
        }
        if (this.eventResponses.size() == this.listOfPlayers.size()) {
            performMerryChristmas();
        }
    }

    private void performMerryChristmas() {
        timer.cancel();
        this.gameService.sendAnimationSpeed(this.lobbyId, 0);
        sendCompleteGameState();
        for (Map.Entry<Player, List<Card>> entry : this.christmasMap.entrySet()) {
            for (Card card : entry.getValue()) {
                entry.getKey().pushCardToHand(card);
            }
        }
        this.gameService.sendAnimationSpeed(this.lobbyId, 500);
        sendCompleteGameState();
        this.eventResponses = new ArrayList<>();
        this.christmasMap = new HashMap<>();
        finishTurn();
    }

    public void prepareMarket(String identity, int card) {
        Player player = getPlayerByIdentity(identity);
        if (player != null) {
            Card choice = this.marketList.remove(card);
            performMarket(player, choice);
        }
    }

    private void prepareRandomMarket(Player player) {
        Card choice = this.marketList.remove(FranticUtils.random.nextInt(this.marketList.size()));
        performMarket(player, choice);
    }

    private void performMarket(Player player, Card choice) {
        player.pushCardToHand(choice);
        this.sendCompleteGameState();

        if (!this.marketList.isEmpty()) {
            int numOfPlayers = this.listOfPlayers.size();
            int initiatorIndex = this.listOfPlayers.indexOf(currentPlayer);
            Player nextPlayer = this.listOfPlayers.get((initiatorIndex + 1) % numOfPlayers);
            this.gameService.sendMarketWindow(this.lobbyId, nextPlayer, this.marketList);
            this.gameService.sendTimer(this.lobbyId, 15);
        }
        else {
            this.marketList = new ArrayList<>();
            finishTurn();
        }
    }

    public void prepareGamblingMan(String identity, int card) {
        Player player = getPlayerByIdentity(identity);
        if (player != null) {
            this.eventResponses.add(player);
            this.gamblingManMap.put(player, card);
        }
        if (this.eventResponses.size() == this.listOfPlayers.size()) {
            performGamblingMan();
        }
    }

    private void performGamblingMan() {
        Value max = Value.ONE;
        List<Player> highest = new ArrayList<>();
        for (Player player : this.listOfPlayers) {
            if (this.gamblingManMap.containsKey(player)) {
                Integer cardIndex = this.gamblingManMap.get(player);
                Card card = player.peekCard(cardIndex);
                if (card.getValue().ordinal() > max.ordinal()) {
                    max = card.getValue();
                    highest.add(player);
                }
            }
        }
        Player loser;
        if (highest.size() > 1) {
            loser = highest.get(FranticUtils.random.nextInt(highest.size()));
        }
        else {
            loser = highest.get(0);
        }
        Chat chat = new Chat("event", "event:gambling-man", loser.getUsername() + " gambles wrong and collects " + this.gamblingManMap.size() + " cards.");
        this.gameService.sendChatMessage(this.lobbyId, chat);
        for (Map.Entry<Player, Integer> entry : this.gamblingManMap.entrySet()) {
            if (!entry.getKey().equals(loser)) {
                loser.pushCardToHand(entry.getKey().popCard(entry.getValue()));
            }
        }

        sendCompleteGameState();
        this.eventResponses = new ArrayList<>();
        this.surprisePartyMap = new HashMap<>();
        finishTurn();
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

    public void addEventResponses(Player player) {
        this.eventResponses.add(player);
    }

    public int getEventResponsesSize() {
        return this.eventResponses.size();
    }

    public void clearEventResponses() {
        this.eventResponses = new ArrayList<>();
    }

    public void setGamblingManMap(Player player, int card) {
        this.gamblingManMap.put(player, card);
    }

    public void setMarketList(List<Card> cards) {
        this.marketList = cards;
    }

    public void setTimeBomb() {
        this.timeBomb = true;
    }

    private void changePlayer() {
        if (!this.listOfPlayers.isEmpty()) {
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
    }

    //a Gameround is over, if someone has 0 cards in his hand (and no nice-try was played)
    // or in case of the time-bomb event, if the 3 rounds are played
    private boolean isRoundOver() {
        return (getHandSizes().containsValue(0));
    }

    private boolean isTimeBombExploding() {
        return this.bombMap.get(this.currentPlayer) >= 4;
    }

    public void onRoundOver() {
        this.timer.cancel();
        int maxPoints = 0;
        Player playerWithMaxPoints = this.currentPlayer; //to make sure playerWithMaxPoints is initialized in all cases
        for (Player player : listOfPlayers) {
            player.setBlocked(false);

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

    //needed for color wish after the nice try was played
    public void startInterTurnTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                prepareNewTurn();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startAnimationTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                performEvent();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startAllSeeingEyeTimer(int seconds) {
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

    public void startRecessionTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                eventResponses = new ArrayList<>();
                finishTurn();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startSurprisePartyTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                performSurpriseParty();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startMerryChristmasTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                performMerryChristmas();
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startMarketTimer(int seconds, Player player) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                prepareRandomMarket(player);
            }
        };
        this.timer.schedule(timerTask, milliseconds);
    }

    public void startGamblingManTimer(int seconds) {
        int milliseconds = seconds * 1000;
        this.timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                performGamblingMan();
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

    public void setShowCards(boolean show) {
        this.showCards = show;
        sendGameState();
    }

    public List<Player> getListOfPlayers() {
        return this.listOfPlayers;
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    public GameService getGameService() {
        return this.gameService;
    }

    public String getLobbyId() {
        return this.lobbyId;
    }

    public Pile<Card> getDiscardPile() {
        return this.discardPile;
    }

    public Pile<Card> getDrawStack() {
        return this.drawStack;
    }

    public int getDrawStackSize() {
        return this.drawStack.size();
    }

    private void initEvents() {
        //initialize all Events and add them to the list
        this.events.add(new CharityEvent(this));
        this.events.add(new CommunismEvent(this));
        this.events.add(new DoomsdayEvent(this.game, this));
        this.events.add(new EarthquakeEvent(this));
        this.events.add(new ExpansionEvent(this));
        this.events.add(new FinishLineEvent(game, this));
        this.events.add(new FridayTheThirteenthEvent(this));
        this.events.add(new GamblingManEvent(this));
        this.events.add(new MarketEvent(this));
        this.events.add(new MerryChristmasEvent(this));
        this.events.add(new MatingSeasonEvent(this));
        this.events.add(new MexicanStandoffEvent(this));
        this.events.add(new RecessionEvent(this));
        this.events.add(new RobinHoodEvent(this));
        this.events.add(new SurprisePartyEvent(this));
        this.events.add(new TheAllSeeingEyeEvent(this));
        this.events.add(new ThirdTimeLuckyEvent(this));
        this.events.add(new TimeBombEvent(this));
        this.events.add(new TornadoEvent(this));
        this.events.add(new VandalismEvent(this));

        Collections.shuffle(this.events);
    }
}