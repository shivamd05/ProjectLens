package com.projectlense.api.service;

import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.entity.ProjectSubmissionEntity;
import com.projectlense.api.entity.SubmissionStatus;
import com.projectlense.api.entity.SubmissionType;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.repository.ProjectSubmissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ProjectSubmissionService {
    private static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    private final ProjectSubmissionRepository submissions;
    private final ProjectAccessService access;
    private final Path uploadDir;

    public ProjectSubmissionService(ProjectSubmissionRepository submissions, ProjectAccessService access,
                                    @Value("${projectlense.upload-dir}") String uploadDir) {
        this.submissions = submissions;
        this.access = access;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public Map<String, Object> submit(String projectId, MultipartFile file, UserEntity user) {
        ProjectEntity project = access.requireStudentOwnerOrAdmin(projectId, user);
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty PDF or ZIP file");
        if (file.getSize() > MAX_FILE_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Submission exceeds the 100 MB limit");

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("").replace('\\', '/');
        String baseName = originalName.substring(originalName.lastIndexOf('/') + 1);
        String safeName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.length() > 255) safeName = safeName.substring(safeName.length() - 255);
        SubmissionType type = typeFromFileName(safeName);
        validateSignature(file, type);
        if ("approved".equalsIgnoreCase(project.getStatus()) || "evaluated".equalsIgnoreCase(project.getStatus())) {
            project.setStatus("under_review");
        }

        String storedName = UUID.randomUUID() + "_" + safeName;
        Path submissionsDir = uploadDir.resolve("submissions").normalize();
        Path storedFile = submissionsDir.resolve(storedName).normalize();
        if (!storedFile.startsWith(submissionsDir)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        try {
            Files.createDirectories(submissionsDir);
            file.transferTo(storedFile.toFile());
            String relativePath = uploadDir.relativize(storedFile).toString().replace('\\', '/');
            ProjectSubmissionEntity entity = new ProjectSubmissionEntity();
            entity.setProject(project); entity.setSubmissionType(type); entity.setFileName(safeName);
            entity.setFilePath(relativePath); entity.setStatus(SubmissionStatus.SUBMITTED);
            return toMap(submissions.saveAndFlush(entity));
        } catch (Exception e) {
            try { Files.deleteIfExists(storedFile); } catch (IOException ignored) {}
            if (e instanceof ResponseStatusException status) throw status;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Submission could not be stored");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String projectId, UserEntity user) {
        access.requireView(projectId, user);
        return submissions.findByProject_IdOrderBySubmissionDateDesc(projectId).stream().map(ProjectSubmissionService::toMap).toList();
    }

    @Transactional(readOnly = true)
    public ProjectSubmissionEntity latest(String projectId) {
        return submissions.findFirstByProject_IdOrderBySubmissionDateDesc(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "A final project submission is required before evaluation"));
    }

    public Path resolveStoredFile(ProjectSubmissionEntity submission) {
        Path path = uploadDir.resolve(submission.getFilePath()).normalize();
        if (!path.startsWith(uploadDir) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored submission file is missing");
        }
        return path;
    }

    private static SubmissionType typeFromFileName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return SubmissionType.PDF;
        if (lower.endsWith(".zip")) return SubmissionType.ZIP;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF and ZIP submissions are supported");
    }

    private static void validateSignature(MultipartFile file, SubmissionType type) {
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if (type == SubmissionType.PDF) {
                if (header.length < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match a PDF");
                }
            } else {
                boolean validZip = header.length >= 4 && header[0] == 'P' && header[1] == 'K'
                        && ((header[2] == 3 && header[3] == 4) || (header[2] == 5 && header[3] == 6)
                        || (header[2] == 7 && header[3] == 8));
                if (!validZip) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match a ZIP archive");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission file could not be read");
        }
    }

    private static Map<String, Object> toMap(ProjectSubmissionEntity item) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", item.getId()); out.put("projectId", item.getProject().getId());
        out.put("submissionType", item.getSubmissionType()); out.put("fileName", item.getFileName());
        out.put("status", item.getStatus()); out.put("submissionDate", item.getSubmissionDate());
        return out;
    }
}
