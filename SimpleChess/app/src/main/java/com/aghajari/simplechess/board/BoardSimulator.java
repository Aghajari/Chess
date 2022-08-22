package com.aghajari.simplechess.board;

import com.aghajari.simplechess.pieces.ChessPiece;
import com.aghajari.simplechess.pieces.King;
import com.aghajari.simplechess.pieces.Pawn;

import java.util.LinkedList;
import java.util.List;

public class BoardSimulator implements Board {

    private static BoardSimulator instance;

    public static BoardSimulator getInstance(ChessPiece piece, ChessPiece replacement,
                                             int newX, int newY) {
        if (instance == null)
            instance = new BoardSimulator();

        instance.init(piece, replacement, newX, newY);
        return instance;
    }

    private int newX, newY;
    private ChessPiece piece, replacement;

    private BoardSimulator() {
    }

    private void init(ChessPiece piece, ChessPiece replacement, int newX, int newY) {
        this.piece = piece;
        this.replacement = replacement;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public boolean isEmpty(int x, int y) {
        if (x == newX && y == newY)
            return false;

        if (x == piece.getRealPosition().x && y == piece.getRealPosition().y)
            return true;

        // En Passant
        if ((replacement instanceof Pawn) &&
                replacement.getRealPosition().x == x &&
                replacement.getRealPosition().y == y)
            return true;

        return piece.getBoard().isEmpty(x, y);
    }

    @Override
    public ChessPiece getPiece(int x, int y) {
        if (isUndefined(x, y))
            return ChessPiece.UNDEFINED;

        if (isEmpty(x, y))
            return null;

        return x == newX && y == newY ? piece : piece.getBoard().getPiece(x, y);
    }

    @Override
    public King getKing(boolean black) {
        return piece.getBoard().getKing(black);
    }

    @Override
    public List<ChessPiece> getPieces(boolean black) {
        if (replacement == null || replacement == ChessPiece.UNDEFINED || piece.isBlack() == black)
            return piece.getBoard().getPieces(black);

        List<ChessPiece> p1 = new LinkedList<>(), p0 = piece.getBoard().getPieces(black);
        for (ChessPiece piece : p0)
            if (piece != replacement)
                p1.add(piece);

        return p1;
    }
}
