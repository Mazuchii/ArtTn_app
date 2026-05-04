package tn.esprit.museum.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QRCodeGenerator {

    public static Image generateQRCode(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            // Convertir BitMatrix en Image JavaFX
            int matrixWidth = bitMatrix.getWidth();
            int matrixHeight = bitMatrix.getHeight();

            WritableImage writableImage = new WritableImage(matrixWidth, matrixHeight);
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            for (int x = 0; x < matrixWidth; x++) {
                for (int y = 0; y < matrixHeight; y++) {
                    boolean isBlack = bitMatrix.get(x, y);
                    if (isBlack) {
                        pixelWriter.setColor(x, y, javafx.scene.paint.Color.BLACK);
                    } else {
                        pixelWriter.setColor(x, y, javafx.scene.paint.Color.WHITE);
                    }
                }
            }

            return writableImage;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
