package com.aghajari.simplechess.board;

import android.graphics.Point;

import com.aghajari.simplechess.pieces.ChessPiece;
import com.aghajari.simplechess.pieces.King;

import java.util.List;

public interface Board {
    int GRID = 8;

    boolean isEmpty(int x, int y);

    ChessPiece getPiece(int x, int y);

    King getKing(boolean black);

    List<ChessPiece> getPieces(boolean black);

    default boolean isUndefined(int x, int y) {
        return x < 0 || y < 0 || x >= GRID || y >= GRID;
    }

    default boolean isUnderAttack(Point position, boolean black) {
        return getUnderAttackCount(position, black, 1) == 1;
    }

    default int getUnderAttackCount(Point position, boolean black, int max) {
        List<ChessPiece> pieces = getPieces(!black);
        int count = 0;
        for (ChessPiece piece : pieces) {
            Board tmp = piece.getBoard();
            piece.setBoard(this);
            piece.setCheckMate(false);
            if (piece.getAvailableMoves().contains(position))
                count++;
            piece.setCheckMate(true);
            piece.setBoard(tmp);
            if (count == max)
                return count;
        }
        return count;
    }

    default boolean isCheck(boolean black) {
        return isUnderAttack(getKing(black).getPosition(), black);
    }

    default boolean isMate(boolean black) {
        List<ChessPiece> pieces = getPieces(black);
        for (ChessPiece piece : pieces) {
            if (piece.getAvailableMoves().size() > 0)
                return false;
        }
        return true;
    }

    default String getBoardKey() {
        return "Black:" + getPieces(true).toString() +
                "\nWhite:" + getPieces(false).toString();
    }

}
