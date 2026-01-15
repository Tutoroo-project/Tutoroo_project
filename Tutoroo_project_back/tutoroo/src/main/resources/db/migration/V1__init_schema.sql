SET NAMES utf8mb4;

-- 1. 사용자 테이블
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `name` VARCHAR(50),
    `gender` VARCHAR(10),
    `age` INT,
    `phone` VARCHAR(20),
    `email` VARCHAR(100) UNIQUE,
    `profile_image` VARCHAR(255),
    `parent_phone` VARCHAR(20),
    `provider` VARCHAR(20),
    `provider_id` VARCHAR(100),
    `role` VARCHAR(20) DEFAULT 'ROLE_USER',
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `withdrawal_reason` TEXT,
    `deleted_at` DATETIME,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 학습 플랜 테이블
CREATE TABLE IF NOT EXISTS `study_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `goal` VARCHAR(255),
    `persona` VARCHAR(50),
    `custom_tutor_name` VARCHAR(50),
    `roadmap_json` LONGTEXT,
    `start_date` DATE,
    `end_date` DATE,
    `progress_rate` DOUBLE DEFAULT 0.0,
    `status` VARCHAR(20) DEFAULT 'PROCEEDING',
    `is_paid` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 결제 테이블
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `plan_id` BIGINT,
    `imp_uid` VARCHAR(100) UNIQUE,
    `merchant_uid` VARCHAR(100) UNIQUE,
    `item_name` VARCHAR(100),
    `pay_method` VARCHAR(50),
    `pg_provider` VARCHAR(50),
    `amount` INT,
    `status` VARCHAR(20),
    `paid_at` DATETIME,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 프롬프트 관리 테이블
CREATE TABLE IF NOT EXISTS `prompts` (
    `prompt_key` VARCHAR(50) PRIMARY KEY,
    `content` TEXT NOT NULL,
    `description` VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. TTS 캐시 테이블
CREATE TABLE IF NOT EXISTS `tts_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `text_hash` VARCHAR(64) NOT NULL,
    `audio_path` VARCHAR(255) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_text_hash` (`text_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. [핵심] 펫(다마고치) 상세 정보 테이블
CREATE TABLE IF NOT EXISTS `pet_info` (
    `pet_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `pet_name` VARCHAR(20) DEFAULT '알',

    -- 기본 스탯
    `fullness` INT DEFAULT 80,
    `intimacy` INT DEFAULT 50,
    `exp` INT DEFAULT 0,

    -- 심화 스탯 (High Quality)
    `cleanliness` INT DEFAULT 100,
    `stress` INT DEFAULT 0,
    `energy` INT DEFAULT 100,
    `is_sleeping` BOOLEAN DEFAULT FALSE,

    -- 성장 및 외형
    `stage` INT DEFAULT 1,
    `pet_type` VARCHAR(50) DEFAULT 'EGG',
    `equipped_items` JSON,

    -- 시간 계산
    `last_fed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_played_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_cleaned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_slept_at` DATETIME,
    `birth_date` DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 펫 진화 규칙 테이블
CREATE TABLE IF NOT EXISTS `pet_evolution_rule` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `current_stage` INT,
    `required_exp` INT,
    `next_stage` INT,
    `next_pet_type` VARCHAR(50),
    `description` VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `pet_evolution_rule` (current_stage, required_exp, next_stage, next_pet_type, description)
VALUES (1, 100, 2, 'BABY_SLIME', '알 부화');

-- 9. 펫의 비밀 일기장 (AI 감성)
CREATE TABLE IF NOT EXISTS `pet_diary` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `pet_id` BIGINT NOT NULL,
    `date` DATE NOT NULL,
    `content` TEXT,
    `mood` VARCHAR(20),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pet_info`(`pet_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 펫 스킬/버프 정보 (RPG 요소)
CREATE TABLE IF NOT EXISTS `pet_skills` (
    `skill_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `pet_type` VARCHAR(50),
    `skill_code` VARCHAR(50),
    `effect_value` DOUBLE,
    `description` VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `pet_skills` (pet_type, skill_code, effect_value, description)
VALUES ('BABY_SLIME', 'POINT_BOOST', 1.05, '포인트 획득량 5% 증가');