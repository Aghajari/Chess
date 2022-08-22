package com.aghajari.simplechess.pieces;

import android.graphics.Point;

import androidx.annotation.NonNull;

import com.aghajari.simplechess.R;

import java.util.ArrayList;
import java.util.List;

public class Knight extends ChessPiece {

    public Knight(int positionX, int positionY, boolean isBlack) {
        super(positionX, positionY, isBlack);
    }

    @Override
    public int getImage() {
        return isBlack() ? R.drawable.chesspiece_knight_black_default
                : R.drawable.chesspiece_knight_white_default;
    }

    @NonNull
    @Override
    public List<Point> getAvailableMoves(Point currentPosition) {
        List<Point> list = new ArrayList<>(8);

        int[][] moves = new int[][]{
                {-2, +1}, {-2, -1},
                {+2, +1}, {+2, -1},
                {+1, -2}, {-1, -2},
                {+1, +2}, {-1, +2}
        };

        for (int[] move : moves)
            validateAndAddMove(list, currentPosition.x + move[0], currentPosition.y + move[1]);

        return list;
    }

    @Override
    public String getSymbol() {
        return "N";
    }
}
