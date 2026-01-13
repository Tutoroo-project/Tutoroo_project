/* V1__init_schema.sql */

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

    -- [추가] 학부모 리포트용 연락처
    `parent_phone` VARCHAR(20),

    `provider` VARCHAR(20),
    `provider_id` VARCHAR(100),
    `role` VARCHAR(20) DEFAULT 'ROLE_USER',
    `membership_tier` VARCHAR(20) DEFAULT 'BASIC',

    `total_point` INT DEFAULT 0,
    `daily_rank` INT DEFAULT 0,
    `level` INT DEFAULT 1,
    `exp` INT DEFAULT 0,

    -- [추가] 스트릭 시스템 & 라이벌
    `current_streak` INT DEFAULT 0,
    `last_study_date` DATE,
    `rival_id` BIGINT,

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (`rival_id`) REFERENCES `users`(`id`)
    );

-- 2. 학습 플랜 테이블
CREATE TABLE IF NOT EXISTS `study_plans` (
                                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             `user_id` BIGINT NOT NULL,
                                             `goal` VARCHAR(255),
    `persona` VARCHAR(50),
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

-- 4. 결제 테이블 (PaymentEntity와 필드 일치화)
CREATE TABLE IF NOT EXISTS `payments` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `user_id` BIGINT NOT NULL,
                                          `plan_id` BIGINT,                 -- 특정 플랜 결제 시 사용 (구독형이면 NULL 가능)
                                          `imp_uid` VARCHAR(100) UNIQUE,    -- 포트원 결제 고유 번호
    `merchant_uid` VARCHAR(100),      -- 가맹점 주문 번호
    `item_name` VARCHAR(100),         -- 상품명 (예: PREMIUM SUBSCRIPTION)
    `pay_method` VARCHAR(50),         -- 결제 수단 (card, trans 등)
    `pg_provider` VARCHAR(50),        -- PG사 정보
    `amount` INT,                     -- 결제 금액
    `status` VARCHAR(20),             -- PAID, CANCELLED 등
    `paid_at` DATETIME,               -- 결제 일시
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    );

-- 5. 프롬프트 관리 테이블
CREATE TABLE IF NOT EXISTS `prompts` (
                                         `prompt_key` VARCHAR(50) PRIMARY KEY,
    `content` TEXT NOT NULL,
    `description` VARCHAR(100)
    );

-- 6. TTS 캐시 테이블
CREATE TABLE IF NOT EXISTS `tts_cache` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `text_hash` VARCHAR(64) NOT NULL,
    `audio_base64` LONGTEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_text_hash` (`text_hash`)
    );

-- [초기 데이터: 프롬프트]
INSERT IGNORE INTO `prompts` (`prompt_key`, `content`, `description`) VALUES
('CLASS_START', '페르소나: %s. \n[이전 수업 피드백: %s]. \n상황: %d일차 수업 시작(%s). \n지시: 위 피드백을 반영하여 오프닝 멘트를 작성해.', '수업 시작 인사'),
('TEST_GENERATE', '주제: %s의 %d일차 학습. 간단한 서술형 퀴즈 1개를 내줘.', '데일리 테스트 생성'),
('TEST_FEEDBACK', '학생 답안: %s. 점수(Score: 숫자)와 해설, 그리고 복습용 요약(★)을 작성해.', '테스트 채점 및 피드백'),
('Chat_FEEDBACK', '학생 피드백: %s. 격려하고 조언해줘.', '커리큘럼 조정 대화'),
('PARENT_REPORT', '학생이름: %s, 주간점수: %s, 피드백요약: %s. \n지시: 학부모에게 보낼 정중하고 긍정적인 알림톡 메시지를 작성해. (최대 300자)', '학부모 리포트 생성');