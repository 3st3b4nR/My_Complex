package com.mycomplex.notifications.application.port.in;

import com.mycomplex.news.domain.event.NewsChangedEvent;

public interface NotifyNewsChangedUseCase {

    void notify(NewsChangedEvent event);
}
