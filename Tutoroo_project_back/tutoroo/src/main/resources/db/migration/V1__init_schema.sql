/* V1__init_schema.sql - 최종 완성본 (2026.01.14 업데이트) */

-- 1. 사용자 테이블
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `name` VARCHAR(50),
    `gender` VARCHAR(10),
    `age` INT,
    `phone` VARCHAR(20),
    `email` VARCHAR(100),
    `profile_image` VARCHAR(255),
    `parent_phone` VARCHAR(20),
    `provider` VARCHAR(20),
    `provider_id` VARCHAR(100),
    `role` VARCHAR(20) DEFAULT 'ROLE_USER',
    `membership_tier` VARCHAR(20) DEFAULT 'BASIC',
    `total_point` INT DEFAULT 0,
    `daily_rank` INT DEFAULT 0,
    `level` INT DEFAULT 1,
    `exp` INT DEFAULT 0,
    `current_streak` INT DEFAULT 0,
    `last_study_date` DATE,
    `rival_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`rival_id`) REFERENCES `users`(`id`)
    );

-- 2. 학습 플랜 테이블 (커스텀 튜터 이름 추가)
CREATE TABLE IF NOT EXISTS `study_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `goal` VARCHAR(255),
    `persona` VARCHAR(50), -- 1일차에 선택한 기본 베이스 (호랑이 등)
    `custom_tutor_name` VARCHAR(50), -- [NEW] 학생이 지어준 커스텀 선생님 이름 (예: 김춘식)
    `roadmap_json` LONGTEXT,
    `start_date` DATE,
    `end_date` DATE,
    `progress_rate` DOUBLE DEFAULT 0.0,
    `status` VARCHAR(20) DEFAULT 'PROCEEDING',
    `is_paid` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    );

-- 3. 학습 로그 테이블
CREATE TABLE IF NOT EXISTS `study_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_id` BIGINT NOT NULL,
    `study_date` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `day_count` INT,
    `content_summary` TEXT,
    `daily_summary` TEXT,
    `test_score` INT,
    `ai_feedback` TEXT,
    `student_feedback` TEXT,
    `point_change` INT DEFAULT 0,
    `is_completed` BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (`plan_id`) REFERENCES `study_plans`(`id`)
    );

-- 4. 결제 테이블
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `plan_id` BIGINT,
    `imp_uid` VARCHAR(100) UNIQUE,
    `merchant_uid` VARCHAR(100),
    `item_name` VARCHAR(100),
    `pay_method` VARCHAR(50),
    `pg_provider` VARCHAR(50),
    `amount` INT,
    `status` VARCHAR(20),
    `paid_at` DATETIME,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    );

-- 5. 프롬프트 관리 테이블
CREATE TABLE IF NOT EXISTS `prompts` (
    `prompt_key` VARCHAR(50) PRIMARY KEY,
    `content` TEXT NOT NULL,
    `description` VARCHAR(100)
    );

-- 6. TTS 캐시 테이블 (수정 완료)
-- [변경점] audio_base64(LONGTEXT) -> audio_path(VARCHAR)
-- 이유: 파일을 DB에 직접 넣지 않고 로컬 경로만 저장하여 성능 최적화
CREATE TABLE IF NOT EXISTS `tts_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `text_hash` VARCHAR(64) NOT NULL,
    `audio_path` VARCHAR(255) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_text_hash` (`text_hash`)
    );