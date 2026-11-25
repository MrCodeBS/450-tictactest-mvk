package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.StdIn;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.HumanPlayer;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TicTacToeConsoleE2ETest {

    @Test
    @StdIo({"0", "3", "6"})
    @DisplayName("E2E: Human as CROSS wins against GreedyPlayer with full game flow")
    void e2e_humanWinsAsCrossAgainstGreedy(StdIn in, StdOut out) {
        HumanPlayer human = new HumanPlayer();
        GreedyPlayer greedy = new GreedyPlayer();

        Stone winner = TicTacToeMain.play(human, greedy);

        assertThat(winner).isEqualTo(Stone.CROSS);
        assertThat(out.capturedLines())
            .anyMatch(line -> line.contains("...and the winner is: CROSS"));
    }

    @Test
    @StdIo({"4", "5"})
    @DisplayName("E2E: GreedyPlayer as CROSS wins against Human as CIRCLE")
    void e2e_greedyWinsAsCrossAgainstHuman(StdIn in, StdOut out) {
        GreedyPlayer greedy = new GreedyPlayer();
        HumanPlayer human = new HumanPlayer();

        Stone winner = TicTacToeMain.play(greedy, human);

        assertThat(winner).isEqualTo(Stone.CROSS);
        assertThat(out.capturedLines())
            .anyMatch(line -> line.contains("...and the winner is: CROSS"));
    }

    @Test
    @StdIo({"invalid_text", "-5", "99", "4"})
    @DisplayName("E2E: Malicious and invalid input is rejected until valid move is entered")
    void e2e_maliciousAndInvalidInputRejected(StdIn in, StdOut out) {
        HumanPlayer human = new HumanPlayer();
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];

        int move = human.play(board, Stone.CROSS);

        assertThat(move).isEqualTo(4);
        assertThat(out.capturedLines())
            .anyMatch(line -> line.contains("Invalid input 'invalid_text'"))
            .anyMatch(line -> line.contains("Invalid position: -5"))
            .anyMatch(line -> line.contains("Invalid position: 99"));
    }

    @Test
    @StdIo({"0", "4"})
    @DisplayName("E2E: Occupied cell input is rejected, asking user again")
    void e2e_occupiedCellRejected(StdIn in, StdOut out) {
        HumanPlayer human = new HumanPlayer();
        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];
        board[0] = Stone.CIRCLE; // Already taken

        int move = human.play(board, Stone.CROSS);

        assertThat(move).isEqualTo(4);
        assertThat(out.capturedLines())
            .anyMatch(line -> line.contains("Position 0 is already occupied"));
    }

    @Test
    @StdIo({"0", "1", "2", "3", "4", "5", "6", "7", "8"})
    @DisplayName("E2E: Full application entry point TicTacToeMain.main runs and terminates without DoS")
    void e2e_mainMethodExecutesCleanly(StdIn in, StdOut out) {
        TicTacToeMain.main(new String[0]);

        // main() spielt gegen den PerfectPlayer: das Spiel endet, und der Mensch (CROSS) kann nicht gewinnen
        assertThat(out.capturedLines())
            .anyMatch(line -> line.contains("winner is: CIRCLE") || line.contains("draw"))
            .noneMatch(line -> line.contains("winner is: CROSS"));
    }
}
