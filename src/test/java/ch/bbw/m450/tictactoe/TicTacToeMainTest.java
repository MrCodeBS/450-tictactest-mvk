package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

class TicTacToeMainTest {

    private static Stream<Arguments> winningBoards() {
        return Stream.of(
            Arguments.of(
                "horizontal row top",
                TicTacToeTestFixtures.board(
                    Stone.CROSS, Stone.CROSS, Stone.CROSS,
                    null, null, null,
                    null, null, null
                ),
                Stone.CROSS,
                true
            ),
            Arguments.of(
                "vertical column center",
                TicTacToeTestFixtures.board(
                    null, Stone.CIRCLE, null,
                    null, Stone.CIRCLE, null,
                    null, Stone.CIRCLE, null
                ),
                Stone.CIRCLE,
                true
            ),
            Arguments.of(
                "diagonal descending",
                TicTacToeTestFixtures.board(
                    Stone.CIRCLE, null, Stone.CROSS,
                    null, Stone.CIRCLE, null,
                    Stone.CROSS, null, Stone.CIRCLE
                ),
                Stone.CIRCLE,
                true
            ),
            Arguments.of(
                "anti-diagonal",
                TicTacToeTestFixtures.board(
                    Stone.CROSS, null, Stone.CIRCLE,
                    null, Stone.CIRCLE, null,
                    Stone.CIRCLE, null, Stone.CROSS
                ),
                Stone.CIRCLE,
                true
            )
        );
    }

    private static Stream<Arguments> nonWinningBoards() {
        return Stream.of(
            Arguments.of(
                "mixed board without winner",
                TicTacToeTestFixtures.board(
                    Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
                    Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
                    Stone.CIRCLE, Stone.CROSS, null
                ),
                Stone.CROSS,
                false
            ),
            Arguments.of(
                "empty board",
                TicTacToeTestFixtures.board(
                    null, null, null,
                    null, null, null,
                    null, null, null
                ),
                Stone.CROSS,
                false
            ),
            Arguments.of(
                "partial board with no three in a row",
                TicTacToeTestFixtures.board(
                    Stone.CROSS, Stone.CIRCLE, null,
                    null, Stone.CROSS, Stone.CIRCLE,
                    Stone.CIRCLE, null, Stone.CROSS
                ),
                Stone.CIRCLE,
                false
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("winningBoards")
    void isWin_detects_winning_board(String description, Stone[] board, Stone color, boolean expected) {
        assertThat(TicTacToeMain.isWin(board, color)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonWinningBoards")
    void isWin_rejects_non_winning_board(String description, Stone[] board, Stone color, boolean expected) {
        assertThat(TicTacToeMain.isWin(board, color)).isEqualTo(expected);
    }

    @Test
    void play_rejects_identical_players() {
        GreedyPlayer player = new GreedyPlayer();

        assertThrows(IllegalArgumentException.class, () -> TicTacToeMain.play(player, player));
    }

    @Test
    void greedyPlayer_plays_first_available_slot() {
        GreedyPlayer player = new GreedyPlayer();
        Stone[] board = TicTacToeTestFixtures.board(
            Stone.CROSS, null, null,
            null, null, null,
            null, null, null
        );

        assertThat(player.play(board, Stone.CIRCLE)).isEqualTo(1);
    }
}
