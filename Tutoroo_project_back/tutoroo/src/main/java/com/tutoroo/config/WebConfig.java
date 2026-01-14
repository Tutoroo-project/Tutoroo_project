package com.tutoroo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * [기능: 정적 리소스 매핑 설정]
 * 설명: 로컬 디스크에 저장된 파일들을 웹 URL로 접근할 수 있도록 경로를 매핑합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads/audio/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL 패턴: /audio/** 요청이 오면 -> 로컬의 uploadDir 폴더를 찾음
        String resourcePath = "file:" + uploadDir;

        // 윈도우/맥/리눅스 경로 호환성 처리
        if (!resourcePath.endsWith("/")) {
            resourcePath += "/";
        }

        registry.addResourceHandler("/audio/**")
                .addResourceLocations(resourcePath);
    }
}