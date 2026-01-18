package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.response.EmailMessage;
import com.kma.ojcore.service.EmailConsumerService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerServiceImpl implements EmailConsumerService {
    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void receiveEmailMessage(EmailMessage message) {
        log.info("Nhận yêu cầu gửi mail đến: {}", message.getTo());
        try {
            sendHtmlEmail(message.getTo(), message.getSubject(), message.getContent());
        } catch (Exception e) {
            log.error("Gửi mail thất bại: {}", e.getMessage());
            // Có thể implement logic retry hoặc đẩy vào Dead Letter Queue (DLQ)
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // True = Render HTML
        helper.setFrom("noreply@ojcore.com"); // Email gửi đi

        mailSender.send(message);
        log.info("Đã gửi mail thành công!");
    }

}
