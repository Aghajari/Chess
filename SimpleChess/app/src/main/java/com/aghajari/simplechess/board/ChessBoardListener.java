package com.aghajari.simplechess.board;

import com.aghajari.simplechess.pieces.ChessPiece;

public interface ChessBoardListener {

    enum ChessResult {
        BLACK_WON,
        WHITE_WON,
        DRAW_STALEMATE,
        DRAW_DEAD_POSITION,
        DRAW_THREEFOLD_REPETITION,
        DRAW_50_MOVE_RULE,
    }

    void onTurnChanged(boolean isBlackTurn);

    void onMove(ChessPiece piece, int newX, int newY);

    void onCapture(ChessPiece piece);

    void onDone(ChessResult result);

    void onAfterMove(String move);

}
