package com.nexplm.platform.object.version;

/**
 * 版本号格式化：大版本（revision）按字母序列 A..Z, AA..AZ, BA..BZ，小版本（iteration）为数字。
 * <p>修正旧系统超过 AG 后进位错乱的缺陷，用 26 进制双射换算实现。</p>
 */
public final class VersionFormatter {

    private VersionFormatter() {
    }

    /** 数字转字母序：1→A，26→Z，27→AA，52→AZ，53→BA。 */
    public static String letter(int revision) {
        if (revision <= 0) {
            return "A";
        }
        StringBuilder builder = new StringBuilder();
        int value = revision;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return builder.toString();
    }

    /** 完整版本标签：A.1 / B.3。 */
    public static String label(Integer revision, Integer iteration) {
        int rev = revision == null || revision <= 0 ? 1 : revision;
        int iter = iteration == null || iteration <= 0 ? 1 : iteration;
        return letter(rev) + "." + iter;
    }
}
