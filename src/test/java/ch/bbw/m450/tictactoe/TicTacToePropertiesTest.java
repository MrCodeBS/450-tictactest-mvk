package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.PerfectPlayer;

class TicTacToePropertiesTest {

    @Property
    void stone_opponent_isSelfInverting(@ForAll("stones") Stone stone) {
        assertThat(stone.opponent().opponent()).isEqualTo(stone);
        assertThat(stone.opponent()).isNotEqualTo(stone);
    }

    @Property
    void greedyPlayer_alwaysSelectsAvailableSlot(@ForAll("partiallyFilledBoardsWithAtLeastOneEmpty") Stone[] board) {
        GreedyPlayer player = new GreedyPlayer();
        int move = player.play(board, Stone.CROSS);

        assertThat(move).isBetween(0, TicTacToeMain.BOARD_SIZE - 1);
        assertThat(board[move]).isNull();
    }

    @Property
    void perfectPlayer_alwaysSelectsAvailableSlot(@ForAll("partiallyFilledBoardsWithAtLeastOneEmpty") Stone[] board) {
        PerfectPlayer player = new PerfectPlayer();
        int move = player.play(board, Stone.CIRCLE);

        assertThat(move).isBetween(0, TicTacToeMain.BOARD_SIZE - 1);
        assertThat(board[move]).isNull();
    }

    @Property
    void isWin_requiresAtLeastThreeStones(@ForAll("anyBoard") Stone[] board, @ForAll("stones") Stone color) {
        if (TicTacToeMain.isWin(board, color)) {
            long count = 0;
            for (Stone s : board) {
                if (s == color) {
                    count++;
                }
            }
            assertThat(count).isGreaterThanOrEqualTo(3);
        }
    }

    @Provide
    Arbitrary<Stone> stones() {
        return Arbitraries.of(Stone.CROSS, Stone.CIRCLE);
    }

    @Provide
    Arbitrary<Stone[]> anyBoard() {
        return Arbitraries.of(Stone.CROSS, Stone.CIRCLE, null)
            .list().ofSize(TicTacToeMain.BOARD_SIZE)
            .map(list -> list.toArray(new Stone[0]));
    }

    @Provide
    Arbitrary<Stone[]> partiallyFilledBoardsWithAtLeastOneEmpty() {
        return anyBoard().filter(board -> {
            for (Stone s : board) {
                if (s == null) {
                    return true;
                }
            }
            return false;
        });
    }
}
