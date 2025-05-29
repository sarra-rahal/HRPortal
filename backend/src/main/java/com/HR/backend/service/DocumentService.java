package com.HR.backend.service;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.model.dto.DocumentDTO;
import com.HR.backend.model.dto.DocumentRequestDTO;
import com.HR.backend.model.entity.Document;
import com.HR.backend.model.entity.DocumentRequest;
import com.HR.backend.model.entity.Employee;
import com.HR.backend.model.entity.User;
import com.HR.backend.repository.*;
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
    private final DepartmentRepository departmentRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final String DOCUMENTS_DIR = "documents/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public DocumentDTO upload(MultipartFile file, String access, String token) {
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
            document = documentRepository.save(document);

            sendNotification(employee.getEmail(), "Document uploaded: " + document.getName());
            return toDTO(document);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload document", e);
        }
    }

    public Resource downloadDocument(Long id, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getAccessibleBy().contains(email) && !isHrManager(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this document");
        }

        try {
            Path filePath = Paths.get(document.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            return resource;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download document", e);
        }
    }

    public List<DocumentDTO> getAccessibleDocuments(String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        boolean isHrManager = isHrManager(token);
        return documentRepository.findAll().stream()
                .filter(doc -> isHrManager || doc.getAccessibleBy().contains(email))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DocumentRequestDTO requestDocument(DocumentRequestDTO requestDTO, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        DocumentRequest request = new DocumentRequest();
        request.setType(requestDTO.getType());
        request.setDescription(requestDTO.getDescription());
        request.setStatus("PENDING");
        request.setRequestDate(LocalDateTime.now());
        request.setUrgency(requestDTO.isUrgency());
        request.setEmployeeEmail(email);
        request = documentRequestRepository.save(request);

        sendNotification(email, "Document request submitted: " + request.getType());
        notifyHrManagers("New document request from " + employee.getFirstname() + " " + employee.getLastname());
        return toRequestDTO(request);
    }

    public List<DocumentRequestDTO> getMyRequests(String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        return documentRequestRepository.findByEmployeeEmailOrderByRequestDateDesc(email)
                .stream()
                .map(this::toRequestDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentRequestDTO> getPendingRequests(String token) {
        if (!isHrManager(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR Managers can view pending requests");
        }
        return documentRequestRepository.findByStatusInOrderByRequestDateDesc(Arrays.asList("PENDING"))
                .stream()
                .map(this::toRequestDTO)
                .collect(Collectors.toList());
    }

    public DocumentRequestDTO reviewRequest(Long id, String status, String token) {
        if (!isHrManager(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR Managers can review requests");
        }
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!Arrays.asList("APPROVED", "REJECTED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        request.setStatus(status);
        request = documentRequestRepository.save(request);

        sendNotification(request.getEmployeeEmail(), "Document request " + status.toLowerCase() + ": " + request.getType());
        return toRequestDTO(request);
    }

    public void cancelRequest(Long id, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!request.getEmployeeEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own requests");
        }
        if (!request.getStatus().equals("PENDING")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be cancelled");
        }
        documentRequestRepository.delete(request);
        sendNotification(email, "Document request cancelled: " + request.getType());
    }

    /*
    public DocumentDTO generateDocument(String name, Map<String, Object> parameters, String accessibleBy, String templateType, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        List<String> accessList = validateAndTransformAccess(accessibleBy);

        String fileName = UUID.randomUUID() + "_" + name + ".pdf";
        String filePath = DOCUMENTS_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(DOCUMENTS_DIR));
            // Placeholder for document generation logic
            // Assume a method like generatePdfFromTemplate(templateType, parameters, filePath) exists
            generatePdfFromTemplate(templateType, parameters, filePath);

            Document document = new Document();
            document.setName(name);
            document.setFilePath(filePath);
            document.setUploadedBy(employee.getFirstname() + " " + employee.getLastname());
            document.setGenerated(true);
            document.setAccessibleBy(accessList);
            document.setUploadDate(LocalDateTime.now());
            document = documentRepository.save(document);

            sendNotification(employee.getEmail(), "Document generated: " + document.getName());
            return toDTO(document);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate document", e);
        }
    }
    */

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList("application/pdf", "image/jpeg", "image/png").contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPEG, and PNG files are allowed");
        }
    }

    private List<String> validateAndTransformAccess(String access) {
        try {
            List<String> accessList = objectMapper.readValue(access, new TypeReference<List<String>>() {});
            List<String> validEmails = new ArrayList<>();
            for (String entry : accessList) {
                if (entry.startsWith("dep_")) {
                    Long depId = Long.valueOf(entry.substring(4));
                    departmentRepository.findById(depId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found: " + depId));
                    validEmails.addAll(employeeRepository.findByDepartmentId(depId)
                            .stream()
                            .map(Employee::getEmail)
                            .collect(Collectors.toList()));
                } else {
                    employeeRepository.findByEmail(entry)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found: " + entry));
                    validEmails.add(entry);
                }
            }
            return validEmails;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid access format", e);
        }
    }

    private boolean isHrManager(String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        return "HR_MANAGER".equals(role);
    }

    private void sendNotification(String email, String message) {
        NotificationService.sendNotification(email, message, notificationRepository, messagingTemplate);
    }

    private void notifyHrManagers(String message) {
        List<User> hrManagers = userRepository.findAllByRoleNot(User.UserRole.EMPLOYEE);
        for (User hr : hrManagers) {
            sendNotification(hr.getEmail(), message);
        }
    }

    private DocumentDTO toDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setFilePath(document.getFilePath());
        dto.setUploadedBy(document.getUploadedBy());
        dto.setUploadDate(document.getUploadDate());
        dto.setGenerated(document.isGenerated());
        dto.setAccessibleBy(document.getAccessibleBy());
        return dto;
    }

    private DocumentRequestDTO toRequestDTO(DocumentRequest request) {
        DocumentRequestDTO dto = new DocumentRequestDTO();
        dto.setId(request.getId());
        dto.setType(request.getType());
        dto.setDescription(request.getDescription());
        dto.setStatus(request.getStatus());
        dto.setRequestDate(request.getRequestDate());
        dto.setUrgency(request.isUrgency());
        dto.setEmployeeEmail(request.getEmployeeEmail());
        return dto;
    }
}
