ALTER TABLE projects
    MODIFY COLUMN source_file_hash VARCHAR(64) NULL;

ALTER TABLE excel_import_jobs
    MODIFY COLUMN file_hash VARCHAR(64) NOT NULL;

ALTER TABLE project_embeddings
    MODIFY COLUMN text_hash VARCHAR(64) NOT NULL;
