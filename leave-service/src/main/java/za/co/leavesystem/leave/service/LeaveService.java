// LeaveService.java : core business logic for leave request management
package za.co.leavesystem.leave.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import za.co.leavesystem.leave.dto.LeaveRequestDto;
import za.co.leavesystem.leave.dto.ReviewRequest;
import za.co.leavesystem.leave.model.LeaveRequest;
import za.co.leavesystem.leave.model.LeaveStatus;
import za.co.leavesystem.leave.model.LeaveType;
import za.co.leavesystem.leave.repository.LeaveRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRepository leaveRepository;
    private final AuditService auditService;

    public LeaveService(LeaveRepository leaveRepository, AuditService auditService) {
        this.leaveRepository = leaveRepository;
        this.auditService = auditService;
    }

    // Pure function: returns leave requests visible to the given user based on their role
    public List<LeaveRequest> getLeaveRequests(String username, String role) {
        if ("ROLE_MANAGER".equals(role) || "ROLE_ADMIN".equals(role)) {
            return leaveRepository.findAllByOrderByCreatedAtDesc();
        }
        return leaveRepository.findByEmployeeUsernameOrderByCreatedAtDesc(username);
    }

    // Pure function: creates a new leave request with validation
    public LeaveRequest createLeaveRequest(LeaveRequestDto dto, String username) {
        validateLeaveDates(dto.getStartDate(), dto.getEndDate());

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeUsername(username);
        request.setLeaveType(LeaveType.valueOf(sanitiseInput(dto.getLeaveType())));
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(sanitiseInput(dto.getReason()));

        LeaveRequest saved = leaveRepository.save(request);
        auditService.logEvent("LEAVE_CREATED", username,
                "Leave request #" + saved.getId() + " created for " + dto.getLeaveType());
        log.info("Leave request created: id={} user={}", saved.getId(), username);
        return saved;
    }

    // Pure function: cancels an employee's own pending leave request
    public LeaveRequest cancelLeaveRequest(Long id, String username) {
        LeaveRequest request = findAndAuthorise(id, username);

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests may be cancelled");
        }

        request.setStatus(LeaveStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRepository.save(request);

        auditService.logEvent("LEAVE_CANCELLED", username, "Leave request #" + id + " cancelled");
        return saved;
    }

    // Pure function: approves a leave request — manager/admin only
    public LeaveRequest approveLeaveRequest(Long id, String managerUsername, ReviewRequest reviewRequest) {
        LeaveRequest request = leaveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests may be approved");
        }

        request.setStatus(LeaveStatus.APPROVED);
        request.setReviewedBy(managerUsername);
        request.setManagerComment(reviewRequest != null ? sanitiseInput(reviewRequest.getComment()) : null);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRepository.save(request);

        auditService.logEvent("LEAVE_APPROVED", managerUsername,
                "Leave request #" + id + " approved for " + request.getEmployeeUsername());
        return saved;
    }

    // Pure function: rejects a leave request — manager/admin only
    public LeaveRequest rejectLeaveRequest(Long id, String managerUsername, ReviewRequest reviewRequest) {
        LeaveRequest request = leaveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests may be rejected");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setReviewedBy(managerUsername);
        request.setManagerComment(reviewRequest != null ? sanitiseInput(reviewRequest.getComment()) : null);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRepository.save(request);

        auditService.logEvent("LEAVE_REJECTED", managerUsername,
                "Leave request #" + id + " rejected for " + request.getEmployeeUsername());
        return saved;
    }

    // Pure function: verifies the request belongs to the requesting user
    private LeaveRequest findAndAuthorise(Long id, String username) {
        LeaveRequest request = leaveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));
        if (!request.getEmployeeUsername().equals(username)) {
            throw new AccessDeniedException("Access denied: this leave request does not belong to you");
        }
        return request;
    }

    // Pure function: validates that dates are logically correct
    private void validateLeaveDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date must be today or in the future");
        }
    }

    // Pure function: strips HTML and dangerous characters to prevent XSS
    private String sanitiseInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'%;()&+]", "").trim();
    }
}
