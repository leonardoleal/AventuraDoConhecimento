package br.com.lealweb.aventuradoconhecimento.jogomemoria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.lealweb.aventuradoconhecimento.jogomemoria.repositorie.Dificulty;

public class MemoryGame implements Serializable {

	public interface GameListener {

		void gameOver(MemoryGame game);
		void gamePaused(MemoryGame game);

		void gameResumed(MemoryGame game);

		void gameStarted(MemoryGame game);

	}
	private static final int CLEAR_NOTHING = -3;

	public static final int INT_CLEAR = -2;
	public static final int INT_UNKNOWN = -1;
	private static final long serialVersionUID = 0L;

	private int clickX = -1;

	private int clickY = -1;
	public int[][] displayedBoard;

	private int[][] gameBoard;
	public int height;
	public int width;
	private int amountOfCards;
	private Dificulty dificulty;

	private transient GameListener listener;
	private int objectToClearAfterTimeout = -1;
	private boolean waitingForTimeout = false;
	private boolean paused = true;
	private boolean started = false;
	private long startTime = -1; // set on start

	private long storedTime = 0;

	private int score = 0;

	public MemoryGame(Dificulty dificulty) {
		setDificulty(dificulty);

		setup();
		shuffle();
	}

	private void setup() {
		this.gameBoard = new int[width][height];
		this.displayedBoard = new int[width][height];
	}

	public void afterTimeout() {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (displayedBoard[i][j] == objectToClearAfterTimeout)
					displayedBoard[i][j] = INT_CLEAR;
				else if (displayedBoard[i][j] != INT_UNKNOWN && displayedBoard[i][j] != INT_CLEAR) {
					displayedBoard[i][j] = INT_UNKNOWN;
				}
			}
		}
		objectToClearAfterTimeout = CLEAR_NOTHING;
		waitingForTimeout = false;
	}

	/** Return wheter something changed (clickOnCard was "accepted"). */
	public boolean clickOnCard(int x, int y) {
		if (paused || !started)
			return false;
		int clickedObject = displayedBoard[x][y];
		if (clickedObject == INT_CLEAR)
			return false;
		if (clickedObject != INT_UNKNOWN)
			return false;

		displayedBoard[x][y] = gameBoard[x][y];
		if (clickX == -1) {
			// nothing clicked last
			clickX = x;
			clickY = y;
			return true;
		} else {
			if (x == clickX && y == clickY)
				return false;
			if (gameBoard[x][y] == gameBoard[clickX][clickY]) {
				objectToClearAfterTimeout = gameBoard[x][y];
			} else {
				objectToClearAfterTimeout = CLEAR_NOTHING;
			}

			clickX = clickY = -1;
			waitingForTimeout = true;
			return true;
		}
	}

	public long getUsedTimeMs() {
		if (paused)
			return storedTime;
		return System.currentTimeMillis() - startTime + storedTime;
	}

	public int getUsedTimeSeg() {
		return (int) (getUsedTimeMs() / 1000);
	}

	public boolean isDone() {
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (displayedBoard[i][j] != INT_CLEAR)
					return false;
		return true;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isWaitingForTimeout() {
		return waitingForTimeout;
	}

	/** Pause the game and store the used time. When returning, resume() will be called. */
	public void pause() {
		if (!paused) {
			storedTime += System.currentTimeMillis() - startTime;
			paused = true;
			listener.gamePaused(this);
		}
	}

	public void restart() {
		setup();

		listener.gameOver(this);
		clickX = clickY = -1;
		started = false;
		paused = true;
		storedTime = 0;
		score = 0;

		shuffle();
	}

	public void resume() {
		paused = false;
		startTime = System.currentTimeMillis();
		listener.gameResumed(this);
	}

	public void setListener(GameListener listener) {
		this.listener = listener;
	}

	private void shuffle() {
		int neededPieces = width * height / 2;
		if (neededPieces * 2 != width * height)
			throw new IllegalArgumentException("Illegal size: " + width + "x" + height);

		List<Object> l = new ArrayList<Object>(neededPieces);
		for (int i = 0; i < neededPieces; i++) {
			l.add(i);
			l.add(i);
		}
		Collections.shuffle(l);

		int n = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				gameBoard[i][j] = (Integer) l.get(n++);
				displayedBoard[i][j] = INT_UNKNOWN;
			}
		}
	}

	public void start() {
		started = true;
		paused = false;
		startTime = System.currentTimeMillis();
		listener.gameStarted(this);
	}

	public void changeDificulty() {
		switch (dificulty) {
			case EASY:
				dificulty = Dificulty.NORMAL;
				break;

			case NORMAL:
				dificulty = Dificulty.HARD;
				break;

			case HARD:
				dificulty = Dificulty.EASY;
		}
		setDificulty(dificulty);
	}

	public void setDificulty(Dificulty dificulty) {
		this.dificulty = dificulty;
		switch (dificulty) {
			case EASY:
				width = 2;
				height = 4;
				break;

			case NORMAL:
				width = 4;
				height = 5;
				break;

			case HARD:
				width = 6;
				height = 6;
		}

		calculateAmountOfCards();
	}

	private void calculateAmountOfCards() {
		amountOfCards = width * height / 2;
	}

	public boolean wasLastClickIncorrect() {
		return objectToClearAfterTimeout == CLEAR_NOTHING;
	}

	public void plusScore() {
		score += 20;
	}

	public void minusScore() {
		score -= 5;
	}

	public int getScore() {
		return score;
	}

	public int getAmountOfCards() {
		return amountOfCards;
	}
}
