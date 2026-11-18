package ch.bbw.m450.tictactoe.players;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestFixtures;

class PerfectPlayerTest {

    @Test
    void perfectPlayer_winsImmediately_whenWinningMoveAvailable() {
        PerfectPlayer player = new PerfectPlayer();
        // Row 0 has two CROSS stones, position 2 wins
        Stone[] board = TicTacToeTestFixtures.board(
            Stone.CROSS, Stone.CROSS, null,
            Stone.CIRCLE, Stone.CIRCLE, null,
            null, null, null
        );

        int move = player.play(board, Stone.CROSS);
        assertThat(move).isEqualTo(2);
    }

    @Test
    void perfectPlayer_blocksOpponentWin_whenOpponentThreatens() {
        PerfectPlayer player = new PerfectPlayer();
        // Opponent (CROSS) has positions 0 and 1, CIRCLE must block position 2
        Stone[] board = TicTacToeTestFixtures.board(
            Stone.CROSS, Stone.CROSS, null,
            null, Stone.CIRCLE, null,
            null, null, null
        );

        int move = player.play(board, Stone.CIRCLE);
        assertThat(move).isEqualTo(2);
    }

    @Test
    void perfectPlayer_neverLosesAgainstGreedyPlayer_whenPlayingFirst() {
        PerfectPlayer perfect = new PerfectPlayer();
        GreedyPlayer greedy = new GreedyPlayer();

        Stone winner = TicTacToeMain.play(perfect, greedy);
        // PerfectPlayer starting first must win against the naive GreedyPlayer
        assertThat(winner).isEqualTo(Stone.CROSS);
    }

    @Test
    void perfectPlayer_neverLosesAgainstGreedyPlayer_whenPlayingSecond() {
        GreedyPlayer greedy = new GreedyPlayer();
        PerfectPlayer perfect = new PerfectPlayer();

        Stone winner = TicTacToeMain.play(greedy, perfect);
        // Even second, PerfectPlayer must win or draw, never lose to GreedyPlayer
        assertThat(winner).isIn(Stone.CIRCLE, null);
    }

    @Test
    void perfectPlayer_drawsAgainstAnotherPerfectPlayer() {
        PerfectPlayer playerX = new PerfectPlayer();
        PerfectPlayer playerO = new PerfectPlayer();

        Stone winner = TicTacToeMain.play(playerX, playerO);
        // Two perfect players always result in a draw
        assertThat(winner).isNull();
    }

    @Test
    void perfectPlayer_throwsException_whenBoardIsFull() {
        PerfectPlayer player = new PerfectPlayer();
        Stone[] fullBoard = TicTacToeTestFixtures.board(
            Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
            Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
            Stone.CIRCLE, Stone.CROSS, Stone.CROSS
        );

        assertThrows(IllegalStateException.class, () -> player.play(fullBoard, Stone.CROSS));
    }
}
