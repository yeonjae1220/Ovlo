package me.yeonjae.ovlo.application.port.in.help;

import me.yeonjae.ovlo.application.dto.result.HelpArticleResult;

import java.util.List;

public interface ListHelpArticlesQuery {
    List<HelpArticleResult> list();
}
