// AuditLog.java : records leave-related security events for auditing
package za.co.leavesystem.leave.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String details;

    public AuditLog() {}

    public AuditLog(String action, String username, String details) {
        this.action = action;
        this.username = username;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getUsername() { return username; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
}
