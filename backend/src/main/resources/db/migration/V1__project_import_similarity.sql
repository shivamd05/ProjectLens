CREATE TABLE projects (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500) NOT NULL,
    description LONGTEXT NULL,
    problem_statement LONGTEXT NULL,
    objectives LONGTEXT NULL,
    technologies TEXT NULL,
    team_name VARCHAR(255) NULL,
    team_leader VARCHAR(255) NULL,
    group_number VARCHAR(50) NULL,
    faculty_coordinator VARCHAR(255) NULL,
    department VARCHAR(255) NULL,
    academic_year VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_file_name VARCHAR(255) NULL,
    source_file_hash CHAR(64) NULL,
    source_sheet VARCHAR(100) NULL,
    source_row INT NULL,
    api_payload_json LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_projects_title_year UNIQUE (normalized_title, academic_year),
    CONSTRAINT uq_projects_source_row UNIQUE (source_file_hash, source_sheet, source_row)
);

CREATE TABLE project_members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    student_id VARCHAR(100) NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    source_row INT NULL,
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_member UNIQUE (project_id, student_id)
);

CREATE TABLE excel_import_jobs (
    job_id VARCHAR(36) NOT NULL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_records INT NOT NULL DEFAULT 0,
    valid_records INT NOT NULL DEFAULT 0,
    invalid_records INT NOT NULL DEFAULT 0,
    duplicate_records INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    error_message TEXT NULL,
    workbook_warnings_json LONGTEXT NULL,
    INDEX ix_import_job_hash (file_hash)
);

CREATE TABLE excel_import_records (
    record_id VARCHAR(36) NOT NULL PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    sheet_name VARCHAR(100) NOT NULL,
    source_row INT NOT NULL,
    record_json LONGTEXT NOT NULL,
    validation_json LONGTEXT NOT NULL,
    warnings_json LONGTEXT NOT NULL,
    record_status VARCHAR(32) NOT NULL,
    imported_project_id VARCHAR(64) NULL,
    CONSTRAINT fk_import_records_job FOREIGN KEY (job_id) REFERENCES excel_import_jobs(job_id) ON DELETE CASCADE,
    CONSTRAINT fk_import_records_project FOREIGN KEY (imported_project_id) REFERENCES projects(id),
    CONSTRAINT uq_import_job_source_row UNIQUE (job_id, sheet_name, source_row),
    INDEX ix_import_records_status (job_id, record_status)
);

CREATE TABLE project_embeddings (
    project_id VARCHAR(64) NOT NULL PRIMARY KEY,
    text_hash CHAR(64) NOT NULL,
    model_id VARCHAR(128) NOT NULL,
    vector_json LONGTEXT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_project_embedding_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    INDEX ix_project_embedding_status (status)
);

CREATE TABLE similarity_checks (
    check_id VARCHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description LONGTEXT NULL,
    objectives LONGTEXT NULL,
    technologies TEXT NULL,
    status VARCHAR(24) NOT NULL,
    threshold_value DOUBLE NOT NULL,
    model_id VARCHAR(128) NOT NULL,
    results_json LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
