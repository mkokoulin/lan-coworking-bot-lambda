package com.lan.app.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeGeneratorTest {

    @Test
    void wifiPayloadUsesStandardWifiUriFormat() {
        assertEquals("WIFI:T:WPA;S:LAN Guest;P:s3cr3t;;", QrCodeGenerator.wifiPayload("LAN Guest", "s3cr3t"));
    }

    @Test
    void wifiPayloadEscapesReservedCharacters() {
        String payload = QrCodeGenerator.wifiPayload("a;b", "p:\"w,d");
        assertEquals("WIFI:T:WPA;S:a\\;b;P:p\\:\\\"w\\,d;;", payload);
    }

    @Test
    void wifiPayloadEscapesLiteralBackslash() {
        String payload = QrCodeGenerator.wifiPayload("net", "a\\b");
        assertEquals("WIFI:T:WPA;S:net;P:a\\\\b;;", payload);
    }

    @Test
    void wifiPayloadOmitsPasswordFieldWhenOpenNetwork() {
        assertEquals("WIFI:T:nopass;S:OpenNet;;", QrCodeGenerator.wifiPayload("OpenNet", ""));
    }

    @Test
    void pngEncodesScannableQrContainingThePayload() throws Exception {
        String payload = QrCodeGenerator.wifiPayload("LAN Guest", "s3cr3t");
        byte[] png = QrCodeGenerator.png(payload, 300);

        assertEquals(decode(png), payload);
    }

    private static String decode(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}
