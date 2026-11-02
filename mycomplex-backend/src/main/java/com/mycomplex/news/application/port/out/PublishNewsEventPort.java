package com.mycomplex.news.application.port.out;

import com.mycomplex.news.domain.model.News;

public interface PublishNewsEventPort {

    void publishCreated(News news);

    void publishUpdated(News news);
}