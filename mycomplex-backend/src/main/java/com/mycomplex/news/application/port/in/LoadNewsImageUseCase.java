package com.mycomplex.news.application.port.in;

import com.mycomplex.news.application.model.NewsImageContent;

public interface LoadNewsImageUseCase {

    NewsImageContent load(LoadNewsImageQuery query);
}
