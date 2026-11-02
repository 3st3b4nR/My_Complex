package com.mycomplex.news.application.port.in;

import com.mycomplex.news.domain.model.News;

public interface UpdateNewsUseCase {

    News update(UpdateNewsCommand command);
}
