// LeaveRepository.java : data access for leave request entities
package za.co.leavesystem.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.leavesystem.leave.model.LeaveRequest;
import za.co.leavesystem.leave.model.LeaveStatus;

import java.util.List;

public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeUsernameOrderByCreatedAtDesc(String username);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    List<LeaveRequest> findByStatus(LeaveStatus status);
}
