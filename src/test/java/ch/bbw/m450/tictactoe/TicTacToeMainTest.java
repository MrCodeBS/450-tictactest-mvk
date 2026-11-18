package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.HumanPlayer;

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
    void play_rejects_negative_move() {
        TicTacToePlayer invalidPlayer = (board, color) -> -1;
        GreedyPlayer greedy = new GreedyPlayer();

        assertThrows(IllegalStateException.class, () -> TicTacToeMain.play(invalidPlayer, greedy));
    }

    @Test
    void play_rejects_out_of_bounds_move() {
        TicTacToePlayer invalidPlayer = (board, color) -> 9;
        GreedyPlayer greedy = new GreedyPlayer();

        assertThrows(IllegalStateException.class, () -> TicTacToeMain.play(invalidPlayer, greedy));
    }

    @Test
    void play_rejects_move_on_occupied_slot() {
        // Player tries to play position 0, which was already occupied in move 1
        TicTacToePlayer badPlayer = (board, color) -> 0;
        GreedyPlayer greedy = new GreedyPlayer();

        assertThrows(IllegalStateException.class, () -> TicTacToeMain.play(greedy, badPlayer));
    }

    @Test
    void play_returns_winner_for_player_x() {
        // Player X wins horizontally on row 0: moves 0, 1, 2
        // Player O plays positions 3, 4
        int[] movesX = {0, 1, 2};
        int[] movesO = {3, 4};
        final int[] indexX = {0};
        final int[] indexO = {0};

        TicTacToePlayer playerX = (board, color) -> movesX[indexX[0]++];
        TicTacToePlayer playerO = (board, color) -> movesO[indexO[0]++];

        Stone winner = TicTacToeMain.play(playerX, playerO);
        assertThat(winner).isEqualTo(Stone.CROSS);
    }

    @Test
    void play_returns_winner_for_player_o() {
        // X plays 0, 3, 7
        // O plays 1, 4, 7 -> wait, O wins on col 1: 1, 4, 7 (X plays 0, 2, 8)
        int[] movesX = {0, 2, 8};
        int[] movesO = {1, 4, 7};
        final int[] indexX = {0};
        final int[] indexO = {0};

        TicTacToePlayer playerX = (board, color) -> movesX[indexX[0]++];
        TicTacToePlayer playerO = (board, color) -> movesO[indexO[0]++];

        Stone winner = TicTacToeMain.play(playerX, playerO);
        assertThat(winner).isEqualTo(Stone.CIRCLE);
    }

    @Test
    void play_returns_null_on_draw() {
        // Predetermined sequence leading to a draw:
        // Board:
        // X | O | X
        // X | O | O
        // O | X | X
        // Moves: X: 0, 2, 3, 7, 8
        //        O: 1, 4, 5, 6
        int[] movesX = {0, 2, 3, 7, 8};
        int[] movesO = {1, 4, 5, 6};
        final int[] indexX = {0};
        final int[] indexO = {0};

        TicTacToePlayer playerX = (board, color) -> movesX[indexX[0]++];
        TicTacToePlayer playerO = (board, color) -> movesO[indexO[0]++];

        Stone winner = TicTacToeMain.play(playerX, playerO);
        assertThat(winner).isNull();
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

    @Test
    void greedyPlayer_throws_when_no_slot_available() {
        GreedyPlayer player = new GreedyPlayer();
        Stone[] fullBoard = TicTacToeTestFixtures.board(
            Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
            Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
            Stone.CIRCLE, Stone.CROSS, Stone.CROSS
        );

        assertThrows(IllegalStateException.class, () -> player.play(fullBoard, Stone.CROSS));
    }

    @Test
    void humanPlayer_reads_valid_move_from_injected_stream() {
        ByteArrayInputStream in = new ByteArrayInputStream("4\n".getBytes(StandardCharsets.UTF_8));
        HumanPlayer player = new HumanPlayer(in);
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(4);
    }

    @Test
    void humanPlayer_throws_when_stream_is_exhausted() {
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        HumanPlayer player = new HumanPlayer(in);
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];

        assertThrows(IllegalStateException.class, () -> player.play(board, Stone.CROSS));
    }

    @Test
    void toString_renders_empty_and_populated_board() {
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];
        board[0] = Stone.CROSS;
        board[4] = Stone.CIRCLE;

        String rendered = TicTacToeMain.toString(board);
        assertThat(rendered).contains("X").contains("O").contains("1").contains("8");
    }

    @Test
    void testFixtures_throws_on_invalid_cell_type() {
        assertThrows(IllegalArgumentException.class, () -> TicTacToeTestFixtures.board("not_a_stone"));
    }

    @Test
    void ticTacToeMain_instantiation() {
        assertThat(new TicTacToeMain()).isNotNull();
    }
}
