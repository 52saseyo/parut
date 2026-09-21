package com.parut.notification.notification.application.port.in;

import com.parut.notification.notification.application.dto.TimeDealOpeningSoonCommand;

public interface TimeDealOpeningSoonNotificationUseCase {
    void createNotifications(TimeDealOpeningSoonCommand command);
}
