package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.*;
import com.HR.backend.repositories.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public Document upload(MultipartFile file, String access, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        validateFile(file);
        List<String> accessibleBy = validateAndTransformAccess(access);
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
        if (accessJson == null || accessJson.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access JSON is required");
        }
        try {
            Map<String, List<String>> access = objectMapper.readValue(
                    accessJson,
                    new TypeReference<Map<String, List<String>>>() {}
            );

            // Validate required keys
            if (!access.containsKey("emp") || !access.containsKey("dep") || !access.containsKey("job") || !access.containsKey("all")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON must contain 'emp', 'dep', 'job', and 'all' keys");
            }

            List<String> empCins = access.get("emp") != null ? access.get("emp").stream().filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();
            List<String> depNames = access.get("dep") != null ? access.get("dep").stream().filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();
            List<String> jobTitles = access.get("job") != null ? access.get("job").stream().filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();
            List<String> allFlag = access.get("all") != null ? access.get("all").stream().filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();

            // Validate emp CINs
            for (String cin : empCins) {
                if (!cin.isEmpty() && !employeeRepository.existsById(cin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CIN: " + cin);
                }
            }

            // Map department names to IDs
            List<Long> depIds = new ArrayList<>();
            for (String depName : depNames) {
                if (depName.equals("-1") || depName.equals("-2")) {
                    depIds.add(Long.parseLong(depName));
                } else {
                    Department department = departmentRepository.findByName(depName)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid department name: " + depName));
                    depIds.add(department.getId());
                }
            }

            // Validate job titles (remove if Employee model lacks jobTitle)
            for (String jobTitle : jobTitles) {
                if (!jobTitle.isEmpty() && !employeeRepository.existsByJobtitle(jobTitle)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job title: " + jobTitle);
                }
            }

            // Validate all flag
            if (!allFlag.isEmpty() && !allFlag.contains("all")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'all' must contain only 'all' or be empty");
            }

            // Transform to list of unique CINs
            Set<String> resultCins = new HashSet<>(empCins.stream().filter(cin -> !cin.isEmpty()).collect(Collectors.toList()));

            // Process based on access
            if (!allFlag.isEmpty() && allFlag.contains("all")) {
                // Add all employees
                List<Employee> allEmployees = employeeRepository.findAll();
                allEmployees.forEach(emp -> resultCins.add(emp.getCin()));
            } else if (!depIds.isEmpty()) {
                Long firstDep = depIds.get(0);
                if (firstDep == -2) {
                    // Only use emp CINs
                    return new ArrayList<>(resultCins);
                } else if (firstDep == -1) {
                    // Add all employees
                    List<Employee> allEmployees = employeeRepository.findAll();
                    allEmployees.forEach(emp -> resultCins.add(emp.getCin()));
                } else {
                    // Add employees from specified departments
                    for (Long depId : depIds) {
                        List<Employee> deptEmployees = employeeRepository.findByDepartmentId(depId);
                        deptEmployees.forEach(emp -> resultCins.add(emp.getCin()));
                    }
                }
            }

            // Add employees by job titles (remove if Employee model lacks jobTitle)
            for (String jobTitle : jobTitles) {
                if (!jobTitle.isEmpty()) {
                    List<Employee> jobEmployees = employeeRepository.findByJobtitle(jobTitle);
                    jobEmployees.forEach(emp -> resultCins.add(emp.getCin()));
                }
            }

            // Ensure HR department is included for uploads targeting HR
            if (depNames.contains("HR")) {
                Department hrDept = departmentRepository.findByName("HR")
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "HR department not found"));
                List<Employee> hrEmployees = employeeRepository.findByDepartmentId(hrDept.getId());
                hrEmployees.forEach(emp -> resultCins.add(emp.getCin()));
            }

            if (resultCins.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid recipients specified in access");
            }

            return new ArrayList<>(resultCins);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid access JSON format: " + e.getMessage());
        }
    }

    public List<Document> getAccessibleDocuments(String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        return documentRepository.findAll().stream()
                .filter(doc -> isAccessible(doc, employee))
                .toList();
    }

    public Resource downloadDocument(Long id, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

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
        messagingTemplate.convertAndSend("/topic/notifications/" + email, message);
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
        String message = "Your document request for " + request.getType() + " is " + status.toLowerCase();
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
        emailService.sendEmail("no-reply@hrportal.com", email, "Document Request Cancelled", message);
        emailService.notifyUser(email, message);
        messagingTemplate.convertAndSend("/topic/notifications/" + email, message);
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
                    emailService.sendEmail("no-reply@hrportal.com", email, "New Document Available: " + document.getName(), message);
                    messagingTemplate.convertAndSend("/topic/notifications/" + email, message); // Changed to send string
                }
            } catch (Exception e) {
                System.err.println("Failed to notify employee with CIN " + cin + ": " + e.getMessage());
            }
        }
    }
}