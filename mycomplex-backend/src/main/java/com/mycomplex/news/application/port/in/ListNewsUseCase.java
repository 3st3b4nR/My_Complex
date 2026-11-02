package com.mycomplex.news.application.port.in;

import com.mycomplex.news.application.model.NewsPage;

public interface ListNewsUseCase {

    NewsPage list(ListNewsQuery query);
}
