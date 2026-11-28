package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.help.model.HelpArticle;

public record HelpArticleResult(String id, String title, String body) {
    public static HelpArticleResult from(HelpArticle article) {
        return new HelpArticleResult(article.id(), article.title(), article.body());
    }
}
