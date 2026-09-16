package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

class TicTacToeMainTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("ch.bbw.m450.tictactoe.TicTacToeTestFixtures#winningBoards")
    void isWin_detects_winning_board(String description, Stone[] board, Stone color, boolean expected) {
        assertThat(TicTacToeMain.isWin(board, color)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ch.bbw.m450.tictactoe.TicTacToeTestFixtures#nonWinningBoards")
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
