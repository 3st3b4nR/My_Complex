package com.mycomplex.news.application.port.out;

import java.util.Optional;

import com.mycomplex.news.application.model.NewsImageContent;
import com.mycomplex.news.domain.model.NewsImage;

public interface LoadNewsImageContentPort {

    Optional<NewsImageContent> load(NewsImage image);
}
