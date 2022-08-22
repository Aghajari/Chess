package com.aghajari.simplechess.pieces;

import android.graphics.Point;

import androidx.annotation.NonNull;

import com.aghajari.simplechess.board.Board;
import com.aghajari.simplechess.R;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends ChessPiece {

    private boolean enPassant = false;

    public Pawn(int positionX, int positionY, boolean isBlack) {
        super(positionX, positionY, isBlack);
    }

    @Override
    public int getImage() {
        return isBlack() ? R.drawable.chesspiece_pawn_black_default
                : R.drawable.chesspiece_pawn_white_default;
    }

    @Override
    public void nextMove() {
        super.nextMove();
        enPassant = false;
    }

    @Override
    public String getSymbol() {
        return "";
    }

    @Override
    public String getKey() {
        return super.getKey() + (isEnPassant() ? "E" : "");
    }

    @Override
    public void moveTo(int x, int y) {
        enPassant = isFirstMove() &&
                y == (isBlack() ? 3 : Board.GRID - 4);

        super.moveTo(x, y);
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    @NonNull
    @Override
    public List<Point> getAvailableMoves(Point currentPosition) {
        List<Point> list = new ArrayList<>(4);

        int size;
        int nextY = currentPosition.y + (isBlack() ? 1 : -1);

        // Pawn only can capture a piece on adjacent diagonal square
        // validateAndAddMove() adds the move even if there is an enemy
        // on forward move, but only returns true if there wasn't any other piece.
        // So we should remove the last move if there was another piece there
        // cus Pawn can't capture in this way.
        if (!findOnlyAttackMoves()) {
            boolean res = validateAndAddMove(list, currentPosition.x, nextY);
            if (!res)
                list.clear();

            if (res && isFirstMove()) {
                size = list.size();

                res = validateAndAddMove(list, currentPosition.x, currentPosition.y + (isBlack() ? 2 : -2));
                if (!res && list.size() == size + 1)
                    list.remove(size);
            }
        }

        size = list.size();
        if (isEnemy(getBoard().getPiece(currentPosition.x + 1, nextY)))
            validateAndAddMove(list, currentPosition.x + 1, nextY);

        if (list.size() == size) {
            ChessPiece p = getBoard().getPiece(currentPosition.x + 1, currentPosition.y);
            if (p instanceof Pawn && ((Pawn) p).isEnPassant())
                validateAndAddMove(list, p, currentPosition.x + 1, nextY);
        }

        size = list.size();
        if (isEnemy(getBoard().getPiece(currentPosition.x - 1, nextY)))
            validateAndAddMove(list, currentPosition.x - 1, nextY);

        if (list.size() == size) {
            ChessPiece p = getBoard().getPiece(currentPosition.x - 1, currentPosition.y);
            if (p instanceof Pawn && ((Pawn) p).isEnPassant())
                validateAndAddMove(list, p, currentPosition.x - 1, nextY);
        }

        return list;
    }

    @Override
    public boolean isFirstMove() {
        return super.isFirstMove() && ((isBlack() && getRealPosition().y == 1)
                || (!isBlack() && getRealPosition().y == Board.GRID - 2));
    }
}
