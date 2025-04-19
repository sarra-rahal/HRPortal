package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.Document;
import com.HR.backend.models.DocumentRequest;
import com.HR.backend.models.Employee;
import com.HR.backend.models.Notification;
import com.HR.backend.repositories.DocumentRepository;
import com.HR.backend.repositories.DocumentRequestRepository;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final DocumentRequestRepository documentRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
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
            Document savedDocument = documentRepository.save(document);

            notifyRecipients(accessibleBy, savedDocument, "New document uploaded: " + document.getName());
            return savedDocument;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload document: " + e.getMessage());
        }
    }

    public Document generateDocument(String name, Map<String, Object> parameters, String accessibleBy, String templateType, String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document name is required");
        }
        validateAccessibleBy(accessibleBy);

        String cin = (String) parameters.get("cin");
        Employee employee = employeeRepository.findById(cin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee with CIN " + cin + " not found"));

        String fileName = UUID.randomUUID() + "_" + name.trim() + ".pdf";
        String filePath = DOCUMENTS_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(DOCUMENTS_DIR));
            String templatePath;
            switch (templateType.toLowerCase()) {
                case "work_certificate":
                    templatePath = REPORTS_DIR + "work_certificate.jrxml";
                    parameters.put("employeeName", employee.getFirstname() + " " + employee.getLastname());
                    parameters.put("jobTitle", employee.getJobtitle());
                    parameters.put("department", employee.getDepartement());
                    break;
                case "leave_certificate":
                    templatePath = REPORTS_DIR + "leave_certificate.jrxml";
                    parameters.put("employeeName", employee.getFirstname() + " " + employee.getLastname());
                    parameters.put("leaveType", parameters.getOrDefault("leaveType", "Annual Leave"));
                    parameters.put("startDate", parameters.getOrDefault("startDate", LocalDateTime.now().toString()));
                    parameters.put("endDate", parameters.getOrDefault("endDate", LocalDateTime.now().toString()));
                    break;
                default:
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template type");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
            parameters.put("name", name);
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
            Document savedDocument = documentRepository.save(document);

            notifyRecipients(accessibleBy, savedDocument, "New document generated: " + document.getName());
            return savedDocument;
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

    public DocumentRequest requestDocument(DocumentRequest request, String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        request.setEmployeeEmail(email);
        request.setRequestDate(LocalDateTime.now());
        DocumentRequest savedRequest = documentRequestRepository.save(request);

        String message = "New document request from " + email + ": " + request.getType();
        Notification notification = new Notification();
        notification.setUserEmail("hr@example.com");
        notification.setMessage(message);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/hr@example.com", notification);
        emailService.sendEmail("no-reply@hrportal.com", "hr@example.com", "New Document Request", message);

        return savedRequest;
    }

    public List<DocumentRequest> getMyRequests(String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        return documentRequestRepository.findByEmployeeEmailOrderByRequestDateDesc(email);
    }

    public List<DocumentRequest> getPendingRequests(String token) {
        validateToken(token);
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        if (!"HR_MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return documentRequestRepository.findByStatusOrderByRequestDateDesc("PENDING");
    }

    public DocumentRequest reviewRequest(Long id, String status, String token) {
        validateToken(token);
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        if (!"HR_MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        request.setStatus(status);
        DocumentRequest updatedRequest = documentRequestRepository.save(request);

        String message = "Your document request for " + request.getType() + " has been " + status.toLowerCase();
        Notification notification = new Notification();
        notification.setUserEmail(request.getEmployeeEmail());
        notification.setMessage(message);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + request.getEmployeeEmail(), notification);
        emailService.sendEmail("no-reply@hrportal.com", request.getEmployeeEmail(), "Document Request Update", message);

        return updatedRequest;
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
            if ("employees".equals(type)) {
                if (!(value instanceof List<?>)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employees must be a list of CINs");
                }
                List<?> rawCins = (List<?>) value;
                List<String> cins = rawCins.stream().map(Object::toString).toList();
                for (String cin : cins) {
                    if (!employeeRepository.existsById(cin)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CIN: " + cin);
                    }
                }
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
            String userCin = employee.getCin();

            if ("HR_MANAGER".equals(role)) {
                return true;
            }

            if (type == null) {
                return false;
            }

            if ("employees".equals(type)) {
                if (!(value instanceof List<?>)) {
                    return false;
                }
                List<?> rawCins = (List<?>) value;
                List<String> cins = rawCins.stream().map(Object::toString).toList();
                return cins.contains(userCin);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void cancelRequest(Long id, String token) {
        validateToken(token);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!request.getEmployeeEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own requests");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be cancelled");
        }
        documentRequestRepository.delete(request);

        String message = "Document request cancelled by " + email + ": " + request.getType();
        Notification notification = new Notification();
        notification.setUserEmail("hr@example.com");
        notification.setMessage(message);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/hr@example.com", notification);
        emailService.sendEmail("no-reply@hrportal.com", "hr@example.com", "Document Request Cancelled", message);
    }

    private void notifyRecipients(String accessibleBy, Document document, String message) {
        try {
            Map<String, Object> json = objectMapper.readValue(accessibleBy, new TypeReference<Map<String, Object>>() {});
            String type = (String) json.get("type");
            Object value = json.get("value");

            List<Employee> recipients = new ArrayList<>();
            if ("employees".equals(type)) {
                List<?> rawCins = (List<?>) value;
                List<String> cins = rawCins.stream().map(Object::toString).toList();
                recipients.addAll(employeeRepository.findAllById(cins));
            }

            for (Employee emp : recipients) {
                Notification notification = new Notification();
                notification.setUserEmail(emp.getEmail());
                notification.setMessage(message);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSend("/topic/notifications/" + emp.getEmail(), notification);
                emailService.sendEmail("no-reply@hrportal.com", emp.getEmail(), "New Document Available", message);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send notifications: " + e.getMessage());
        }
    }
}