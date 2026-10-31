package me.yeonjae.ovlo.application.service.office;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    @DisplayName("BOM 으로 시작하고 CRLF 로 끝내며, 쉼표·따옴표·개행은 감싸고 수식 시작 문자는 무력화한다")
    void escapes() {
        String csv = new CsvWriter().header("id", "text").row(1, "a, \"b\"\nc").row(2, "=SUM(A1)").row(3, null).build();

        assertThat(csv).startsWith(CsvWriter.BOM + "id,text\r\n");
        assertThat(csv).contains("1,\"a, \"\"b\"\"\nc\"\r\n");
        assertThat(csv).contains("2,'=SUM(A1)\r\n");
        assertThat(csv).endsWith("3,\r\n");
    }
}
