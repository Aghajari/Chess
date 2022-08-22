package com.aghajari.simplechess.pieces;

import android.graphics.Point;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.aghajari.simplechess.board.Board;
import com.aghajari.simplechess.board.BoardSimulator;
import com.aghajari.simplechess.board.AlgebraicNotation;

import java.util.Collections;
import java.util.List;

public abstract class ChessPiece {

    public static final ChessPiece UNDEFINED = new ChessPiece(-1, -1, false) {
        @Override
        public int getImage() {
            return 0;
        }

        @NonNull
        @Override
        public List<Point> getAvailableMoves(Point currentPosition) {
            return Collections.emptyList();
        }

        @Override
        public String getKey() {
            return "undefined";
        }

        @Override
        public String getSymbol() {
            return "";
        }
    };

    private final Point position;

    // Used only for simulating next move
    private final Point tmpPosition = new Point(-1, -1);

    private final boolean isBlack;
    private Board board;
    private boolean checkMate = true;

    private boolean hasMoved = false;

    public ChessPiece(int positionX, int positionY, boolean isBlack) {
        this.position = new Point(positionX, positionY);
        this.isBlack = isBlack;
    }

    public void setCheckMate(boolean checkMate) {
        this.checkMate = checkMate;
    }

    public Point getPosition() {
        return tmpPosition.x != -1 ? tmpPosition : position;
    }

    public Point getRealPosition() {
        return position;
    }

    public boolean isBlack() {
        return isBlack;
    }

    @DrawableRes
    public abstract int getImage();

    @NonNull
    public List<Point> getAvailableMoves() {
        return getAvailableMoves(getPosition());
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public boolean isEnemy(ChessPiece piece) {
        return piece != UNDEFINED && piece != null && piece.isBlack() != isBlack();
    }

    public boolean isFirstMove() {
        return !hasMoved;
    }

    @NonNull
    public abstract List<Point> getAvailableMoves(Point currentPosition);

    // checkMate is false when BoardSimulator is simulating
    // mate status, So we should only find the capture moves on getAvailableMoves()
    // if we are simulating
    protected boolean findOnlyAttackMoves() {
        return !checkMate;
    }

    /**
     * Validates the move and adds it to the list.
     *
     * @return True if there wasn't any other piece on that place.
     */
    protected boolean validateAndAddMove(List<Point> list, int x, int y) {
        return validateAndAddMove(list, getBoard().getPiece(x, y), x, y);
    }

    protected boolean validateAndAddMove(List<Point> list, ChessPiece replace, int x, int y) {
        tmpPosition.set(x, y);

        if ((replace == null || isEnemy(replace))) {
            if (findOnlyAttackMoves() || !checkMateStatus(replace, x, y))
                list.add(new Point(x, y));
        }
        tmpPosition.set(-1, -1);
        return replace == null;
    }

    protected boolean checkMateStatus(ChessPiece replacement, int newX, int newY) {
        return BoardSimulator.getInstance(this, replacement, newX, newY)
                .isCheck(isBlack());
    }

    public void moveTo(int x, int y) {
        hasMoved = true;
        position.set(x, y);
    }

    public void nextMove() {
    }

    public void swap(ChessPiece piece) {
        hasMoved = piece.hasMoved;
        board = piece.board;
        checkMate = piece.checkMate;
        position.set(piece.position.x, piece.position.y);
        tmpPosition.set(-1, -1);
    }

    /**
     * Use this key to find Threefold Repetition
     *
     * @return a key for current position of the piece
     */
    public String getKey() {
        return getSymbol() + getPositionName();
    }

    public String getPositionName() {
        return AlgebraicNotation.getPositionName(position);
    }

    public abstract String getSymbol();

    @Override
    public String toString() {
        return getKey();
    }
}
