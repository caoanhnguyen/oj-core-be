package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.auth.EmailMessage;

public interface EmailConsumerService {
    void receiveEmailMessage(EmailMessage message);
}
