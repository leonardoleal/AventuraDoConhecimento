package br.com.lealweb.aventuradoconhecimento.jogopreenchernumeros;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import static br.com.lealweb.aventuradoconhecimento.jogopreenchernumeros.GameView.Dificulty.*;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private enum Actions {
        ROLL_DICE, NEW_GAME, CHOSSE_NUMBER;
    }

    public enum Dificulty {
        ONE_DICE, TWO_DICES, THREE_DICES;
    }

    public static int SCREEN_WIDTH_RATIO = 800;
    public static int SCREEN_HEIGHT_RATIO = 480;
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;


    private static final long BLINK_DURATION = 350;
    private MainThread thread;
    private GameActivity gameActivity;
    private boolean needShowDialog = true;

    private Background bg;
    private DificultyIcon dificultyIcon;
    private ExitIcon exitIcon;
    private Paint paintText;
    private Player player1, player2;
    private List<Dice> dices;
    private Actions action;
    private static Dificulty dificulty;

    private Player playerTurn;
    private int blinkColor;
    private long blinkStart;
    private long lastUpdateTime;

    private boolean blink;

    public GameView(Context context) {
        super(context);

        gameActivity = (GameActivity) context;
        setupMetrics();

        getHolder().addCallback(this);
        setFocusable(true);

        SoundManager.initSounds(context);
        thread = new MainThread(getHolder(), this);
    }

    private void setupMetrics() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        gameActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        SCREEN_WIDTH = displaymetrics.widthPixels;
        SCREEN_HEIGHT = displaymetrics.heightPixels;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while(retry) {
            try {
                thread.setRunning(false);
                thread.join();

            } catch(InterruptedException e) { e.printStackTrace(); }
            retry = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        paintText = new Paint();
        paintText.setTextSize(18 * SCREEN_WIDTH / SCREEN_WIDTH_RATIO);
        paintText.setFakeBoldText(true);

        blinkColor = Color.BLUE;

        bg = new Background(getContext());
        dificultyIcon = new DificultyIcon(getContext());
        exitIcon = new ExitIcon(getContext());

        dificulty = TWO_DICES;
        setNewGame();

        thread.setRunning(true);
        thread.start();
    }

    private void setNewGame() {
        dices = new ArrayList<>();
        int centerScreen = SCREEN_WIDTH/2;
        int[] pos = {centerScreen - getWidthProportion(120), centerScreen + getWidthProportion(120) ,centerScreen};
        for (int i = 0; i <= dificulty.ordinal(); i++) {
            dices.add(new Dice(getContext(), pos[i], 5));
        }

        player1 = new Player(getContext(), BitmapFactory.decodeResource(getResources(), R.drawable.jpn_player1_elephant)
                , 40, 90, Color.YELLOW, "Jogador 1"
        );
        player2 = new Player(getContext(), BitmapFactory.decodeResource(getResources(), R.drawable.jpn_player2_elephant)
                , SCREEN_WIDTH_RATIO/2+40, 90, Color.GREEN, "Jogador 2"
        );

        playerTurn = player1;
        playerTurn.setPlaying(true);
        action = Actions.ROLL_DICE;
    }

    private void restartGame() {
        setNewGame();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (action.equals(Actions.NEW_GAME)) {
                    new ConfirmDialog(getContext(), dialogNewGameListener)
                            .show(getResources().getString(R.string.new_game_dialog));
                }

                return true;

            case MotionEvent.ACTION_UP:

                if (exitIcon.isTouched(event)) {
                    new ConfirmDialog(getContext(), dialogExitListener)
                            .show(getResources().getString(R.string.exit_dialog));
                    return true;
                }

                if (dificultyIcon.isTouched(event)) {
                    if (needShowDialog) {
                        new ConfirmDialog(getContext(), dialogChangeDificultyListener)
                                .show(getResources().getString(R.string.change_dificulty_dialog));
                    } else {
                        changeDificulty();
                    }
                    return true;
                }
                needShowDialog = true;

                if (action.equals(Actions.ROLL_DICE)) {
                    if (dices.get(0).isRolling()) {
                        for (Dice dice: dices) {
                            dice.stopRolling();
                        }
                        action = Actions.CHOSSE_NUMBER;
                    } else {
                        for (Dice dice: dices) {
                            dice.startRolling();
                        }
                    }
                } else if (action.equals(Actions.CHOSSE_NUMBER)) {
                    int result = 0;
                    for (Dice dice : dices) {
                        result += dice.getResult();
                    }

                    if (! playerTurn.existNumber(result)) endTurn();

                    else if (playerTurn.isTouchedNumber(result, event)) {
                        if (playerTurn.isWinner()) {
                            action = Actions.NEW_GAME;
                            return true;
                        } else {
                            endTurn();
                        }
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    DialogInterface.OnClickListener dialogChangeDificultyListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    changeDificulty();
                    needShowDialog = false;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    needShowDialog = true;
                    break;
            }
        }
    };

    DialogInterface.OnClickListener dialogNewGameListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    restartGame();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    DialogInterface.OnClickListener dialogExitListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    gameActivity.finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    private void changeDificulty() {
        switch (getDificulty()) {
            case ONE_DICE:
                dificulty = TWO_DICES;
                break;

            case TWO_DICES:
                dificulty = THREE_DICES;
                break;

            case THREE_DICES:
                dificulty = ONE_DICE;
                break;
        }
        restartGame();
    }

    private void endTurn() {
        if (player1.isPlaying()) {
            player1.setPlaying(false);
            playerTurn = player2;
        } else {
            player2.setPlaying(false);
            playerTurn = player1;
        }

        playerTurn.setPlaying(true);
        action = Actions.ROLL_DICE;
    }

    static public Dificulty getDificulty() {
        return dificulty;
    }

    public void update() {
        if (dices.get(0).isRolling()) {
            for (Dice dice : dices) {
                dice.update();
            }
        }

        if (System.currentTimeMillis() - lastUpdateTime >= BLINK_DURATION && !blink) {
            blink = true;
            blinkStart = System.currentTimeMillis();
        }
        
        if (System.currentTimeMillis() - blinkStart >= 800 && blink) {
            blink = false;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        bg.draw(canvas);

        player1.draw(canvas);
        player2.draw(canvas);

        drawPlayerTurnMessage(canvas);

        if (playerTurn.isWinner()) {
            drawWinnerMessage(canvas);
        }

        for (Dice dice : dices) {
            dice.draw(canvas);
        }

        dificultyIcon.draw(canvas);
        exitIcon.draw(canvas);
    }

    private void drawWinnerMessage(Canvas canvas) {
        if (blink) {
            blinkColor = (blinkColor == Color.BLUE) ? Color.RED :
                    ((blinkColor == Color.YELLOW) ? Color.BLUE : Color.YELLOW);
            paintText.setColor(blinkColor);
        }
        canvas.drawText(playerTurn.getName() +" GANHOU!!!", SCREEN_WIDTH / 2 - 110, SCREEN_HEIGHT / 2, paintText);
    }

    private void drawPlayerTurnMessage(Canvas canvas) {
        paintText.setColor(Color.RED);
        canvas.drawText("É a vez do ", getWidthProportion(20), getHeightProportion(20), paintText);
        paintText.setColor(playerTurn.getColor());
        canvas.drawText(playerTurn.getName(), getWidthProportion(110), getHeightProportion(20), paintText);
    }


    public static int getWidthProportion(int value) {
        return value * GameView.SCREEN_WIDTH / GameView.SCREEN_WIDTH_RATIO;
    }

    public static int getHeightProportion(int value) {
        return value * GameView.SCREEN_HEIGHT / GameView.SCREEN_HEIGHT_RATIO;
    }
}