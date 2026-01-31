SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0; -- 초기화 시 외래키 제약 임시 해제

-- -----------------------------------------------------
-- 1. 사용자 (Users)
-- [매핑]: UserEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
                                       `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       `username`          VARCHAR(100) NOT NULL UNIQUE COMMENT '로그인 아이디 (OAuth2 ID 포함)',
    `password`          VARCHAR(255) NOT NULL,
    `name`              VARCHAR(50),
    `gender`            VARCHAR(10),
    `age`               INT,
    `phone`             VARCHAR(20),
    `email`             VARCHAR(100) UNIQUE,
    `profile_image`     VARCHAR(512),

    `parent_phone`      VARCHAR(20),
    `provider`          VARCHAR(20),
    `provider_id`       VARCHAR(100),

    `role`              VARCHAR(20) DEFAULT 'ROLE_USER',
    `status`            VARCHAR(20) DEFAULT 'ACTIVE',
    `withdrawal_reason` TEXT,
    `deleted_at`        DATETIME,

    `membership_tier`   VARCHAR(20) DEFAULT 'BASIC',
    `total_point`       INT DEFAULT 0,
    `point_balance`     INT DEFAULT 0,

    `daily_rank`        INT DEFAULT 0,
    `level`             INT DEFAULT 1,
    `exp`               INT DEFAULT 0,
    `current_streak`    INT DEFAULT 0,
    `last_study_date`   DATE,
    `rival_id`          BIGINT,

    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (`rival_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_users_total_point` (`total_point` DESC)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 2. 학습 플랜 (Study Plans)
-- [매핑]: StudyPlanEntity.java
-- [핵심]: roadmap_json (LONGTEXT) 필수
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_plans` (
                                             `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             `user_id`           BIGINT NOT NULL,
                                             `goal`              TEXT NOT NULL,

                                             `persona`           VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '상담 시엔 DEFAULT, 수업 시작 시 변경',
    `custom_tutor_name` VARCHAR(50),

    `roadmap_json`      LONGTEXT COMMENT '[중요] AI 생성 로드맵 전체 JSON',
    `current_level`     VARCHAR(50),
    `target_level`      VARCHAR(50),

    `start_date`        DATE,
    `end_date`          DATE,
    `progress_rate`     DOUBLE DEFAULT 0.0,

    `is_paid`           BOOLEAN DEFAULT FALSE,
    `status`            VARCHAR(20) DEFAULT 'PROCEEDING',

    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 3. 학습 로그 (Study Logs)
-- [매핑]: StudyLogEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_logs` (
                                            `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `plan_id`           BIGINT NOT NULL,
                                            `study_date`        DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            `day_count`         INT NOT NULL COMMENT 'N일차',

                                            `content_summary`   TEXT COMMENT '학습 내용 요약',
                                            `daily_summary`     TEXT,

                                            `ai_feedback`        TEXT,
                                            `ai_feedback_status` VARCHAR(20) DEFAULT 'PENDING',
    `ai_feedback_at`     DATETIME NULL,

    `test_score`        INT DEFAULT 0,
    `student_feedback`  TEXT, -- (Legacy)

    `point_change`      INT DEFAULT 0,
    `is_completed`      BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (`plan_id`) REFERENCES `study_plans`(`id`) ON DELETE CASCADE,
    INDEX `idx_study_logs_plan_day` (`plan_id`, `day_count`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- [NEW] 4. 학생 피드백 (Student Feedbacks)
-- [매핑]: TutorService.saveStudentFeedback 대응
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `student_feedbacks` (
                                                   `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   `plan_id`       BIGINT NOT NULL,
                                                   `day_count`     INT,
                                                   `feedback_text` TEXT COMMENT '학생이 선생님에게 남긴 말',
                                                   `rating`        INT COMMENT '별점 (1~5)',
                                                   `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,

                                                   FOREIGN KEY (`plan_id`) REFERENCES `study_plans`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 5. 펫 정보 (Pet Info)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_info` (
                                          `pet_id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `user_id`           BIGINT NOT NULL,
                                          `pet_name`          VARCHAR(50),
    `pet_type`          VARCHAR(30) NOT NULL,

    `custom_description` TEXT,
    `custom_image_url`   VARCHAR(512),

    `stage`             INT DEFAULT 1,
    `status`            VARCHAR(20) DEFAULT 'ACTIVE',

    `fullness`          INT DEFAULT 80,
    `intimacy`          INT DEFAULT 80,
    `exp`               INT DEFAULT 0,
    `cleanliness`       INT DEFAULT 100,
    `stress`            INT DEFAULT 0,
    `energy`            INT DEFAULT 100,
    `is_sleeping`       BOOLEAN DEFAULT FALSE,

    `equipped_items`    LONGTEXT,

    `last_fed_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_played_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_cleaned_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_slept_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `birth_date`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 6. 펫 다이어리 (Pet Diary)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_diary` (
                                           `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `pet_id`        BIGINT NOT NULL,
                                           `date`          DATE NOT NULL,
                                           `content`       TEXT,
                                           `mood`          VARCHAR(20),
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`pet_id`) REFERENCES `pet_info`(`pet_id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 7. 결제 내역 (Payments)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `payments` (
                                          `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `user_id`       BIGINT NOT NULL,
                                          `plan_id`       BIGINT,

                                          `imp_uid`       VARCHAR(100) NOT NULL UNIQUE,
    `merchant_uid`  VARCHAR(100) NOT NULL UNIQUE,
    `item_name`     VARCHAR(100),

    `pay_method`    VARCHAR(20),
    `pg_provider`   VARCHAR(20),
    `amount`        INT NOT NULL,
    `status`        VARCHAR(20) NOT NULL,

    `paid_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 8. TTS 캐시 (TTS Cache)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tts_cache` (
                                           `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `text_hash`     VARCHAR(64) NOT NULL UNIQUE,
    `audio_path`    VARCHAR(255) NOT NULL,
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_tts_hash` (`text_hash`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 9. 프롬프트 관리 (Prompts)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prompts` (
                                         `prompt_key`    VARCHAR(50) PRIMARY KEY,
    `content`       TEXT NOT NULL,
    `description`   VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 10. 펫 성장 규칙
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_growth_rule` (
                                                 `stage`         INT PRIMARY KEY,
                                                 `stage_name`    VARCHAR(20),
    `required_exp`  INT,
    `description`   VARCHAR(100)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 11. 펫 스킬 정보
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_skills` (
                                            `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `pet_type`      VARCHAR(50),
    `skill_name`    VARCHAR(50),
    `skill_code`    VARCHAR(50),
    `effect_value`  DOUBLE,
    `description`   VARCHAR(200),
    INDEX `idx_pet_skill_type` (`pet_type`, `skill_code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 12. 알림 (Notifications)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
                                               `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               `user_id`       BIGINT NOT NULL,
                                               `title`         VARCHAR(100),
    `message`       TEXT NOT NULL,
    `type`          VARCHAR(20) DEFAULT 'INFO',
    `is_read`       BOOLEAN DEFAULT FALSE,
    `related_url`   VARCHAR(255),
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_noti_user_read` (`user_id`, `is_read`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 13. 실전 문제 은행 (Practice Questions)
-- [매핑]: PracticeQuestionEntity.java
-- =====================================================
CREATE TABLE IF NOT EXISTS `practice_questions` (
                                                    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    `plan_id`       BIGINT NOT NULL,
                                                    `content_hash`  VARCHAR(64) NOT NULL COMMENT '문제 내용의 SHA-256 해시',
    `question_json` TEXT NOT NULL COMMENT '문제 내용 전체 JSON (지문, 보기, 정답, 해설 등)',
    `topic`         VARCHAR(100),
    `question_type` VARCHAR(50),
    `difficulty`    INT DEFAULT 3,

    `image_url`     VARCHAR(512) COMMENT '[New] AI 생성 이미지 경로',

    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_content_hash` (`content_hash`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 14. 실전 풀이 기록 (Practice Logs)
-- [매핑]: PracticeLogEntity.java
-- =====================================================
CREATE TABLE IF NOT EXISTS `practice_logs` (
                                               `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               `user_id`       BIGINT NOT NULL,
                                               `question_id`   BIGINT NOT NULL,
                                               `user_answer`   TEXT,
                                               `is_correct`    BOOLEAN,
                                               `ai_feedback`   TEXT,
                                               `solved_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,

                                               FOREIGN KEY (`question_id`) REFERENCES `practice_questions`(`id`) ON DELETE CASCADE,
    INDEX `idx_practice_log_user` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- [NEW] 15. 수업 대화 기록 (Chat History) - 영구 기억장치
-- [매핑]: ChatMapper 내부 DTO
-- =====================================================
CREATE TABLE IF NOT EXISTS `chat_messages` (
                                               `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               `plan_id`       BIGINT NOT NULL,
                                               `sender`        VARCHAR(10) NOT NULL COMMENT 'USER or AI',
    `message`       TEXT NOT NULL,
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX `idx_chat_plan` (`plan_id`, `created_at`) -- 시간순 조회 최적화
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================
-- [DATA SEEDING] 초기 필수 데이터 삽입
-- =====================================================

INSERT IGNORE INTO `pet_growth_rule` VALUES
(1, 'EGG', 100, '알 단계'),
(2, 'BABY', 600, '아기 단계'),
(3, 'TEEN', 1500, '청소년 단계'),
(4, 'ADULT', 3000, '어른 단계'),
(5, 'MASTER', 999999, '졸업 단계');

-- [중요 수정] AssessmentService에서 사용하는 'CONSULT_SYSTEM' 추가됨
INSERT IGNORE INTO `prompts` (`prompt_key`, `content`, `description`) VALUES
('CONSULT_SYSTEM', '너는 15년 경력의 입시 전문 컨설턴트야. 학생의 목표를 듣고 냉철하고 분석적으로 상담해. 특정 선생님 스타일을 연기하지 말고 중립을 지켜.', '입학 상담 시스템'),
('TEACHER_TIGER', '너는 엄격한 호랑이 선생님이야. 학생이 나태해지면 따끔하게 혼내고, 반말을 사용하며 강력하게 리드해.', '호랑이 선생님'),
('TEACHER_RABBIT', '너는 성격 급한 토끼 선생님이야. 서론은 생략하고 핵심만 빠르게 말해.', '토끼 선생님'),
('TEACHER_TURTLE', '너는 인자한 거북이 선생님이야. 존댓말을 쓰고 칭찬을 많이 해줘.', '거북이 선생님'),
('TEACHER_KANGAROO', '너는 헬스장 관장님 같은 캥거루 선생님이야. "할 수 있다!"라고 외치며 에너지를 줘.', '캥거루 선생님'),
('TEACHER_EASTERN_DRAGON', '너는 천 년 묵은 동양의 용 선생님이야. 하오체를 쓰며 지혜를 전달해.', '동양용 선생님'),
('TEST_ADAPTIVE', '[역할]: %s\n[상황]: 학생의 점수 %d점.\n[주제]: %s\n퀴즈를 내줘.', '적응형 테스트'),
('TEST_FEEDBACK', '주제: %s\n학생 답안: %s\n채점하고 피드백해.', '테스트 피드백');

INSERT IGNORE INTO `pet_skills` (`pet_type`, `skill_name`, `skill_code`, `effect_value`, `description`) VALUES
('TIGER', '맹수의 집중', 'POINT_BOOST', 1.2, '포인트 20% 증가'),
('RABBIT', '신속한 학습', 'EXP_BOOST', 1.1, '경험치 10% 증가'),
('TURTLE', '꾸준한 인내', 'STREAK_PROTECT', 1.0, '스트릭 보호'),
('KANGAROO', '폭발적 에너지', 'ENERGY_REDUCE', 0.8, '에너지 소모 감소'),
('EASTERN_DRAGON', '용의 지혜', 'ALL_BOOST', 1.15, '모든 보상 증가');

SET FOREIGN_KEY_CHECKS = 1;