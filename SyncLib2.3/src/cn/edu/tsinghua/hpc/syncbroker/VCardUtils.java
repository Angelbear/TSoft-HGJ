package cn.edu.tsinghua.hpc.syncbroker;

public final class VCardUtils {
    final public static int VERSION_VCARD21_INT = 1;

    final public static int VERSION_VCARD30_INT = 2;

    final public static String foldingString(String str, int version) {
        if (str.endsWith("\r\n")) {
            str = str.substring(0, str.length() - 2);
        } else if (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        } else {
            return null;
        }

        str = str.replaceAll("\r\n", "\n");
        if (version == VERSION_VCARD21_INT) {
            return str.replaceAll("\n", "\r\n ");
        } else if (version == VERSION_VCARD30_INT) {
            return str.replaceAll("\n", "\n ");
        } else {
            return null;
        }
    }
}
