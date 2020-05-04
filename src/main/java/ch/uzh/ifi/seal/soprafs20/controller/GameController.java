package ch.uzh.ifi.seal.soprafs20.controller;

import ch.uzh.ifi.seal.soprafs20.service.GameService;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.DrawDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.StartGameDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.incoming.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/lobby/{lobbyId}/start-game")
    public void startGame(@DestinationVariable String lobbyId,
                          SimpMessageHeaderAccessor sha, StartGameDTO dto) {
        String identity = sha.getUser().getName();
        gameService.startGame(lobbyId, identity);
    }

    @MessageMapping("/lobby/{lobbyId}/play")
    public void playCard(@DestinationVariable String lobbyId,
                         SimpMessageHeaderAccessor sha, PlayCardDTO dto) {
        String identity = sha.getUser().getName();
        gameService.playCard(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/draw")
    public void newChatMessage(@DestinationVariable String lobbyId,
                               SimpMessageHeaderAccessor sha, DrawDTO dto) {
        String identity = sha.getUser().getName();
        gameService.drawCard(lobbyId, identity);
    }

    @MessageMapping("/lobby/{lobbyId}/end-turn")
    public void endTurn(@DestinationVariable String lobbyId,
                        SimpMessageHeaderAccessor sha, EndTurnDTO dto) {
        String identity = sha.getUser().getName();
        gameService.endTurn(lobbyId, identity);
    }

    @MessageMapping("/lobby/{lobbyId}/action/exchange")
    public void exchange(@DestinationVariable String lobbyId,
                         SimpMessageHeaderAccessor sha, ExchangeDTO dto) {
        String identity = sha.getUser().getName();
        gameService.exchange(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/gift")
    public void gift(@DestinationVariable String lobbyId,
                     SimpMessageHeaderAccessor sha, GiftDTO dto) {
        String identity = sha.getUser().getName();
        gameService.gift(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/skip")
    public void skip(@DestinationVariable String lobbyId,
                     SimpMessageHeaderAccessor sha, SkipDTO dto) {
        String identity = sha.getUser().getName();
        gameService.skip(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/fantastic")
    public void fantastic(@DestinationVariable String lobbyId,
                          SimpMessageHeaderAccessor sha, FantasticDTO dto) {
        String identity = sha.getUser().getName();
        gameService.fantastic(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/fantastic-four")
    public void fantasticFour(@DestinationVariable String lobbyId,
                              SimpMessageHeaderAccessor sha, FantasticFourDTO dto) {
        String identity = sha.getUser().getName();
        gameService.fantasticFour(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/equality")
    public void equality(@DestinationVariable String lobbyId,
                         SimpMessageHeaderAccessor sha, EqualityDTO dto) {
        String identity = sha.getUser().getName();
        gameService.equality(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/counterattack")
    public void counterAttack(@DestinationVariable String lobbyId,
                              SimpMessageHeaderAccessor sha, CounterAttackDTO dto) {
        String identity = sha.getUser().getName();
        gameService.counterAttack(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/nice-try")
    public void niceTry(@DestinationVariable String lobbyId,
                              SimpMessageHeaderAccessor sha, NiceTryDTO dto) {
        String identity = sha.getUser().getName();
        gameService.niceTry(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/surprise-party")
    public void surpriseParty(@DestinationVariable String lobbyId,
                          SimpMessageHeaderAccessor sha, SurprisePartyDTO dto) {
        String identity = sha.getUser().getName();
        gameService.surpriseParty(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/merry-christmas")
    public void merryChristmas(@DestinationVariable String lobbyId,
                              SimpMessageHeaderAccessor sha, MerryChristmasDTO dto) {
        String identity = sha.getUser().getName();
        gameService.merryChristmas(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/recession")
    public void recession(@DestinationVariable String lobbyId,
                              SimpMessageHeaderAccessor sha, RecessionDTO dto) {
        String identity = sha.getUser().getName();
        gameService.recession(lobbyId, identity, dto);
    }

    @MessageMapping("/lobby/{lobbyId}/action/market")
    public void market(@DestinationVariable String lobbyId,
                          SimpMessageHeaderAccessor sha, MarketDTO dto) {
        String identity = sha.getUser().getName();
        gameService.market(lobbyId, identity, dto);
    }
}
