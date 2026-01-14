package com.tutoroo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * [기능: 파일 저장소 유틸리티]
 * 설명: 바이트 배열(byte[]) 또는 Base64 문자열을 받아 서버 디스크에 저장하고, 웹 접근 경로를 반환합니다.
 */
@Slf4j
@Component
public class FileStore {

    // 파일이 저장될 로컬 경로 (application.yml에서 설정, 기본값: ./uploads/audio/)
    @Value("${file.upload-dir:./uploads/audio/}")
    private String uploadDir;

    /**
     * 1. [핵심] 바이트 배열(byte[])을 파일로 저장 (TutorService용 최적화 메서드)
     * @param fileData 저장할 파일의 바이트 데이터
     * @param extension 파일 확장자 (예: ".mp3", ".png")
     * @return 웹에서 접근 가능한 URL 경로 (예: "/audio/uuid.mp3")
     */
    public String storeFile(byte[] fileData, String extension) {
        if (fileData == null || fileData.length == 0) {
            throw new RuntimeException("파일 데이터가 비어있습니다.");
        }

        try {
            // 1. 디렉토리 확인 및 생성
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    log.warn("디렉토리 생성 실패 또는 이미 존재함: {}", uploadDir);
                }
            }

            // 2. 랜덤 파일명 생성 (UUID 사용)
            String fileName = UUID.randomUUID() + extension;
            File destination = new File(directory, fileName);

            // 3. 파일 쓰기
            try (FileOutputStream fos = new FileOutputStream(destination)) {
                fos.write(fileData);
            }

            log.info("파일 저장 완료: {}", destination.getAbsolutePath());

            // 4. 웹 접근 경로 반환 (/audio/파일명.확장자)
            // 주의: WebMvcConfig에서 /audio/** 패턴이 uploadDir와 매핑되어 있어야 함
            return "/audio/" + fileName;

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    /**
     * 2. [호환성] Base64 문자열을 디코딩하여 저장 (기존 코드 지원)
     * @param base64Audio Base64로 인코딩된 오디오 문자열
     * @return 웹 접근 경로
     */
    public String saveAudio(String base64Audio) {
        if (base64Audio == null || base64Audio.isEmpty()) {
            throw new RuntimeException("Base64 오디오 데이터가 없습니다.");
        }

        // Base64 디코딩 후 위 storeFile 메서드 재사용 (중복 제거)
        byte[] decodedBytes = Base64.getDecoder().decode(base64Audio);
        return storeFile(decodedBytes, ".mp3");
    }
}