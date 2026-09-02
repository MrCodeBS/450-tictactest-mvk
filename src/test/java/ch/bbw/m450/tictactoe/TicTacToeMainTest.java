package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

class TicTacToeMainTest {

    @Test
    void isWin_detects_horizontal_win() {
        Stone[] board = {
            Stone.CROSS, Stone.CROSS, Stone.CROSS,
            null, null, null,
            null, null, null
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isTrue();
    }

    @Test
    void isWin_detects_diagonal_win() {
        Stone[] board = {
            Stone.CIRCLE, null, Stone.CROSS,
            null, Stone.CIRCLE, null,
            Stone.CROSS, null, Stone.CIRCLE
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isTrue();
    }

    @Test
    void isWin_returns_false_when_board_has_no_winner() {
        Stone[] board = {
            Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
            Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
            Stone.CIRCLE, Stone.CROSS, null
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isFalse();
        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isFalse();
    }

    @Test
    void play_rejects_identical_players() {
        GreedyPlayer player = new GreedyPlayer();

        assertThrows(IllegalArgumentException.class, () -> TicTacToeMain.play(player, player));
    }

    @Test
    void greedyPlayer_plays_first_available_slot() {
        GreedyPlayer player = new GreedyPlayer();
        Stone[] board = {
            Stone.CROSS, null, null,
            null, null, null,
            null, null, null
        };

        assertThat(player.play(board, Stone.CIRCLE)).isEqualTo(1);
    }
}
