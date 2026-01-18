package com.kma.ojcore;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OjCoreApplication {

    public static void main(String[] args) {
        // Load .env từ thư mục gốc
        Dotenv dotenv = Dotenv.configure().directory("./").ignoreIfMissing().load();

        // Nạp tất cả biến môi trường vào hệ thống
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(OjCoreApplication.class, args);
    }

}
