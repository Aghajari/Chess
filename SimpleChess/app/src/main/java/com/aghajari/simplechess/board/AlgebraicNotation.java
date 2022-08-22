package com.aghajari.simplechess.board;

import android.graphics.Point;

import com.aghajari.simplechess.pieces.ChessPiece;

// https://en.wikipedia.org/wiki/Algebraic_notation_(chess)
public class AlgebraicNotation {

    public static String getPositionName(Point position) {
        return ((char) ('a' + position.x)) + "" + (8 - position.y);
    }

    public static String read(ChessPiece piece, Point from, Point to, boolean capture,
                              int checkCount, boolean mate, ChessPiece promotion) {
        return piece.getSymbol() +
                getPositionName(from) +
                (capture ? " × " : " - ") +
                getPositionName(to) +
                (promotion == null ? "" : promotion.getSymbol()) +
                (checkCount == 1 ? "+" : (checkCount == 2 ? "++" : "")) +
                (mate ? "#" : "");
    }

    public static String readCastling(boolean kingSide) {
        return kingSide ? "0-0" : "0-0-0";
    }


    protected static class MoveData {

        private final ChessPiece piece;
        private final Point from, to;

        private ChessPiece promotion;
        private boolean capture, mate;
        private int checkCount;

        private int castling = -1;

        public MoveData(ChessPiece piece, int endX, int endY) {
            this.piece = piece;
            this.from = new Point(piece.getRealPosition());
            this.to = new Point(endX, endY);
        }

        public ChessPiece getPiece() {
            return piece;
        }

        public void setPromotion(ChessPiece promotion) {
            this.promotion = promotion;
        }

        public void setCapture(boolean capture) {
            this.capture = capture;
        }

        public boolean isCapture() {
            return capture;
        }

        public void setMate(boolean mate) {
            this.mate = mate;
        }

        public void setCheckCount(int checkCount) {
            this.checkCount = checkCount;
        }

        public void setCastling(boolean kingSide) {
            castling = kingSide ? 1 : 2;
        }

        public String read() {
            if (castling >= 1)
                return readCastling(castling == 1);

            return AlgebraicNotation.read(piece,
                    from,
                    to,
                    capture,
                    checkCount,
                    mate,
                    promotion
            );
        }
    }
}
