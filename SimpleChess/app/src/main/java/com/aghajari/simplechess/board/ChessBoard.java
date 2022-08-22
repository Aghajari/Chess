package com.aghajari.simplechess.board;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

import com.aghajari.axanimation.AXAnimation;
import com.aghajari.simplechess.R;
import com.aghajari.simplechess.pieces.Bishop;
import com.aghajari.simplechess.pieces.ChessPiece;
import com.aghajari.simplechess.pieces.King;
import com.aghajari.simplechess.pieces.Knight;
import com.aghajari.simplechess.pieces.Pawn;
import com.aghajari.simplechess.pieces.Queen;
import com.aghajari.simplechess.pieces.Rook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChessBoard extends ViewGroup implements Board, ChessBoardListener {

    static final long DURATION = 150,
            MOVE_DURATION = 300,
            CAPTURE_DURATION = 350,
            PAWN_REPLACER_DURATION = 400;

    static final int SELECTION_COLOR = Color.GREEN;
    static final int ERROR_COLOR = 0xFFE53935;

    private final View[][] boardViews = new View[GRID][GRID];

    private final Paint paint = new Paint();
    private final Paint paint2 = new Paint();

    private List<Point> availableMoves = null;
    private final Point selectedPosition = new Point(-1, -1);

    private State state = State.DEFAULT;
    private ValueAnimator animator;

    private final List<ChessPiece> blackPieces = new LinkedList<>();
    private final List<ChessPiece> whitePieces = new LinkedList<>();

    private final List<View> blackCapturedPieces = new LinkedList<>();
    private final List<View> whiteCapturedPieces = new LinkedList<>();

    private boolean isBlackTurn = false;
    private ChessBoardListener listener;

    private final View checkNotifyView;

    private final List<String> moves = new ArrayList<>();
    private final Map<String, Integer> rule_threefold = new HashMap<>();
    private int rule_50 = 0;

    private enum State {
        DEFAULT,
        SELECTED,
        PAWN_PROMOTION,
        FINISHED
    }

    public ChessBoard(@NonNull Context context) {
        this(context, null);
    }

    public ChessBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChessBoard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint.setColor(Color.argb(100, 255, 255, 255));
        paint.setStyle(Paint.Style.FILL);

        paint2.setAlpha(0);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(dp(2));

        init();

        checkNotifyView = new View(context);
        checkNotifyView.setVisibility(GONE);
        GradientDrawable checkDrawable = new GradientDrawable();
        checkDrawable.setStroke(dp(2), ERROR_COLOR);
        checkNotifyView.setTranslationX(dp(0.5f));
        checkNotifyView.setBackground(checkDrawable);

        addView(checkNotifyView, new ChessLayoutParams(null));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int padding = dp(2);
        int size = (right - left) / GRID;
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            ChessLayoutParams lp = (ChessLayoutParams) child.getLayoutParams();
            ChessPiece piece = lp.getPiece();

            if (piece == null || child.getVisibility() == GONE)
                continue;

            int l = piece.getPosition().x * size;
            int t = piece.getPosition().y * size + (child == checkNotifyView ? 0 : padding);

            if (child instanceof PawnPromotionView) {
                t = (piece.isBlack() ? 6 : 1) * size;
                l += size / 2;

                int w = 4 * size;

                int l2 = Math.max(0, l - w / 2);
                if (l2 + w > GRID * size)
                    l2 = GRID * size - w;

                ((PawnPromotionView) child).setArrowX(l - l2);
                child.layout(l2, t, l2 + w, t + size + dp(8));
            } else {
                child.layout(l, t, l + size, t + size);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (state == State.FINISHED)
            return false;

        if (state == State.PAWN_PROMOTION)
            return super.dispatchTouchEvent(event);

        long duration = event.getEventTime() - event.getDownTime();
        if (event.getActionMasked() == MotionEvent.ACTION_UP && duration < 250)
            click(event);

        return true;
    }

    private void click(MotionEvent e) {
        float size = getMeasuredWidth() * 1.0f / GRID;
        int y = (int) Math.floor(e.getY() / size);

        if (y < GRID) {
            int x = (int) Math.floor(e.getX() / size);
            ChessPiece current = getPiece(x, y);

            if (current == ChessPiece.UNDEFINED)
                return;

            if (selectedPosition.equals(x, y)) {
                state = State.DEFAULT;
                stopSelection();
            } else if (current != null && current.isBlack() == isBlackTurn) {
                selectedPosition.set(x, y);
                availableMoves = getPiece(x, y).getAvailableMoves();

                state = State.SELECTED;
                startSelection();
            } else if (state == State.SELECTED
                    && availableMoves.contains(new Point(x, y))) {
                state = State.DEFAULT;
                stopSelection();
                move(getPiece(selectedPosition.x, selectedPosition.y), x, y);
            } else {
                selectedPosition.set(x, y);
                availableMoves = Collections.emptyList();
                state = State.DEFAULT;
                startSelection();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float size = getMeasuredWidth() * 1.0f / GRID;
        float sw = paint2.getStrokeWidth() / 2f;
        for (int j = 0; j < GRID; j++) {
            for (int i = (j % 2 == 0 ? 0 : 1); i < GRID; i += 2) {
                float left = i * size;
                float top = j * size;
                canvas.drawRect(left, top, left + size, top + size, paint);
            }
        }

        if (selectedPosition.x >= 0 && selectedPosition.y >= 0) {
            if (checkNotifyView.getVisibility() == GONE ||
                    !((ChessLayoutParams) checkNotifyView.getLayoutParams())
                            .getPiece().getRealPosition().equals(selectedPosition)) {
                float left = selectedPosition.x * size + sw;
                float top = selectedPosition.y * size + sw;
                canvas.drawRect(left, top, left + size - sw * 2, top + size - sw * 2, paint2);
            }
        }

        if (availableMoves != null) {
            for (Point p : availableMoves) {
                float left = p.x * size + sw;
                float top = p.y * size + sw;
                canvas.drawRect(left, top, left + size - sw * 2, top + size - sw * 2, paint2);
            }
        }

        super.dispatchDraw(canvas);
    }

    private void init() {
        /*/ Simulate En Passant
        if (true) {
            attach(new King(GRID >> 1, 0, true));
            attach(new King(1, GRID - 1, false));

            attach(new Rook(1, 0, true));

            attach(new Pawn(2, 1, true));
            attach(new Pawn(3, 3, false));
            attach(new Pawn(1, 3, false));
            return;
        }
        //*/

        /*/ Simulate Castling
        if (true) {
            attach(new King(GRID >> 1, 0, true));
            attach(new King(GRID >> 1, GRID - 1, false));

            attach(new Rook(0, 0, true));
            attach(new Rook(0, GRID - 1, false));
            attach(new Rook(GRID - 1, 0, true));
            attach(new Rook(GRID - 1, GRID - 1, false));
            return;
        }
        //*/

        /*/ Simulate Pawn Promotion
        if (true) {
            attach(new King(4, 0, true));
            attach(new King(3, GRID - 1, false));

            attach(new Pawn(5, GRID - 2, true));
            attach(new Pawn(2, 1, false));
            return;
        }
        //*/

        /*/ Simulate Double Check
        if (true) {
            attach(new King(4, 0, true));
            attach(new King(3, GRID - 1, false));

            attach(new Queen((GRID >> 1), GRID - 1, false));
            attach(new Bishop((GRID >> 1), GRID - 2, false));
            return;
        }
        //*/

        attach(new King(GRID >> 1, 0, true));
        attach(new King(GRID >> 1, GRID - 1, false));

        attach(new Queen((GRID >> 1) - 1, 0, true));
        attach(new Queen((GRID >> 1) - 1, GRID - 1, false));

        attach(new Bishop(2, 0, true));
        attach(new Bishop(2, GRID - 1, false));
        attach(new Bishop(GRID - 3, 0, true));
        attach(new Bishop(GRID - 3, GRID - 1, false));

        attach(new Knight(1, 0, true));
        attach(new Knight(1, GRID - 1, false));
        attach(new Knight(GRID - 2, 0, true));
        attach(new Knight(GRID - 2, GRID - 1, false));

        attach(new Rook(0, 0, true));
        attach(new Rook(0, GRID - 1, false));
        attach(new Rook(GRID - 1, 0, true));
        attach(new Rook(GRID - 1, GRID - 1, false));

        for (int i = 0; i < GRID; i++) {
            attach(new Pawn(i, 1, true));
            attach(new Pawn(i, GRID - 2, false));
        }
    }

    private void attach(ChessPiece piece) {
        piece.setBoard(this);
        if (piece.isBlack())
            blackPieces.add(piece);
        else
            whitePieces.add(piece);

        AppCompatImageView img = new AppCompatImageView(getContext());
        img.setImageResource(piece.getImage());

        addView(img, new ChessLayoutParams(piece));
        boardViews[piece.getPosition().x][piece.getPosition().y] = img;
    }

    public List<String> getMoves() {
        return moves;
    }

    public boolean isBlackTurn() {
        return isBlackTurn;
    }

    private void stopSelection() {
        if (animator != null)
            animator.cancel();

        animator = ObjectAnimator.ofInt(paint2, "alpha", paint2.getAlpha(), 0);
        animator.setDuration(DURATION);
        animator.addUpdateListener(a -> invalidate());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                selectedPosition.set(-1, -1);
                availableMoves = null;
                invalidate();
            }
        });
        animator.start();
    }

    private void startSelection() {
        if (animator != null)
            animator.cancel();

        animator = ObjectAnimator.ofInt(paint2, "alpha", 0, 255);
        animator.addUpdateListener(a -> invalidate());
        animator.setDuration(DURATION);

        if (availableMoves == null || availableMoves.isEmpty()) {
            state = State.DEFAULT;
            paint2.setColor(ERROR_COLOR);

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    stopSelection();
                }
            });
        } else {
            paint2.setColor(SELECTION_COLOR);
        }
        animator.start();
    }

    private void move(ChessPiece piece, int endX, int endY) {
        AlgebraicNotation.MoveData moveData =
                new AlgebraicNotation.MoveData(
                        piece,
                        endX,
                        endY
                );

        ChessPiece replace = getPiece(endX, endY);
        if (replace != null && replace != ChessPiece.UNDEFINED) {
            onCapture(replace);
            moveData.setCapture(true);

        } else if (piece instanceof Pawn && piece.getRealPosition().x != endX) {
            //En Passant
            replace = getPiece(endX, endY + (piece.isBlack() ? -1 : 1));

            if (replace instanceof Pawn && ((Pawn) replace).isEnPassant()) {
                onCapture(replace);
                moveData.setCapture(true);
            }
        } else if (piece instanceof King && Math.abs(piece.getRealPosition().x - endX) == 2) {
            // Castling
            moveData.setCastling(endX > piece.getRealPosition().x);

            if (endX > piece.getRealPosition().x)
                moveNow(getPiece(GRID - 1, endY), endX - 1, endY, null);
            else
                moveNow(getPiece(0, endY), endX + 1, endY, null);
        }

        moveNow(piece, endX, endY, moveData);
        onMove(piece, endX, endY);
    }

    private void moveNow(ChessPiece piece, int endX, int endY, AlgebraicNotation.MoveData moveData) {
        float size = getMeasuredWidth() * 1.0f / GRID;
        Point start = piece.getRealPosition();

        View v = boardViews[start.x][start.y];
        boardViews[start.x][start.y] = null;
        boardViews[endX][endY] = v;

        boolean pawnPromotion = piece instanceof Pawn && (endY == 0 || endY == GRID - 1);
        AXAnimation.create()
                .duration(MOVE_DURATION/*
                    * (Math.abs(start.y - endY)
                        + Math.abs(start.x - endX))*/)
                .firstValueFromView(false)
                .translationX(0f, (endX * size) - (start.x * size))
                .translationY(0f, (endY * size) - (start.y * size))
                .withEndAction(a -> {
                    v.setTranslationX(0f);
                    v.setTranslationY(0f);
                    piece.moveTo(endX, endY);
                    v.requestLayout();

                    if (moveData != null && !pawnPromotion)
                        afterMove(moveData);
                })
                .start(v);

        if (moveData != null && pawnPromotion)
            startPawnPromotion(piece, moveData);
    }

    private void startPawnPromotion(ChessPiece piece, AlgebraicNotation.MoveData move) {
        state = State.PAWN_PROMOTION;

        PawnPromotionView view = new PawnPromotionView(getContext(), piece, move);
        ChessLayoutParams lp = new ChessLayoutParams(piece);

        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        addView(view, lp);

        view.setEnabled(false);

        AXAnimation.create()
                .delay(MOVE_DURATION)
                .duration(PAWN_REPLACER_DURATION)
                .scale(1f)
                .alpha(1f)
                .withEndAction(a -> view.setEnabled(true))
                .start(view);
    }

    protected void afterMove(AlgebraicNotation.MoveData moveData) {
        onTurnChanged(!isBlackTurn);

        int checkCount = getUnderAttackCount(
                getKing(isBlackTurn).getRealPosition(),
                isBlackTurn, 2
        );

        ValueAnimator animator = (ValueAnimator) checkNotifyView.getTag();

        if (checkCount >= 1) {
            if (animator == null) {
                animator = ObjectAnimator.ofFloat(checkNotifyView, "alpha", 0f, 1f);
                animator.setDuration(MOVE_DURATION);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.start();
                checkNotifyView.setTag(animator);
            }

            ((ChessLayoutParams) checkNotifyView.getLayoutParams())
                    .setPiece(getKing(isBlackTurn));
            checkNotifyView.setVisibility(VISIBLE);
        } else {
            if (animator != null)
                animator.cancel();
            checkNotifyView.setTag(null);
            checkNotifyView.setVisibility(GONE);
        }

        boolean isMate = isMate(isBlackTurn);

        moveData.setCheckCount(checkCount);
        moveData.setMate(isMate);
        onAfterMove(moveData.read());

        if (moveData.isCapture() || moveData.getPiece() instanceof Pawn) {
            rule_50 = 0;
            rule_threefold.clear();

        } else if (!isBlackTurn) {
            rule_50++;

            String key = getBoardKey();
            Integer count = rule_threefold.get(key);
            rule_threefold.put(key, count == null ? 1 : count + 1);

            // Threefold Repetition
            if (count != null && count == 2) {
                onDone(ChessResult.DRAW_THREEFOLD_REPETITION);
                return;
            }

            // 50-Move Rule
            if (rule_50 == 50) {
                onDone(ChessResult.DRAW_50_MOVE_RULE);
                return;
            }
        }

        // Checkmate
        if (isMate && checkCount >= 1)
            onDone(isBlackTurn ? ChessResult.WHITE_WON : ChessResult.BLACK_WON);

            // Stalemate
        else if (isMate && checkCount == 0)
            onDone(ChessResult.DRAW_STALEMATE);

            // Insufficient Material
        else if (whitePieces.size() == 1 && blackPieces.size() == 1)
            // King Vs King
            onDone(ChessResult.DRAW_DEAD_POSITION);
        else if (whitePieces.size() + blackPieces.size() == 3) {
            // King & (Bishop | Knight) Vs King
            ChessPiece piece = (whitePieces.size() == 2
                    ? whitePieces.get(1) : blackPieces.get(1));
            if (piece instanceof Knight || piece instanceof Bishop)
                onDone(ChessResult.DRAW_DEAD_POSITION);
        } else if (whitePieces.size() == 2 && blackPieces.size() == 2) {
            if (whitePieces.get(1) instanceof Knight && blackPieces.get(1) instanceof Knight)
                // King & Knight Vs King & Knight
                onDone(ChessResult.DRAW_DEAD_POSITION);
            else if (whitePieces.get(1) instanceof Bishop && blackPieces.get(1) instanceof Bishop
                    && isColoredCell(whitePieces.get(1).getRealPosition()) ==
                    isColoredCell(blackPieces.get(1).getRealPosition()))
                // King & Bishop Vs King & Bishop of the same color as the opponent's bishop
                onDone(ChessResult.DRAW_DEAD_POSITION);
        }

    }

    private boolean isColoredCell(Point p) {
        return ((p.x + (p.y % 2 == 0 ? 0 : 1)) % 2) == 0;
    }

    @Override
    public boolean isEmpty(int x, int y) {
        if (isUndefined(x, y))
            return true;

        return boardViews[x][y] == null;
    }

    @Override
    public ChessPiece getPiece(int x, int y) {
        if (isUndefined(x, y))
            return ChessPiece.UNDEFINED;

        if (boardViews[x][y] == null)
            return null;

        return ((ChessLayoutParams) boardViews[x][y].getLayoutParams()).getPiece();
    }

    @Override
    public King getKing(boolean black) {
        return (King) (black ? blackPieces.get(0) : whitePieces.get(0));
    }

    @Override
    public List<ChessPiece> getPieces(boolean black) {
        return black ? blackPieces : whitePieces;
    }

    public void setListener(ChessBoardListener listener) {
        if (listener != this)
            this.listener = listener;
    }

    @Override
    public void onTurnChanged(boolean isBlackTurn) {
        this.isBlackTurn = isBlackTurn;

        if (listener != null)
            listener.onTurnChanged(isBlackTurn);
    }

    @Override
    public void onMove(ChessPiece piece, int newX, int newY) {
        for (ChessPiece p : blackPieces)
            p.nextMove();

        for (ChessPiece p : whitePieces)
            p.nextMove();

        if (listener != null)
            listener.onMove(piece, newX, newY);
    }

    @Override
    public void onCapture(ChessPiece piece) {
        float size = getMeasuredWidth() * 1.0f / Board.GRID;

        Point start = piece.getRealPosition();
        View v = boardViews[start.x][start.y];
        boardViews[start.x][start.y] = null;

        if (piece.isBlack()) {
            blackPieces.remove(piece);
            shiftCapturedPieces(blackCapturedPieces, size);
            blackCapturedPieces.add(0, v);
        } else {
            whitePieces.remove(piece);
            shiftCapturedPieces(whiteCapturedPieces, size);
            whiteCapturedPieces.add(0, v);
        }

        int endY = GRID + 1 + (piece.isBlack() ? 1 : 0);

        AXAnimation.create()
                .duration(CAPTURE_DURATION)
                .firstValueFromView(false)
                .translationX(0f, -(start.x * size))
                .translationY(0f, (endY * size) - (start.y * size))
                .start(v);

        if (listener != null)
            listener.onCapture(piece);
    }

    @Override
    public void onDone(ChessResult result) {
        state = State.FINISHED;

        if (listener != null)
            listener.onDone(result);
    }

    @Override
    public void onAfterMove(String move) {
        moves.add(move);

        if (listener != null)
            listener.onAfterMove(move);
    }

    private void shiftCapturedPieces(List<View> list, float size) {
        for (View v0 : list) {
            AXAnimation.create()
                    .duration(CAPTURE_DURATION)
                    .translationX(v0.getTranslationX() + size)
                    .start(v0);
        }
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value);
    }

    private static class ChessLayoutParams extends ViewGroup.LayoutParams {

        private ChessPiece piece;

        public ChessLayoutParams(ChessPiece piece) {
            super(-2, -2);
            this.piece = piece;
        }

        public ChessPiece getPiece() {
            return piece;
        }

        public void setPiece(ChessPiece piece) {
            this.piece = piece;
        }
    }

    private class PawnPromotionView extends ViewGroup {

        private final Drawable backgroundDrawable;
        private final Drawable arrowDrawable;

        private int arrowX;
        private final boolean isArrowUp;
        private final ChessPiece target;
        private final AlgebraicNotation.MoveData moveData;

        public PawnPromotionView(Context context, ChessPiece piece, AlgebraicNotation.MoveData move) {
            super(context);
            this.target = piece;
            this.moveData = move;
            this.isArrowUp = !piece.isBlack();

            backgroundDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.back, null);
            arrowDrawable = ResourcesCompat.getDrawable(getResources(), isArrowUp ? R.drawable.arrow_up : R.drawable.arrow, null);

            if (isArrowUp) {
                setDarkColor(backgroundDrawable);
                setDarkColor(arrowDrawable);
            }

            attach(new Queen(0, 0, piece.isBlack()));
            attach(new Bishop(0, 0, piece.isBlack()));
            attach(new Rook(0, 0, piece.isBlack()));
            attach(new Knight(0, 0, piece.isBlack()));
        }

        void setDarkColor(Drawable drawable) {
            if (drawable != null)
                drawable.setColorFilter(new PorterDuffColorFilter(
                        0xFF2F2F2F, PorterDuff.Mode.MULTIPLY));
        }

        public void setArrowX(int arrowX) {
            this.arrowX = arrowX;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            backgroundDrawable.setBounds(0, isArrowUp ? dp(6) : 0, getWidth(), getHeight() - dp(4) + (isArrowUp ? dp(6) : 0));
            backgroundDrawable.draw(canvas);

            arrowDrawable.setBounds(arrowX - dp(9), isArrowUp ? 0 : getHeight() - dp(8.5f), arrowX + dp(9), isArrowUp ? dp(8) : getHeight() - dp(0.5f));
            arrowDrawable.draw(canvas);

            super.dispatchDraw(canvas);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int size = (r - l) / 4;
            int count = getChildCount();
            int top = isArrowUp ? dp(6) : 0;

            for (int i = 0; i < count; i++) {
                int left = i * size;
                getChildAt(i).layout(left, top, left + size, top + size);
            }
        }

        private void attach(ChessPiece piece) {
            AppCompatImageView img = new AppCompatImageView(getContext());
            img.setImageResource(piece.getImage());
            img.setScaleX(0.75f);
            img.setScaleY(0.75f);
            addView(img, new ChessLayoutParams(piece));

            img.setOnClickListener(v -> {
                if (isEnabled()) {
                    setEnabled(false);
                    float size = ChessBoard.this.getMeasuredWidth() * 1.0f / GRID;

                    int[] pos1 = new int[2];
                    int[] pos2 = new int[2];
                    v.getLocationInWindow(pos1);
                    ChessBoard.this.getLocationInWindow(pos2);

                    removeView(v);
                    v.setTranslationX(pos1[0] - pos2[0] - (v.getWidth() * (1f - img.getScaleX()) / 2f));
                    v.setTranslationY(pos1[1] - pos2[1] - (v.getHeight() * (1f - img.getScaleY()) / 2f) - dp(2));

                    View v2 = boardViews[target.getRealPosition().x][target.getRealPosition().y];
                    ChessBoard.this.addView(v);

                    AXAnimation.create()
                            .duration(PAWN_REPLACER_DURATION)
                            .scale(1f)
                            .translationX(target.getRealPosition().x * size)
                            .translationY(target.getRealPosition().y * size)
                            .start(v);

                    AXAnimation.create()
                            .duration(PAWN_REPLACER_DURATION)
                            .alpha(0f)
                            .start(v2);

                    AXAnimation.create()
                            .duration(PAWN_REPLACER_DURATION + 10)
                            .scale(0f)
                            .alpha(0f)
                            .withEndAction(a -> {
                                state = State.DEFAULT;
                                piece.swap(target);
                                boardViews[piece.getRealPosition().x][piece.getRealPosition().y] = v;
                                if (target.isBlack())
                                    blackPieces.set(blackPieces.indexOf(target), piece);
                                else
                                    whitePieces.set(whitePieces.indexOf(target), piece);

                                v.setTranslationX(0f);
                                v.setTranslationY(0f);
                                ChessBoard.this.removeView(this);

                                int index = ChessBoard.this.indexOfChild(v2);
                                ChessBoard.this.removeViewAt(index);
                                ChessBoard.this.removeView(v);
                                ChessBoard.this.addView(v, index);

                                moveData.setPromotion(piece);
                                afterMove(moveData);
                            })
                            .start(this);
                }
            });
        }
    }
}
