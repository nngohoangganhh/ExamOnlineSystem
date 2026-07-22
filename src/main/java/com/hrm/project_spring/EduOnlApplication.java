package com.hrm.project_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduOnlApplication {
    public static void main(String[] args) {
        System.out.println("HỆ THỐNG ĐANG CHẠY VUI LÒNG ĐỢI");
        SpringApplication.run(EduOnlApplication.class, args);
        System.out.println("HỆ THỐNG ĐÃ CHẠY THÀNH CÔNG CẢM ƠN BẠN ĐÃ ĐỢI");
    }

}
