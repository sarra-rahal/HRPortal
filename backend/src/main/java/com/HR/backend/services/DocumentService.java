package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.Document;
import com.HR.backend.models.Employee;
import com.HR.backend.repositories.DocumentRepository;
import com.HR.backend.repositories.DepartmentRepository;
import com.HR.backend.repositories.EmployeeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;
    private static final String DOCUMENTS_DIR = "documents/";
    private static final String REPORTS_DIR = "src/main/resources/reports/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Document uploadDocument(MultipartFile file, String accessibleBy, String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        validateFile(file);
        validateAccessibleBy(accessibleBy);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String filePath = DOCUMENTS_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(DOCUMENTS_DIR));
            file.transferTo(new File(filePath));

            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setFilePath(filePath);
            document.setUploadedBy(email);
            document.setGenerated(false);
            document.setAccessibleBy(accessibleBy);
            document.setUploadDate(LocalDateTime.now());
            return documentRepository.save(document);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload document: " + e.getMessage());
        }
    }

    public Document generateDocument(String name, String content, String accessibleBy, String templateType, String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        if (name == null || name.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and content are required");
        }
        validateAccessibleBy(accessibleBy);

        String fileName = UUID.randomUUID() + "_" + name.trim() + ".pdf";
        String filePath = DOCUMENTS_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(DOCUMENTS_DIR));
            String templatePath;
            switch (templateType.toLowerCase()) {
                case "work_certificate":
                    templatePath = REPORTS_DIR + "work_certificate.jrxml";
                    break;
                case "leave_certificate":
                    templatePath = REPORTS_DIR + "leave_certificate.jrxml";
                    break;
                default:
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template type");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", name);
            parameters.put("content", content);
            parameters.put("generatedBy", email);
            parameters.put("generatedDate", LocalDateTime.now().toString());

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(Collections.emptyList()));
            JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);

            Document document = new Document();
            document.setName(name.trim() + ".pdf");
            document.setFilePath(filePath);
            document.setUploadedBy(email);
            document.setGenerated(true);
            document.setAccessibleBy(accessibleBy);
            document.setUploadDate(LocalDateTime.now());
            return documentRepository.save(document);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate document: " + e.getMessage());
        }
    }

    public List<Document> getAccessibleDocuments(String token) {
        validateToken(token);
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        List<Document> allDocs = documentRepository.findAll();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        return allDocs.stream()
                .filter(doc -> isAccessible(doc.getAccessibleBy(), role, employee))
                .toList();
    }

    public Resource downloadDocument(Long id, String token) {
        validateToken(token);
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (!isAccessible(document.getAccessibleBy(), role, employee)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this document");
        }

        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found or unreadable");
            }
            return resource;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download document: " + e.getMessage());
        }
    }

    private void validateToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token format");
        }
        String jwt = token.split(" ")[1];
        if (!jwtUtil.isTokenValid(jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }
    }

    private void validateAccessibleBy(String accessibleBy) {
        if (accessibleBy == null || accessibleBy.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessibleBy is required");
        }
        try {
            Map<String, Object> json = objectMapper.readValue(accessibleBy, new TypeReference<Map<String, Object>>() {});
            String type = (String) json.get("type");
            Object value = json.get("value");
            if (!List.of("all_departments", "department", "employees").contains(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid accessibleBy type");
            }
            if ("department".equals(type)) {
                if (!(value instanceof String) || !departmentRepository.existsByName((String) value)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unknown department");
                }
            } else if ("employees".equals(type)) {
                if (!(value instanceof List<?>)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employees must be a list of CINs");
                }
                List<?> rawCins = (List<?>) value;
                List<String> cins = rawCins.stream()
                        .map(Object::toString)
                        .toList();
                for (String cin : cins) {
                    if (!employeeRepository.existsById(cin)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CIN: " + cin);
                    }
                }
            } else if ("all_departments".equals(type) && value != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All departments should have no value");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid accessibleBy format: " + e.getMessage());
        }
    }

    private boolean isAccessible(String accessibleBy, String role, Employee employee) {
        if (accessibleBy == null || accessibleBy.trim().isEmpty()) {
            return false;
        }

        try {
            Map<String, Object> json = objectMapper.readValue(accessibleBy, new TypeReference<Map<String, Object>>() {});
            String type = (String) json.get("type");
            Object value = json.get("value");
            String userDepartment = employee.getDepartement();
            String userCin = employee.getCin();

            if ("HR_MANAGER".equals(role)) {
                return true; // HR_MANAGER can access all documents
            }

            if (type == null) {
                return false;
            }

            switch (type) {
                case "all_departments":
                    return true;
                case "department":
                    return userDepartment != null && userDepartment.equals(value);
                case "employees":
                    if (!(value instanceof List<?>)) {
                        return false;
                    }
                    List<?> rawCins = (List<?>) value;
                    List<String> cins = rawCins.stream().map(Object::toString).toList();
                    return cins.contains(userCin);
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
