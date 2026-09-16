package ch.bbw.m450.tictactoe;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

public final class TicTacToeTestFixtures {

    private TicTacToeTestFixtures() {
    }

    public static Stone[] board(Object... cells) {
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];

        for (int i = 0; i < cells.length && i < board.length; i++) {
            Object value = cells[i];
            if (value instanceof Stone stone) {
                board[i] = stone;
            } else if (value == null) {
                board[i] = null;
            } else {
                throw new IllegalArgumentException("Only Stone values or null are allowed in test fixtures");
            }
        }

        return board;
    }
}
