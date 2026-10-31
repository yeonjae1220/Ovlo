package me.yeonjae.ovlo.application.service.office;

import java.util.List;

/**
 * 엑셀에서 바로 열리는 CSV: UTF-8 BOM + CRLF. 쉼표·따옴표·개행은 따옴표로 감싸고,
 * `= + - @` 로 시작하는 셀은 수식 주입을 막으려고 작은따옴표를 앞에 붙인다.
 */
public final class CsvWriter {

    public static final String BOM = "﻿";

    private final StringBuilder sb = new StringBuilder(BOM);

    public CsvWriter header(String... columns) {
        return row((Object[]) columns);
    }

    public CsvWriter row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
        return this;
    }

    public CsvWriter row(List<?> cells) {
        return row(cells.toArray());
    }

    public String build() {
        return sb.toString();
    }

    static String escape(Object cell) {
        if (cell == null) return "";
        String s = String.valueOf(cell);
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        boolean needsQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        return needsQuote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
