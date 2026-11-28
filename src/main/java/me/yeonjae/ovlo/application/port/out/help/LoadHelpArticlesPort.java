package me.yeonjae.ovlo.application.port.out.help;

import me.yeonjae.ovlo.domain.help.model.HelpArticle;

import java.util.List;

/** 도움말 문서 전체. 문서가 적어 검색 없이 전부를 근거로 넘기므로 전체 크기에 상한을 둔다. */
public interface LoadHelpArticlesPort {

    /** 제목+본문 UTF-8 바이트 합계 상한. 답변 요청이 InquiryAdmissionPort.MAX_INPUT_BYTES 안에 들어가야 한다. */
    int MAX_CORPUS_BYTES = 30_000;

    List<HelpArticle> loadAll();
}
