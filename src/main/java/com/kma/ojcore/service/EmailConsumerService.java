package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.EmailMessage;

public interface EmailConsumerService {
    void receiveEmailMessage(EmailMessage message);
}
