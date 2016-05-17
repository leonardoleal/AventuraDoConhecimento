package com.aventuradoconhecimento.jogopreenchernumeros;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.util.Random;

public class Dice extends GameObject {

    private Bitmap spritesheet;
    private int result, frames;
    private Animation animation = new Animation();
    private boolean rolling;
    private Context context;
    private SoundList sound;

    public Dice(Context c, int xAxys, int yAxis) {
        sound = new SoundList(c);
        context = c;
        x = xAxys;
        y = yAxis;

        setImage();
    }

    public void startRolling() {
        rolling = true;
        resetDice();
        sound.play();
    }

    public void stopRolling() {
        rolling = false;
        raffleResult();
        sound.stop();
        sound.play();
    }

    public boolean isRolling() {
        return rolling;
    }

    private void raffleResult() {
        Random rand = new Random();
        result = rand.nextInt(6) + 1;
        setImage();
    }

    public void update() {
        animation.update();
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(animation.getImage(), x, y, null);
    }

    public int getResult() {
        return result;
    }

    public void resetDice() {
        result = 0;
        setImage();
    }

    public void config() {
        height = spritesheet.getHeight();
        width = spritesheet.getWidth()/frames;

        Bitmap[] image = new Bitmap[frames];//16s

        for (int i = 0; i < image.length; i++) {
            image[i] = Bitmap.createBitmap(spritesheet, i * width, 0, width, height);
        }

        animation.setFrames(image);
    }

    private void setImage() {
        frames = 1;
        sound.setSoundResource(R.raw.dice_throw);

        switch (result) {
            case 1:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_1);
                break;

            case 2:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_2);
                break;

            case 3:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_3);
                break;

            case 4:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_4);
                break;

            case 5:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_5);
                break;

            case 6:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice_6);
                break;

            default:
                spritesheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jpn_dice);
                sound.setSoundResource(R.raw.dice_shake, true);
                frames = 16;
        }

        config();
    }
}