package com.aghajari.simplechess.pieces;

import android.graphics.Point;

import androidx.annotation.NonNull;

import com.aghajari.simplechess.R;

import java.util.ArrayList;
import java.util.List;

public class King extends ChessPiece {

    public King(int positionX, int positionY, boolean isBlack) {
        super(positionX, positionY, isBlack);
    }

    @Override
    public int getImage() {
        return isBlack() ? R.drawable.chesspiece_king_black_default
                : R.drawable.chesspiece_king_white_default;
    }

    @NonNull
    @Override
    public List<Point> getAvailableMoves(Point currentPosition) {
        List<Point> list = new ArrayList<>(8);

        int[][] moves = new int[][]{
                {0, +1}, {0, -1},
                {+1, 0}, {-1, 0},
                {+1, +1}, {-1, +1},
                {+1, -1}, {-1, -1}
        };

        for (int[] move : moves)
            validateAndAddMove(list, currentPosition.x + move[0], currentPosition.y + move[1]);

        // Castling
        if (!findOnlyAttackMoves() && isFirstMove()) {
            // Castling king side (castling short)
            if (checkCastling(currentPosition, true))
                list.add(new Point(6, currentPosition.y));

            // Castling queen side (castling long)
            if (checkCastling(currentPosition, false))
                list.add(new Point(2, currentPosition.y));
        }

        return list;
    }

    @Override
    public String getSymbol() {
        return "K";
    }

    private boolean checkCastling(Point position, boolean kingSide) {
        ChessPiece piece = getBoard().getPiece(kingSide ? 7 : 0, position.y);
        if (piece instanceof Rook && piece.isFirstMove()) {
            if (getBoard().isCheck(isBlack()))
                return false;

            int[] xs;
            if (kingSide)
                xs = new int[]{5, 6};
            else
                xs = new int[]{2, 3};

            for (int x : xs) {
                if (getBoard().getPiece(x, position.y) != null
                        || getBoard().isUnderAttack(new Point(x, position.y), isBlack()))
                    return false;
            }

            return true;
        }
        return false;
    }
}
