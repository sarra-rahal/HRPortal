package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.*;
import com.HR.backend.repositories.*;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final String DOCUMENTS_DIR = "documents/";
    private static final String REPORTS_DIR = "src/main/resources/reports/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public Document upload (MultipartFile file, String access, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        User user= userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        validateFile(file);
        List<String> accessibleBy =validateAndTransformAccess(access);
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String filePath = DOCUMENTS_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(DOCUMENTS_DIR));
            file.transferTo(new File(filePath));

            Document document = new Document();
            document.setName(file.getOriginalFilename());
            document.setFilePath(filePath);
            document.setUploadedBy(employee.getFirstname() + " " + employee.getLastname());
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

    public List<String> validateAndTransformAccess(String accessJson) {
        // Check for null or empty input
        if (accessJson == null || accessJson.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access JSON is required");
        }
        try {
            // Parse JSON to Map<String, List<String>>
            Map<String, List<String>> access = objectMapper.readValue(
                    accessJson,
                    new TypeReference<Map<String, List<String>>>() {}
            );

            // Validate required keys
            if (!access.containsKey("dep") || !access.containsKey("emp")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON must contain 'dep' and 'emp' keys");
            }

            List<String> dep = access.get("dep");
            List<String> empCins = access.get("emp"); // Renamed to avoid confusion

            // Validate dep
            if (dep == null || dep.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'dep' array cannot be empty");
            }
            for (String depValue : dep) {
                if (depValue == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'dep' contains null values");
                }
                if (!depValue.equals("-1") && !depValue.equals("-2")) {
                    try {
                        int depId = Integer.parseInt(depValue);
                        if (!departmentRepository.existsById((long) depId)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid department ID: " + depValue);
                        }
                    } catch (NumberFormatException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid department ID format: " + depValue);
                    }
                }
            }

            // Validate emp

            for (String cin : empCins) {
                if (cin == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'emp' contains null values");
                }
                if (!cin.isEmpty() && !employeeRepository.existsById(cin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CIN: " + cin);
                }
            }

            // Special rule: if dep contains "-2", emp must have at least one CIN
            if (dep.contains("-2") && (empCins.isEmpty() || empCins.stream().allMatch(String::isEmpty))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "When dep is -2, emp must contain at least one CIN");
            }

            // Transform to list of unique CINs
            Set<String> resultCins = new HashSet<>(empCins.stream().filter(cin -> !cin.isEmpty()).collect(Collectors.toList()));

            // Process based on dep[0]
            String firstDep = dep.get(0);
            if (firstDep.equals("-2")) {
                // Only use emp CINs
                return new ArrayList<>(resultCins);
            } else if (firstDep.equals("-1")) {
                // Add all employees from all departments
                List<Employee> allEmployees = employeeRepository.findAll();
                allEmployees.forEach(employee -> resultCins.add(employee.getCin())); // Changed 'emp' to 'employee'
            } else {
                // Add employees from the specified department
                int depId = Integer.parseInt(firstDep);
                List<Employee> deptEmployees = employeeRepository.findByDepartmentId(depId);
                deptEmployees.forEach(employee -> resultCins.add(employee.getCin())); // Changed 'emp' to 'employee'
            }

            return new ArrayList<>(resultCins);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid access JSON format: " + e.getMessage());
        }
    }

    public List<Document> getAccessibleDocuments(String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        List<Document> allDocs = documentRepository.findAll();
        User user= userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        return allDocs.stream()
                .filter(doc -> isAccessible(doc, employee))
                .toList();
    }

    public Resource downloadDocument(Long id, String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!isAccessible(document, employee)) {
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
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        request.setEmployeeEmail(email);
        request.setRequestDate(LocalDateTime.now());
        DocumentRequest savedRequest = documentRequestRepository.save(request);

        String message = "New document request from " + email + ": " + request.getType();
        messagingTemplate.convertAndSend("/topic/notifications/"+ email, message);
        emailService.sendEmail("no-reply@hrportal.com", "rahalsarra2@gmail.com", "New Document Request", message);

        return savedRequest;
    }

    public List<DocumentRequest> getMyRequests(String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        return documentRequestRepository.findByEmployeeEmailOrderByRequestDateDesc(email);
    }

    public List<DocumentRequest> getPendingRequests(String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        if (!"HR_MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return documentRequestRepository.findByStatusInOrderByRequestDateDesc(List.of("PENDING"));
    }

    public DocumentRequest reviewRequest(Long id, String status, String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        if (!"HR_MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        request.setStatus(status);
        DocumentRequest updatedRequest = documentRequestRepository.save(request);

        String senderEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        String message = "Your document for " + request.getType() + "is" + status.toLowerCase();
        emailService.sendEmail(senderEmail, request.getEmployeeEmail(), "Document Request Update", message);
        emailService.notifyUser(request.getEmployeeEmail(), message);

        return updatedRequest;
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

    public boolean isAccessible(Document document, Employee employee) {
        if (document == null || employee == null) {
            throw new IllegalArgumentException("Document and employee must not be null");
        }
        if (document.getAccessibleBy() == null || employee.getCin() == null) {
            return false;
        }
        return document.getAccessibleBy().contains(employee.getCin());
    }

    public void cancelRequest(Long id, String token) {
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
        emailService.sendEmail("sarrarahal2004@gmail.com", email, "Document Request Cancelled " , message);
        emailService.notifyUser(email, message);
        messagingTemplate.convertAndSend("/topic/notifications/" + email , message);
    }

    public void notifyRecipients(List<String> accessibleBy, Document document, String message) {
        if (accessibleBy == null || document == null || message == null) {
            throw new IllegalArgumentException("accessibleBy, document, and message must not be null");
        }

        for (String cin : accessibleBy) {
            try {
                User user = userRepository.findByCin(cin)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for CIN: " + cin));
                String email = user.getEmail();
                if (email != null && !email.isEmpty()) {
                    emailService.sendEmail("sarrarahal2004@gmail.com", email, "New Document Available: " + document.getName(), message);
                    emailService.notifyUser(email, message);
                }
            } catch (Exception e) {
                // Log error but continue with other recipients
                System.err.println("Failed to notify employee with CIN " + cin + ": " + e.getMessage());
            }
        }
    }
}