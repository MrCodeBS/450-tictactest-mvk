package ch.bbw.m450.tictactoe.players;

import java.util.Arrays;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * An unbeatable player using the minimax algorithm.
 * Guarantees a win or at least a draw in every game.
 */
public class PerfectPlayer implements TicTacToePlayer {

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		Stone[] current = Arrays.copyOf(board, board.length);
		int bestMove = -1;
		int bestScore = Integer.MIN_VALUE;

		for (int i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
			if (current[i] == null) {
				current[i] = colorToPlay;
				int score = minimax(current, 0, false, colorToPlay);
				current[i] = null;
				if (score > bestScore) {
					bestScore = score;
					bestMove = i;
				}
			}
		}

		if (bestMove == -1) {
			throw new IllegalStateException("cannot play at all");
		}
		return bestMove;
	}

	private int minimax(Stone[] board, int depth, boolean isMaximizing, Stone myColor) {
		Stone opponentColor = myColor.opponent();

		if (TicTacToeMain.isWin(board, myColor)) {
			return 10 - depth;
		}
		if (TicTacToeMain.isWin(board, opponentColor)) {
			return depth - 10;
		}
		if (isBoardFull(board)) {
			return 0;
		}

		if (isMaximizing) {
			int maxScore = Integer.MIN_VALUE;
			for (int i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
				if (board[i] == null) {
					board[i] = myColor;
					int score = minimax(board, depth + 1, false, myColor);
					board[i] = null;
					maxScore = Math.max(maxScore, score);
				}
			}
			return maxScore;
		} else {
			int minScore = Integer.MAX_VALUE;
			for (int i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
				if (board[i] == null) {
					board[i] = opponentColor;
					int score = minimax(board, depth + 1, true, myColor);
					board[i] = null;
					minScore = Math.min(minScore, score);
				}
			}
			return minScore;
		}
	}

	private boolean isBoardFull(Stone[] board) {
		for (Stone stone : board) {
			if (stone == null) {
				return false;
			}
		}
		return true;
	}
}
