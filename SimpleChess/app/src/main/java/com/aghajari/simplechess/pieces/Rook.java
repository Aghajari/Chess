package com.aghajari.simplechess.pieces;

import android.graphics.Point;

import androidx.annotation.NonNull;

import com.aghajari.simplechess.R;

import java.util.ArrayList;
import java.util.List;

public class Rook extends ChessPiece {

    public Rook(int positionX, int positionY, boolean isBlack) {
        super(positionX, positionY, isBlack);
    }

    @Override
    public int getImage() {
        return isBlack() ? R.drawable.chesspiece_rook_black_default
                : R.drawable.chesspiece_rook_white_default;
    }

    @NonNull
    @Override
    public List<Point> getAvailableMoves(Point currentPosition) {
        ArrayList<Point> list = new ArrayList<>();

        int[][] moves = new int[][]{
                {-1, 0}, {+1, 0},
                {0, -1}, {0, +1}
        };

        for(int[] move : moves) {
            int x = currentPosition.x + move[0];
            int y = currentPosition.y + move[1];

            while (!getBoard().isUndefined(x, y) && validateAndAddMove(list, x, y)) {
                x += move[0];
                y += move[1];
            }
        }
        return list;
    }

    @Override
    public String getSymbol() {
        return "R";
    }
}
