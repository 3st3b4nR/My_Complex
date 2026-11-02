package com.mycomplex.news.application.port.in;

import com.mycomplex.news.domain.model.News;

public interface CreateNewsUseCase {

    News create(CreateNewsCommand command);
}