package com.mycomplex.news.application.port.out;

import com.mycomplex.news.domain.model.News;

public interface UpdateNewsPort {

    News update(News news);
}
