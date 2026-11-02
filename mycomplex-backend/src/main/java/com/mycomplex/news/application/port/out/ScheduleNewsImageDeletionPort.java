package com.mycomplex.news.application.port.out;

import com.mycomplex.news.domain.model.NewsImage;

public interface ScheduleNewsImageDeletionPort {

    void schedule(NewsImage image);
}
