package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.HelpArticleResult;
import me.yeonjae.ovlo.application.port.in.help.ListHelpArticlesQuery;
import me.yeonjae.ovlo.application.port.out.help.LoadHelpArticlesPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HelpArticleQueryService implements ListHelpArticlesQuery {

    private final LoadHelpArticlesPort articlesPort;

    public HelpArticleQueryService(LoadHelpArticlesPort articlesPort) {
        this.articlesPort = articlesPort;
    }

    @Override
    public List<HelpArticleResult> list() {
        return articlesPort.loadAll().stream().map(HelpArticleResult::from).toList();
    }
}
