package io.github.jason07289.svn.mcp.svn.svnkit;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

final class SvnKitContentSupport {

    private SvnKitContentSupport() {}

    static boolean isProbablyText(String mime, byte[] bytes) {
        if (mime != null) {
            String m = mime.toLowerCase();
            if (m.startsWith("text/")
                    || m.contains("xml")
                    || m.contains("json")
                    || m.contains("javascript")) {
                return true;
            }
            if (m.startsWith("image/")
                    || m.startsWith("video/")
                    || m.startsWith("audio/")
                    || m.startsWith("application/octet-stream")) {
                return false;
            }
        }
        return isValidUtf8(bytes);
    }

    static String decodeUtf8Lenient(byte[] bytes) {
        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static boolean isValidUtf8(byte[] bytes) {
        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
