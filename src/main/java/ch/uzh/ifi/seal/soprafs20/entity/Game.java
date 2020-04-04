package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.GameLength;

import java.util.ArrayList;
import java.util.List;

public class Game implements Runnable {

    private GameLength gameDuration;

    private ArrayList<Player> listOfPlayers;

    private int maxPoints;

    private Player firstPlayer;

    private Player winner;

    public Game(GameLength gameDuration, ArrayList<Player> listOfPlayers) {
        this.gameDuration = gameDuration;
        this.listOfPlayers = listOfPlayers;
        this.maxPoints = calculateMaxPoints();
    }

    @Override
    public void run() {
    }

    private void StartNewGameRound() {
    }

    private Player[] getScores() {
        return null;
    }

    private boolean gameOver() {
        return false;
    }

    private void changeFirstPlayer() {
        int playersIndex = this.listOfPlayers.indexOf(this.firstPlayer);
        playersIndex = (playersIndex + 1)%listOfPlayers.size();
        this.firstPlayer = listOfPlayers.get(playersIndex);
    }

    private int calculateMaxPoints() {
        int numOfPlayers = this.listOfPlayers.size();
        if (numOfPlayers <= 4) {
            if (gameDuration == GameLength.SHORT) { return 137;
            } else if (gameDuration == GameLength.MEDIUM) { return 154;
            } else { return 179; }
        } else {
            if (gameDuration == GameLength.SHORT) { return 113;
            } else if (gameDuration == GameLength.MEDIUM) { return 137;
            } else { return 154; }
        }
    }
}
