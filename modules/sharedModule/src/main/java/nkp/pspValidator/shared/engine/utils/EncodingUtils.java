package nkp.pspValidator.shared.engine.utils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class EncodingUtils {
    /**
     * Detekuje kódování souboru kombinací různých metod.
     * - Pokud soubor obsahuje UTF-8 BOM, vrací "UTF-8".
     * - Pokud lze soubor dekódovat jako UTF-8 beze ztrát, vrací "UTF-8".
     * - Pokud ICU4J detekuje UTF-8 jako pravděpodobné kódování, vrací "UTF-8".
     * - Jinak vrací nejpravděpodobnější jiné kódování.
     *
     * @param file Soubor, jehož kódování se má detekovat.
     * @return Detekované kódování souboru.
     * @throws IOException Pokud dojde k chybě při čtení souboru.
     */
    public static String detectEncoding(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());

        // 1. Zkontrolujeme, jestli soubor obsahuje UTF-8 BOM
        if (hasUtf8BOM(data)) {
            return "UTF-8";
        }

        // 2. Zkusíme dekódovat soubor jako UTF-8 a ověřit, zda to projde beze ztrát
        if (isValidUtf8(data)) {
            return "UTF-8";
        }

        // 3. Použijeme ICU4J k detekci nejpravděpodobnějšího kódování
        CharsetDetector detector = new CharsetDetector();
        detector.setText(data);
        CharsetMatch match = detector.detect();
        String detectedEncoding = match.getName();

        // 4. Pokud ICU4J detekuje UTF-8, byť s pár divnými znaky, protože isValidUtf8() selhalo, vracíme "UTF-8"
        List<String> utf8Variants = Arrays.asList("UTF-8", "utf8", "utf-8");
        if (utf8Variants.contains(detectedEncoding.toLowerCase())) {
            return "UTF-8";
        }

        // 5. Jinak vracíme nejpravděpodobnější detekované kódování
        return detectedEncoding;
    }

    /**
     * Kontroluje, zda soubor obsahuje UTF-8 BOM (Byte Order Mark)
     */
    private static boolean hasUtf8BOM(byte[] data) {
        return data.length >= 3 &&
                (data[0] & 0xFF) == 0xEF &&
                (data[1] & 0xFF) == 0xBB &&
                (data[2] & 0xFF) == 0xBF;
    }

    /**
     * Kontroluje, zda lze soubor správně dekódovat jako UTF-8
     */
    private static boolean isValidUtf8(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.reset();
        try {
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
