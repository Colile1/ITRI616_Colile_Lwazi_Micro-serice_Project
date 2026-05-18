package za.co.leavesystem.auth.dto;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean enabled;

    public UserResponse(Long id, String username, String email, String role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
