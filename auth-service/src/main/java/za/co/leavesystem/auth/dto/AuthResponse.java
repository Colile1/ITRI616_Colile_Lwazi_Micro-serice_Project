// AuthResponse.java : DTO for authentication responses
package za.co.leavesystem.auth.dto;

public class AuthResponse {

    private String token;
    private String username;
    private String role;
    private String message;
    private long expiresIn;

    public AuthResponse(String token, String username, String role, long expiresIn) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.expiresIn = expiresIn;
        this.message = "Authentication successful";
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getMessage() { return message; }
    public long getExpiresIn() { return expiresIn; }
}
