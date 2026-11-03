CREATE TABLE evaluation_rubrics (
    rubric_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rubric_name VARCHAR(150) NOT NULL,
    submission_type ENUM('PDF', 'ZIP') NOT NULL,
    description TEXT NOT NULL,
    total_marks DECIMAL(5,2) NOT NULL,
    `version` INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_evaluation_rubric_type_version UNIQUE (submission_type, `version`),
    INDEX ix_evaluation_rubric_active (submission_type, is_active)
);

CREATE TABLE rubric_criteria (
    criterion_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rubric_id INT NOT NULL,
    criterion_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES evaluation_rubrics(rubric_id),
    CONSTRAINT uq_rubric_criterion_name UNIQUE (rubric_id, criterion_name),
    CONSTRAINT ck_rubric_criterion_marks CHECK (max_marks >= 0),
    INDEX ix_rubric_criteria_order (rubric_id, display_order)
);

CREATE TABLE rubric_subcriteria (
    subcriterion_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    criterion_id INT NOT NULL,
    subcriterion_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rubric_subcriteria_criterion FOREIGN KEY (criterion_id) REFERENCES rubric_criteria(criterion_id),
    CONSTRAINT uq_rubric_subcriterion_name UNIQUE (criterion_id, subcriterion_name),
    CONSTRAINT ck_rubric_subcriterion_marks CHECK (max_marks >= 0),
    INDEX ix_rubric_subcriteria_order (criterion_id, display_order)
);

CREATE TABLE project_review_stages (
    review_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_name VARCHAR(50) NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL,
    CONSTRAINT uq_project_review_stage_name UNIQUE (review_name),
    CONSTRAINT ck_project_review_stage_marks CHECK (max_marks > 0)
);

CREATE TABLE project_reviews (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    review_id INT NOT NULL,
    faculty_id VARCHAR(36) NOT NULL,
    marks_obtained DECIMAL(5,2) NOT NULL,
    feedback TEXT NULL,
    review_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_project_reviews_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_reviews_stage FOREIGN KEY (review_id) REFERENCES project_review_stages(review_id),
    CONSTRAINT fk_project_reviews_faculty FOREIGN KEY (faculty_id) REFERENCES users(user_id),
    CONSTRAINT uq_project_review_project_stage UNIQUE (project_id, review_id),
    CONSTRAINT ck_project_review_marks CHECK (marks_obtained >= 0),
    INDEX ix_project_reviews_faculty (faculty_id)
);

CREATE TABLE project_submissions (
    submission_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    submission_type ENUM('PDF', 'ZIP') NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    submission_status ENUM('SUBMITTED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'SUBMITTED',
    submission_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_project_submissions_project FOREIGN KEY (project_id) REFERENCES projects(id),
    INDEX ix_project_submissions_project_date (project_id, submission_date)
);

CREATE TABLE ai_evaluations (
    evaluation_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    submission_id INT NOT NULL,
    rubric_id INT NOT NULL,
    total_marks DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    overall_feedback TEXT NULL,
    evaluation_status ENUM('PENDING', 'COMPLETED', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    evaluated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_by VARCHAR(36) NULL,
    approved_at TIMESTAMP(6) NULL,
    decision_feedback TEXT NULL,
    decision_by VARCHAR(36) NULL,
    decision_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_ai_evaluations_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_ai_evaluations_submission FOREIGN KEY (submission_id) REFERENCES project_submissions(submission_id),
    CONSTRAINT fk_ai_evaluations_rubric FOREIGN KEY (rubric_id) REFERENCES evaluation_rubrics(rubric_id),
    CONSTRAINT fk_ai_evaluations_approved_by FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT fk_ai_evaluations_decision_by FOREIGN KEY (decision_by) REFERENCES users(user_id),
    CONSTRAINT ck_ai_evaluation_total_marks CHECK (total_marks >= 0),
    INDEX ix_ai_evaluations_project_date (project_id, evaluated_at),
    INDEX ix_ai_evaluations_submission_rubric (submission_id, rubric_id)
);

CREATE TABLE ai_evaluation_details (
    detail_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NOT NULL,
    criterion_id INT NOT NULL,
    awarded_marks DECIMAL(5,2) NOT NULL,
    justification TEXT NOT NULL,
    improvement_suggestions TEXT NULL,
    CONSTRAINT fk_ai_evaluation_details_evaluation FOREIGN KEY (evaluation_id) REFERENCES ai_evaluations(evaluation_id),
    CONSTRAINT fk_ai_evaluation_details_criterion FOREIGN KEY (criterion_id) REFERENCES rubric_criteria(criterion_id),
    CONSTRAINT uq_ai_evaluation_criterion UNIQUE (evaluation_id, criterion_id),
    CONSTRAINT ck_ai_evaluation_awarded_marks CHECK (awarded_marks >= 0),
    INDEX ix_ai_evaluation_details_criterion (criterion_id)
);

INSERT INTO project_review_stages (review_name, max_marks) VALUES
    ('Project Review 1', 20.00),
    ('Project Review 2', 30.00),
    ('Project Review 3', 50.00)
ON DUPLICATE KEY UPDATE review_name = VALUES(review_name);

INSERT INTO evaluation_rubrics (rubric_name, submission_type, description, total_marks, `version`, is_active) VALUES
    ('PDF Report Evaluation', 'PDF', 'Evaluation rubric for the academic report submitted as a PDF.', 100.00, 1, TRUE),
    ('ZIP Project Evaluation', 'ZIP', 'Evaluation rubric for the project source archive submitted as a ZIP.', 100.00, 1, TRUE)
ON DUPLICATE KEY UPDATE rubric_name = VALUES(rubric_name);

INSERT INTO rubric_criteria (rubric_id, criterion_name, description, max_marks, display_order)
SELECT r.rubric_id, c.criterion_name, c.description, c.max_marks, c.display_order
FROM evaluation_rubrics r
JOIN (
    SELECT 'PDF' AS submission_type, 'Problem Statement and Objectives' AS criterion_name, 'Assesses whether the project problem is clearly scoped and the objectives are specific, relevant, and measurable.' AS description, 10.00 AS max_marks, 1 AS display_order
    UNION ALL SELECT 'PDF', 'Literature Survey and Research Gap', 'Assesses coverage and synthesis of relevant prior work, citation quality, and whether a meaningful research or engineering gap is identified.', 15.00, 2
    UNION ALL SELECT 'PDF', 'Methodology and System Design', 'Assesses the suitability and clarity of the methods, architecture, design decisions, workflows, and technical rationale.', 15.00, 3
    UNION ALL SELECT 'PDF', 'Implementation Description', 'Assesses how clearly the report explains implemented components, algorithms, technologies, and important engineering decisions.', 15.00, 4
    UNION ALL SELECT 'PDF', 'Results and Testing Documentation', 'Assesses the evidence for implementation correctness, test coverage, measured results, and interpretation of outcomes.', 15.00, 5
    UNION ALL SELECT 'PDF', 'Innovation and Uniqueness', 'Assesses the originality of the approach and the distinct value of the project relative to existing solutions.', 10.00, 6
    UNION ALL SELECT 'PDF', 'Documentation Quality and Structure', 'Assesses organization, readability, completeness, figure and table use, and consistency of academic presentation.', 10.00, 7
    UNION ALL SELECT 'PDF', 'References and Citations', 'Assesses relevance, credibility, completeness, and consistent formatting of citations and references.', 5.00, 8
    UNION ALL SELECT 'PDF', 'Abstract and Project Overview', 'Assesses whether the abstract concisely communicates the problem, approach, main results, and project contribution.', 5.00, 9
    UNION ALL SELECT 'ZIP', 'Problem Understanding and Requirements', 'Assesses whether the implementation addresses a coherent problem and translates requirements into observable behavior.', 10.00, 1
    UNION ALL SELECT 'ZIP', 'System Architecture and Design', 'Assesses the architecture, component boundaries, data flow, interfaces, and appropriateness of design choices.', 10.00, 2
    UNION ALL SELECT 'ZIP', 'Functionality and Implementation', 'Assesses the completeness and correctness of implemented features against the stated project purpose.', 20.00, 3
    UNION ALL SELECT 'ZIP', 'Code Quality and Coding Standards', 'Assesses readability, naming, consistency, maintainability, error handling, and adherence to language conventions.', 15.00, 4
    UNION ALL SELECT 'ZIP', 'Modularity and Project Structure', 'Assesses separation of concerns, reuse, dependency organization, and clarity of the repository layout.', 10.00, 5
    UNION ALL SELECT 'ZIP', 'Testing and Validation', 'Assesses the presence and quality of tests, validation paths, and evidence that important behavior is verified.', 15.00, 6
    UNION ALL SELECT 'ZIP', 'Security and Error Handling', 'Assesses input validation, secret handling, access control, failure handling, and secure defaults visible in the source.', 5.00, 7
    UNION ALL SELECT 'ZIP', 'Documentation and Readability', 'Assesses setup instructions, API or usage documentation, comments, and the clarity of project materials.', 5.00, 8
    UNION ALL SELECT 'ZIP', 'Innovation and Uniqueness', 'Assesses original technical choices or features and the practical value they add beyond a routine implementation.', 10.00, 9
) c ON c.submission_type = r.submission_type
WHERE r.`version` = 1
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    max_marks = VALUES(max_marks),
    display_order = VALUES(display_order);
