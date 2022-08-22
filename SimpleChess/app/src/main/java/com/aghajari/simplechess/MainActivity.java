package com.aghajari.simplechess;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.WindowManager;

import com.aghajari.simplechess.board.ChessBoardListener;
import com.aghajari.simplechess.pieces.ChessPiece;
import com.aghajari.xmlbypass.XmlByPass;
import com.aghajari.xmlbypass.XmlLayout;

@XmlByPass(layouts = {@XmlLayout(layout = "*")})
public class MainActivity extends AppCompatActivity implements ChessBoardListener {

    private static final int[][] COLORS = {
            new int[]{0xFFFF9190, 0xFF5E72EB},
            new int[]{0xFF0AB28C, 0xFFAB13A6},
            new int[]{0xFFC5227A, 0xFF7F19CD},
    };

    private activity_main view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(view = new activity_main(this));
        setBackground(COLORS[colorIndex]);

        view.board.setListener(this);
    }

    // BACKGROUND GRADIENT

    int colorIndex = 0;

    private void setBackground(int[] colors) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColors(colors);
        gd.setOrientation(GradientDrawable.Orientation.TL_BR);
        view.setBackground(gd);
    }

    private boolean isAnimating = false;

    private final Runnable bgAnimation = () -> {
        int next = (colorIndex + 1 >= COLORS.length) ? 0 : colorIndex + 1;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(4000);
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int[] c = new int[2];

        isAnimating = true;

        animator.addUpdateListener(valueAnimator -> {
            for (int i = 0; i < 2; i++)
                c[i] = (int) evaluator.evaluate(
                        valueAnimator.getAnimatedFraction(),
                        COLORS[colorIndex][i], COLORS[next][i]
                );

            setBackground(c);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                colorIndex = next;
                isAnimating = false;
                animate();
            }
        });
        animator.start();
    };

    @Override
    protected void onResume() {
        super.onResume();
        animate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.removeCallbacks(bgAnimation);
    }

    private void animate() {
        view.removeCallbacks(bgAnimation);
        if (isAnimating)
            return;
        view.postDelayed(bgAnimation, 1000);
    }

    // END OF BACKGROUND

    @Override
    public void onTurnChanged(boolean isBlackTurn) {

    }

    @Override
    public void onMove(ChessPiece piece, int newX, int newY) {

    }

    @Override
    public void onCapture(ChessPiece piece) {

    }

    @Override
    public void onDone(ChessResult result) {
        switch (result) {
            case BLACK_WON:
                view.log.setText("Black Won!");
                break;
            case WHITE_WON:
                view.log.setText("White Won!");
                break;
            case DRAW_STALEMATE:
                view.log.setText("Draw (Stalemate)!");
                break;
            case DRAW_DEAD_POSITION:
                view.log.setText("Draw (Dead Position)!");
                break;
            case DRAW_THREEFOLD_REPETITION:
                view.log.setText("Draw (Threefold Repetition)!");
                break;
            case DRAW_50_MOVE_RULE:
                view.log.setText("Draw (50 Move Rule)!");
                break;
        }
    }

    @Override
    public void onAfterMove(String move) {
        String text = view.board.isBlackTurn() ? "Black to Move\n" : "White to Move\n";
        text += (view.board.getMoves().size() / 2 + (view.board.getMoves().size() % 2));
        text += ". ";

        if (view.board.getMoves().size() % 2 == 1) {
            text += move;
        } else {
            text += view.board.getMoves().get(view.board.getMoves().size() - 2) + " | " + move;
        }
        view.log.setText(text);
    }

}