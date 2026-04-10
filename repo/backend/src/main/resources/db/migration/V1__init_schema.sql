-- ScholarOps Database Schema
-- All tables for offline learning & content intake system

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_permissions_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    workstation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permission_change_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    role_id BIGINT,
    permission_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE SET NULL,
    INDEX idx_perm_change_target (target_user_id),
    INDEX idx_perm_change_by (changed_by_user_id),
    INDEX idx_perm_change_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crawl_source_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    base_url VARCHAR(2000) NOT NULL,
    description TEXT,
    rate_limit_per_minute INT NOT NULL DEFAULT 30,
    requires_auth BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_crawl_source_enabled (enabled),
    INDEX idx_crawl_source_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE encrypted_source_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_profile_id BIGINT NOT NULL UNIQUE,
    encrypted_username VARBINARY(512),
    encrypted_password VARBINARY(512),
    encrypted_api_key VARBINARY(512),
    encryption_iv VARBINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (source_profile_id) REFERENCES crawl_source_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crawl_rule_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_profile_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    extraction_method VARCHAR(20) NOT NULL,
    rule_definition JSON NOT NULL,
    field_mappings JSON NOT NULL,
    type_validations JSON,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (source_profile_id) REFERENCES crawl_source_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_source_version (source_profile_id, version_number),
    INDEX idx_rule_source (source_profile_id),
    INDEX idx_rule_active (source_profile_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crawl_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_profile_id BIGINT NOT NULL,
    rule_version_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    total_pages INT NOT NULL DEFAULT 0,
    pages_crawled INT NOT NULL DEFAULT 0,
    pages_failed INT NOT NULL DEFAULT 0,
    items_extracted INT NOT NULL DEFAULT 0,
    error_log TEXT,
    initiated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_profile_id) REFERENCES crawl_source_profiles(id),
    FOREIGN KEY (rule_version_id) REFERENCES crawl_rule_versions(id),
    FOREIGN KEY (initiated_by) REFERENCES users(id),
    INDEX idx_crawl_run_status (status),
    INDEX idx_crawl_run_source (source_profile_id),
    INDEX idx_crawl_run_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE standardized_content_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crawl_run_id BIGINT,
    source_profile_id BIGINT NOT NULL,
    source_url VARCHAR(2000),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    body_text LONGTEXT,
    content_type VARCHAR(50),
    original_timestamp TIMESTAMP NULL,
    standardized_timestamp TIMESTAMP NULL,
    timezone_id VARCHAR(50),
    original_location VARCHAR(500),
    normalized_address VARCHAR(500),
    detected_language VARCHAR(10),
    price DECIMAL(10,2),
    availability_start DATE,
    availability_end DATE,
    popularity_score INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    published_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (crawl_run_id) REFERENCES crawl_runs(id) ON DELETE SET NULL,
    FOREIGN KEY (source_profile_id) REFERENCES crawl_source_profiles(id),
    FOREIGN KEY (published_by) REFERENCES users(id),
    INDEX idx_content_published (is_published),
    INDEX idx_content_type (content_type),
    INDEX idx_content_availability (availability_start, availability_end),
    INDEX idx_content_popularity (popularity_score),
    INDEX idx_content_created (created_at),
    FULLTEXT INDEX ft_content_search (title, description, body_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE media_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_record_id BIGINT NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    local_path VARCHAR(1000),
    width INT,
    height INT,
    duration_seconds INT,
    checksum VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (content_record_id) REFERENCES standardized_content_records(id) ON DELETE CASCADE,
    INDEX idx_media_content (content_record_id),
    INDEX idx_media_type (media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    day_of_week TINYINT,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    color VARCHAR(7),
    content_record_id BIGINT,
    quiz_paper_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_schedule_user (user_id),
    INDEX idx_schedule_time (start_time, end_time),
    INDEX idx_schedule_day (user_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedule_change_journal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    schedule_id BIGINT,
    change_type VARCHAR(30) NOT NULL,
    previous_state JSON,
    new_state JSON,
    is_undone BOOLEAN NOT NULL DEFAULT FALSE,
    sequence_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL,
    INDEX idx_journal_user (user_id),
    INDEX idx_journal_sequence (user_id, sequence_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE locked_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    day_of_week TINYINT,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_locked_user (user_id),
    INDEX idx_locked_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_banks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    subject VARCHAR(100),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_qbank_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_bank_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    difficulty_level TINYINT NOT NULL CHECK (difficulty_level BETWEEN 1 AND 5),
    question_text TEXT NOT NULL,
    options JSON,
    correct_answer TEXT,
    explanation TEXT,
    points DECIMAL(5,2) NOT NULL DEFAULT 1.0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (question_bank_id) REFERENCES question_banks(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_question_bank (question_bank_id),
    INDEX idx_question_difficulty (difficulty_level),
    INDEX idx_question_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_knowledge_tags (
    question_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES knowledge_tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_papers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    question_bank_id BIGINT NOT NULL,
    total_questions INT NOT NULL,
    total_points DECIMAL(7,2) NOT NULL,
    time_limit_minutes INT,
    max_attempts INT NOT NULL DEFAULT 1,
    release_start TIMESTAMP NULL,
    release_end TIMESTAMP NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    show_immediate_feedback BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (question_bank_id) REFERENCES question_banks(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_quiz_bank (question_bank_id),
    INDEX idx_quiz_published (is_published),
    INDEX idx_quiz_release (release_start, release_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_questions (
    quiz_paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    PRIMARY KEY (quiz_paper_id, question_id),
    FOREIGN KEY (quiz_paper_id) REFERENCES quiz_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_paper_id BIGINT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    min_count INT,
    max_count INT,
    difficulty_level TINYINT,
    tag_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_paper_id) REFERENCES quiz_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES knowledge_tags(id) ON DELETE SET NULL,
    INDEX idx_quiz_rule_paper (quiz_paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_paper_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    auto_saved_at TIMESTAMP NULL,
    time_remaining_seconds INT,
    total_score DECIMAL(7,2),
    max_score DECIMAL(7,2),
    percentage DECIMAL(5,2),
    fingerprint_hash VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_paper_id) REFERENCES quiz_papers(id),
    FOREIGN KEY (student_id) REFERENCES users(id),
    UNIQUE KEY uk_submission_attempt (quiz_paper_id, student_id, attempt_number),
    INDEX idx_submission_student (student_id),
    INDEX idx_submission_quiz (quiz_paper_id),
    INDEX idx_submission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE submission_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text TEXT,
    selected_option VARCHAR(10),
    is_correct BOOLEAN,
    score DECIMAL(5,2),
    auto_graded BOOLEAN NOT NULL DEFAULT FALSE,
    graded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    UNIQUE KEY uk_answer (submission_id, question_id),
    INDEX idx_answer_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grading_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_answer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_to BIGINT,
    graded_by BIGINT,
    score DECIMAL(5,2),
    feedback TEXT,
    graded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_answer_id) REFERENCES submission_answers(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (graded_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_grading_status (status),
    INDEX idx_grading_assigned (assigned_to),
    INDEX idx_grading_answer (submission_answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rubric_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grading_state_id BIGINT NOT NULL,
    criterion_name VARCHAR(200) NOT NULL,
    max_score DECIMAL(5,2) NOT NULL,
    awarded_score DECIMAL(5,2) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (grading_state_id) REFERENCES grading_states(id) ON DELETE CASCADE,
    INDEX idx_rubric_grading (grading_state_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_answer_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    student_answer TEXT,
    correct_answer TEXT,
    explanation TEXT,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (submission_id) REFERENCES submissions(id),
    INDEX idx_wrong_student (student_id),
    INDEX idx_wrong_question (question_id),
    INDEX idx_wrong_reviewed (student_id, reviewed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plagiarism_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    max_similarity_score DECIMAL(5,4),
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id),
    INDEX idx_plagiarism_submission (submission_id),
    INDEX idx_plagiarism_flagged (is_flagged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plagiarism_matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plagiarism_check_id BIGINT NOT NULL,
    matched_submission_id BIGINT,
    matched_content_id BIGINT,
    similarity_score DECIMAL(5,4) NOT NULL,
    matched_text_excerpt TEXT,
    source_text_excerpt TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plagiarism_check_id) REFERENCES plagiarism_checks(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_submission_id) REFERENCES submissions(id) ON DELETE SET NULL,
    FOREIGN KEY (matched_content_id) REFERENCES standardized_content_records(id) ON DELETE SET NULL,
    INDEX idx_match_check (plagiarism_check_id),
    INDEX idx_match_score (similarity_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed roles
INSERT INTO roles (name, description) VALUES
('ADMINISTRATOR', 'System administrator with full access'),
('CONTENT_CURATOR', 'Manages crawl sources, rules, and content intake'),
('INSTRUCTOR', 'Creates question banks, assembles quizzes, reviews grades'),
('TEACHING_ASSISTANT', 'Grades subjective items, assists instructors'),
('STUDENT', 'Takes assessments, manages timetable, browses catalog');

-- Seed permissions
INSERT INTO permissions (code, description, category) VALUES
('USER_MANAGE', 'Create, update, delete users', 'ADMIN'),
('ROLE_ASSIGN', 'Assign and revoke roles', 'ADMIN'),
('AUDIT_VIEW', 'View audit logs and permission history', 'ADMIN'),
('PASSWORD_ADMIN_RESET', 'Perform admin-assisted password resets', 'ADMIN'),
('CRAWL_SOURCE_MANAGE', 'Manage crawl source profiles', 'CRAWL'),
('CRAWL_RULE_MANAGE', 'Create, update, revert crawl rules', 'CRAWL'),
('CRAWL_RUN_MANAGE', 'Start and monitor crawl runs', 'CRAWL'),
('CONTENT_REVIEW', 'Review and publish standardized content', 'CONTENT'),
('CONTENT_VIEW', 'View published content catalog', 'CONTENT'),
('QUESTION_BANK_MANAGE', 'Create and manage question banks', 'ASSESSMENT'),
('QUIZ_MANAGE', 'Assemble and schedule quizzes', 'ASSESSMENT'),
('QUIZ_TAKE', 'Take published quizzes', 'ASSESSMENT'),
('SUBMISSION_VIEW_OWN', 'View own submissions', 'ASSESSMENT'),
('SUBMISSION_VIEW_ALL', 'View all submissions for assigned quizzes', 'ASSESSMENT'),
('GRADING_MANAGE', 'Grade subjective items', 'GRADING'),
('GRADING_VIEW', 'View grading states', 'GRADING'),
('SCHEDULE_MANAGE_OWN', 'Manage own timetable', 'SCHEDULE'),
('PLAGIARISM_VIEW', 'View plagiarism check results', 'ASSESSMENT'),
('WRONG_ANSWER_VIEW_OWN', 'View own wrong answer history', 'ASSESSMENT');

-- Assign permissions to roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMINISTRATOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CONTENT_CURATOR' AND p.code IN (
    'CRAWL_SOURCE_MANAGE', 'CRAWL_RULE_MANAGE', 'CRAWL_RUN_MANAGE', 'CONTENT_REVIEW', 'CONTENT_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'INSTRUCTOR' AND p.code IN (
    'QUESTION_BANK_MANAGE', 'QUIZ_MANAGE', 'SUBMISSION_VIEW_ALL', 'GRADING_MANAGE',
    'GRADING_VIEW', 'CONTENT_VIEW', 'PLAGIARISM_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TEACHING_ASSISTANT' AND p.code IN (
    'GRADING_MANAGE', 'GRADING_VIEW', 'SUBMISSION_VIEW_ALL', 'CONTENT_VIEW', 'PLAGIARISM_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'STUDENT' AND p.code IN (
    'CONTENT_VIEW', 'QUIZ_TAKE', 'SUBMISSION_VIEW_OWN', 'SCHEDULE_MANAGE_OWN',
    'WRONG_ANSWER_VIEW_OWN'
);

-- Seed default admin user (password: Admin@12345678)
INSERT INTO users (username, email, password_hash, full_name) VALUES
('admin', 'admin@scholarops.local', '$2a$12$LJ3m4yst.VHMEFBpJhkeAeJy7VCfLEMZTgHBf2ycXU6OqD0XMHJKa', 'System Administrator');

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, u.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMINISTRATOR';
