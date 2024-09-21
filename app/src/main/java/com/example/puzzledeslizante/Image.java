package com.example.puzzledeslizante;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class Image {

    private Bitmap originalImage;
    private List<Bitmap> puzzlePieces;

    public Image(Bitmap image) {
        this.originalImage = image;
        this.puzzlePieces = new ArrayList<>();
        divideImage();
    }

    private void divideImage() {
        int rows = 3;
        int cols = 3;
        int pieceWidth = originalImage.getWidth() / cols;
        int pieceHeight = originalImage.getHeight() / rows;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * pieceWidth;
                int y = row * pieceHeight;
                Bitmap piece = Bitmap.createBitmap(originalImage, x, y, pieceWidth, pieceHeight);
                puzzlePieces.add(piece);
            }
        }
    }

    public List<Bitmap> getPuzzlePieces() {
        return puzzlePieces;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
    }

    public static Bitmap mergeBitmaps(List<Bitmap> bitmaps, int cols, int rows) {
        if (bitmaps.isEmpty()) return null;

        int pieceWidth = bitmaps.get(0).getWidth();
        int pieceHeight = bitmaps.get(0).getHeight();

        Bitmap mergedBitmap = Bitmap.createBitmap(pieceWidth * cols, pieceHeight * rows, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedBitmap);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Bitmap piece = bitmaps.get(row * cols + col);
                canvas.drawBitmap(piece, col * pieceWidth, row * pieceHeight, null);
            }
        }

        return mergedBitmap;
    }

    public static Bitmap loadImage(String path) {
        return BitmapFactory.decodeFile(path);
    }
}
