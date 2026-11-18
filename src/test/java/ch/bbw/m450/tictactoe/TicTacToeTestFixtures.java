package ch.bbw.m450.tictactoe;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

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

    public static Stream<Arguments> winningBoards() {
        return Stream.of(
            // Row 0
            Arguments.of("horizontal row top",
                board(Stone.CROSS, Stone.CROSS, Stone.CROSS, null, null, null, null, null, null),
                Stone.CROSS, true),
            // Row 1
            Arguments.of("horizontal row middle",
                board(null, null, null, Stone.CIRCLE, Stone.CIRCLE, Stone.CIRCLE, null, null, null),
                Stone.CIRCLE, true),
            // Row 2
            Arguments.of("horizontal row bottom",
                board(null, null, null, null, null, null, Stone.CROSS, Stone.CROSS, Stone.CROSS),
                Stone.CROSS, true),
            // Column 0
            Arguments.of("vertical column left",
                board(Stone.CROSS, null, null, Stone.CROSS, null, null, Stone.CROSS, null, null),
                Stone.CROSS, true),
            // Column 1
            Arguments.of("vertical column center",
                board(null, Stone.CIRCLE, null, null, Stone.CIRCLE, null, null, Stone.CIRCLE, null),
                Stone.CIRCLE, true),
            // Column 2
            Arguments.of("vertical column right",
                board(null, null, Stone.CROSS, null, null, Stone.CROSS, null, null, Stone.CROSS),
                Stone.CROSS, true),
            // Diagonal 1
            Arguments.of("diagonal descending (0, 4, 8)",
                board(Stone.CIRCLE, null, null, null, Stone.CIRCLE, null, null, null, Stone.CIRCLE),
                Stone.CIRCLE, true),
            // Diagonal 2
            Arguments.of("anti-diagonal (2, 4, 6)",
                board(null, null, Stone.CROSS, null, Stone.CROSS, null, Stone.CROSS, null, null),
                Stone.CROSS, true)
        );
    }

    public static Stream<Arguments> nonWinningBoards() {
        return Stream.of(
            Arguments.of("mixed board without winner",
                board(Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
                      Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
                      Stone.CIRCLE, Stone.CROSS, null),
                Stone.CROSS, false),
            Arguments.of("empty board",
                board(null, null, null, null, null, null, null, null, null),
                Stone.CROSS, false),
            Arguments.of("partial board with no three in a row",
                board(Stone.CROSS, Stone.CIRCLE, null,
                      null, Stone.CROSS, Stone.CIRCLE,
                      Stone.CIRCLE, null, Stone.CROSS),
                Stone.CIRCLE, false),
            Arguments.of("opponent has three in a row, checked for other color",
                board(Stone.CROSS, Stone.CROSS, Stone.CROSS, null, null, null, null, null, null),
                Stone.CIRCLE, false)
        );
    }
}
