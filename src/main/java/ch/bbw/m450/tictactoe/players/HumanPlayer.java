package ch.bbw.m450.tictactoe.players;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Human player taking input from stdin (or a custom InputStream / Scanner for testing).
 */
public class HumanPlayer implements TicTacToePlayer {

	private final Scanner scanner;

	public HumanPlayer() {
		this(System.in);
	}

	public HumanPlayer(InputStream in) {
		this(new Scanner(in, StandardCharsets.UTF_8));
	}

	public HumanPlayer(Scanner scanner) {
		this.scanner = scanner;
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		while (true) {
			System.out.println(TicTacToeMain.toString(board) + "where to to put the next " + colorToPlay + "? (0-8): ");
			if (!scanner.hasNextLine()) {
				throw new IllegalStateException("No input available for HumanPlayer");
			}
			String line = scanner.nextLine().trim();
			try {
				int move = Integer.parseInt(line);
				if (move >= 0 && move < TicTacToeMain.BOARD_SIZE && board[move] == null) {
					return move;
				}
				if (move >= 0 && move < TicTacToeMain.BOARD_SIZE) {
					System.out.println("Position " + move + " is already occupied. Choose another field.");
				} else {
					System.out.println("Invalid position: " + move + ". Must be between 0 and 8.");
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid input '" + line + "'. Please enter a valid number (0-8).");
			}
		}
	}
}
