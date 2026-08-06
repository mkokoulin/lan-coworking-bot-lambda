package com.lan.app.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public final class QrCodeGenerator {

    private QrCodeGenerator() {}

    public static byte[] png(String content, int sizePx) throws Exception {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Builds the standard "WIFI:" payload (§ZXing/RFC draft) that phone camera apps
     * recognize as a network to join in one tap, e.g. WIFI:T:WPA;S:MySSID;P:MyPass;;
     */
    public static String wifiPayload(String ssid, String password) {
        String type = password == null || password.isBlank() ? "nopass" : "WPA";
        StringBuilder sb = new StringBuilder("WIFI:T:").append(type).append(";S:").append(escape(ssid)).append(";");
        if (!"nopass".equals(type)) {
            sb.append("P:").append(escape(password)).append(";");
        }
        return sb.append(";").toString();
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:")
                .replace("\"", "\\\"");
    }
}
