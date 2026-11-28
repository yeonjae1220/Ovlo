package me.yeonjae.ovlo.domain.help.model;

import java.util.regex.Pattern;

/**
 * 운영팀이 쓴 서비스 도움말 문서 1건. AI 답변의 유일한 근거이므로 비어 있거나 식별 불가한 문서는 만들지 않는다.
 * id 는 화면 앵커(/help#id)로도 쓰므로 소문자·숫자·하이픈만 허용한다.
 */
public record HelpArticle(String id, String title, String body) {

    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    public static final int MAX_TITLE_LENGTH = 200;

    public HelpArticle {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("도움말 문서 id 형식이 잘못됐습니다: " + id);
        }
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("도움말 문서 제목이 비었거나 너무 깁니다: " + id);
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("도움말 문서 본문이 비었습니다: " + id);
        }
    }
}
