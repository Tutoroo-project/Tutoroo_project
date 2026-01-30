SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0; -- 초기화 시 외래키 제약 임시 해제

-- -----------------------------------------------------
-- 1. 사용자 (Users)
-- [매핑]: UserEntity.java
-- [수정]: nickname 컬럼 추가 및 인덱스 최적화
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
                                       `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       `username`          VARCHAR(100) NOT NULL UNIQUE COMMENT '로그인 아이디 (OAuth2 ID 포함)',
    `password`          VARCHAR(255) NOT NULL,
    `name`              VARCHAR(50),
    `nickname`          VARCHAR(50) COMMENT '[필수] 닉네임',
    `gender`            VARCHAR(10),
    `age`               INT,
    `phone`             VARCHAR(20),
    `email`             VARCHAR(100) UNIQUE,
    `profile_image`     VARCHAR(512) COMMENT 'URL 길이가 길어질 수 있음',

    -- 학부모 알림용
    `parent_phone`      VARCHAR(20),

    -- 소셜 로그인 (OAuth2)
    `provider`          VARCHAR(20) COMMENT 'google, kakao, naver',
    `provider_id`       VARCHAR(100),

    -- 권한 및 상태
    `role`              VARCHAR(20) DEFAULT 'ROLE_USER',
    `status`            VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, WITHDRAWN, BANNED',
    `withdrawal_reason` TEXT,
    `deleted_at`        DATETIME,

    -- [핵심] 멤버십 및 포인트
    `membership_tier`   VARCHAR(20) DEFAULT 'BASIC' COMMENT 'BASIC, STANDARD, PREMIUM',
    `total_point`       INT DEFAULT 0 COMMENT '누적 랭킹 포인트 (감소 안 함)',
    `point_balance`     INT DEFAULT 0 COMMENT '사용 가능 포인트 (지갑)',

    -- 게이미피케이션 정보
    `daily_rank`        INT DEFAULT 0,
    `level`             INT DEFAULT 1,
    `exp`               INT DEFAULT 0,
    `current_streak`    INT DEFAULT 0,
    `last_study_date`   DATE,
    `rival_id`          BIGINT COMMENT '라이벌 유저 ID (Self Referencing)',

    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (`rival_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_users_total_point` (`total_point` DESC) -- 랭킹 조회 속도 최적화
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 2. 학습 플랜 (Study Plans)
-- [매핑]: StudyPlanEntity.java
-- [수정]: goal 컬럼 TEXT로 변경, 레벨 정보 추가
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_plans` (
                                             `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             `user_id`           BIGINT NOT NULL,
                                             `goal`              TEXT NOT NULL COMMENT '학습 목표 (길이 제한 완화)',

                                             `persona`           VARCHAR(50) COMMENT '선생님 타입 (TIGER, RABBIT 등)',
    `custom_tutor_name` VARCHAR(50) COMMENT '선생님 애칭',

    `roadmap_json`      LONGTEXT COMMENT 'JSON: 전체 커리큘럼 데이터',
    `start_date`        DATE,
    `end_date`          DATE,
    `progress_rate`     DOUBLE DEFAULT 0.0,

    -- [필수 추가] 학습 난이도 및 목표 레벨 (Mapper와 연동)
    `current_level`     VARCHAR(50) COMMENT '현재 실력 (예: BEGINNER)',
    `target_level`      VARCHAR(50) COMMENT '목표 실력 (예: ADVANCED)',

    `is_paid`           BOOLEAN DEFAULT FALSE,
    `status`            VARCHAR(20) DEFAULT 'PROCEEDING', -- PROCEEDING, COMPLETED

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
                                            `daily_summary`     TEXT COMMENT '복습용 상세 요약',

                                            `ai_feedback`        TEXT COLLATE utf8mb4_unicode_ci COMMENT 'AI 강의 피드백',
                                            `ai_feedback_status` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/READY/FAILED',
                                            `ai_feedback_at`     DATETIME NULL COMMENT '피드백 생성 시각',

                                            `test_score`        INT DEFAULT 0,
                                            `student_feedback`  TEXT COMMENT '학생이 남긴 수업 후기 (누락 방지)',

                                            `point_change`      INT DEFAULT 0,
                                            `is_completed`      BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (`plan_id`) REFERENCES `study_plans`(`id`) ON DELETE CASCADE,
    INDEX `idx_study_logs_plan_day` (`plan_id`, `day_count`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 4. 펫 정보 (Pet Info)
-- [매핑]: PetInfoEntity.java
-- [중요 수정]: 커스텀 펫(Step 20) 지원을 위한 컬럼 추가
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_info` (
                                          `pet_id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `user_id`           BIGINT NOT NULL,
                                          `pet_name`          VARCHAR(50),
    `pet_type`          VARCHAR(30) NOT NULL COMMENT 'Enum: TIGER, RABBIT, CUSTOM...',

    -- [New] 커스텀 펫 전용 필드 (AI 생성 정보 저장)
    `custom_description` TEXT COMMENT '커스텀 펫 외형 묘사',
    `custom_image_url`   VARCHAR(512) COMMENT 'AI가 생성한 펫 이미지 URL',

    `stage`             INT DEFAULT 1 COMMENT '1(알)~5(졸업)',
    `status`            VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, GRADUATED, RUNAWAY',

    -- 상태 스탯
    `fullness`          INT DEFAULT 80,
    `intimacy`          INT DEFAULT 80,
    `exp`               INT DEFAULT 0,
    `cleanliness`       INT DEFAULT 100,
    `stress`            INT DEFAULT 0,
    `energy`            INT DEFAULT 100,
    `is_sleeping`       BOOLEAN DEFAULT FALSE,

    `equipped_items`    LONGTEXT COMMENT 'JSON: 착용 아이템 목록',

    -- 시간 기록
    `last_fed_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_played_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_cleaned_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_slept_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `birth_date`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 5. 펫 다이어리 (Pet Diary)
-- [매핑]: PetDiaryEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_diary` (
                                           `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `pet_id`        BIGINT NOT NULL,
                                           `date`          DATE NOT NULL,
                                           `content`       TEXT COMMENT '일기 내용 (이미지 URL 마크다운 포함)',
                                           `mood`          VARCHAR(20) COMMENT 'HAPPY, SAD',
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`pet_id`) REFERENCES `pet_info`(`pet_id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 6. 결제 내역 (Payments)
-- [매핑]: PaymentEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `payments` (
                                          `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `user_id`       BIGINT NOT NULL,
                                          `plan_id`       BIGINT COMMENT '구독형일 경우 NULL',

                                          `imp_uid`       VARCHAR(100) NOT NULL UNIQUE COMMENT '포트원 거래고유번호',
    `merchant_uid`  VARCHAR(100) NOT NULL UNIQUE COMMENT '가맹점 주문번호',
    `item_name`     VARCHAR(100) COMMENT '상품명 (누락 방지)',

    `pay_method`    VARCHAR(20),
    `pg_provider`   VARCHAR(20),
    `amount`        INT NOT NULL,
    `status`        VARCHAR(20) NOT NULL, -- PAID, CANCELLED

    `paid_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 7. TTS 캐시 (TTS Cache)
-- [매핑]: TtsCacheEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tts_cache` (
                                           `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `text_hash`     VARCHAR(64) NOT NULL UNIQUE,
    `audio_path`    VARCHAR(255) NOT NULL COMMENT '저장된 파일의 URL 경로',
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_tts_hash` (`text_hash`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 8. 프롬프트 관리 (Prompts) - 시스템용
-- [매핑]: PromptEntity.java
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prompts` (
                                         `prompt_key`    VARCHAR(50) PRIMARY KEY,
    `content`       TEXT NOT NULL,
    `description`   VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 9. 펫 성장 규칙 (초기 데이터)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pet_growth_rule` (
                                                 `stage`         INT PRIMARY KEY,
                                                 `stage_name`    VARCHAR(20),
    `required_exp`  INT,
    `description`   VARCHAR(100)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 10. 펫 스킬 정보 (초기 데이터) - PetMapper에서 사용
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
-- 11. 알림 (Notifications)
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
-- [DATA SEEDING] 초기 필수 데이터 삽입
-- =====================================================

-- 1. 펫 성장 규칙 초기화
INSERT IGNORE INTO `pet_growth_rule` VALUES
(1, 'EGG', 100, '알 단계'),
(2, 'BABY', 600, '아기 단계'),
(3, 'TEEN', 1500, '청소년 단계'),
(4, 'ADULT', 3000, '어른 단계'),
(5, 'MASTER', 999999, '졸업 단계');

-- 2. 시스템 프롬프트 & 페르소나 초기화
INSERT IGNORE INTO `prompts` (`prompt_key`, `content`, `description`) VALUES
('CONSULT_SYSTEM', '너는 15년 경력의 입시 전문 컨설턴트야. 정중하고 전문적으로 상담해.', '초기 상담'),
('TEACHER_TIGER', '너는 엄격한 호랑이 선생님이야. 학생이 나태해지면 따끔하게 혼내고, 반말을 사용하며 강력하게 리드해. "정신 안 차려?", "이것도 몰라?" 같은 말을 자주 써.', '호랑이 선생님'),
('TEACHER_RABBIT', '너는 성격 급한 토끼 선생님이야. 서론은 생략하고 핵심만 빠르게 말해. "시간 없어! 빨리빨리!", "중요한 건 이거야!"라며 속도감 있게 가르쳐.', '토끼 선생님'),
('TEACHER_TURTLE', '너는 인자한 거북이 선생님이야. 학생이 이해할 때까지 백 번이고 천 번이고 친절하게 설명해줘. 존댓말을 쓰고 "천천히 해도 괜찮아요", "잘하고 있어요"라고 격려해.', '거북이 선생님'),
('TEACHER_KANGAROO', '너는 에너지가 넘치는 헬스장 관장님 같은 캥거루 선생님이야. 공부를 근육 단련하듯이 가르쳐. "원 투! 원 투! 뇌세포에 자극 들어간다!", "할 수 있다! 포기하지 마!"라고 외쳐.', '캥거루 선생님'),
('TEACHER_EASTERN_DRAGON', '너는 천 년을 산 동양의 용 선생님이야. 아주 점잖고 고풍스러운 하오체를 써라. "그대의 배움이 깊어지고 있구려", "이치를 깨달아야 하오"라며 인생의 지혜를 함께 전달해.', '동양용 선생님'),
('TEST_ADAPTIVE', '[역할]: %s\n[상황]: 학생의 평균 점수는 %d점이야. 난이도 \'%s\'로 퀴즈를 하나 내줘.\n[주제]: %s (%d일차)\n문제와 보기만 깔끔하게 출력해.', '적응형 테스트 생성'),
('TEST_FEEDBACK', '주제: %s\n학생 답안: %s\n위 답안을 채점(100점 만점)하고, 틀린 부분이 있다면 친절하게 설명해줘. 마지막에 "점수: XX" 형식을 꼭 포함해.', '테스트 피드백');

-- 3. 펫 스킬 초기화 (Mapper 로직 지원용)
INSERT IGNORE INTO `pet_skills` (`pet_type`, `skill_name`, `skill_code`, `effect_value`, `description`) VALUES
('TIGER', '맹수의 집중', 'POINT_BOOST', 1.2, '포인트 획득량 20% 증가'),
('RABBIT', '신속한 학습', 'EXP_BOOST', 1.1, '경험치 획득량 10% 증가'),
('TURTLE', '꾸준한 인내', 'STREAK_PROTECT', 1.0, '스트릭 끊김 1회 방지'),
('KANGAROO', '폭발적 에너지', 'ENERGY_REDUCE', 0.8, '활동 에너지 소모 20% 감소'),
('EASTERN_DRAGON', '용의 지혜', 'ALL_BOOST', 1.15, '모든 보상 15% 증가');

SET FOREIGN_KEY_CHECKS = 1; -- 외래키 제약 복구