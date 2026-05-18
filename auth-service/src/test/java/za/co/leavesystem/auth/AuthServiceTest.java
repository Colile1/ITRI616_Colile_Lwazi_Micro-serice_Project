// AuthServiceTest.java : unit tests for authentication business logic
package za.co.leavesystem.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRegisterAndLoginSuccess() throws Exception {
        // Register a new user
        String registerBody = """
            {"username":"test.colile","email":"test.colile@nwu.ac.za",
             "password":"TestPass@1","role":"EMPLOYEE"}
            """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // Login with the same credentials
        String loginBody = """
            {"username":"test.colile","password":"TestPass@1"}
            """;
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.username").value("test.colile"));
    }

    @Test
    void testLoginWithInvalidCredentialsReturns401() throws Exception {
        String loginBody = """
            {"username":"nonexistent","password":"WrongPass@1"}
            """;
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterDuplicateUsernameReturns409() throws Exception {
        String body = """
            {"username":"duplicate.user","email":"dup@nwu.ac.za",
             "password":"TestPass@1","role":"EMPLOYEE"}
            """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void testLoginValidationRejectsBlankUsername() throws Exception {
        String loginBody = """
            {"username":"","password":"TestPass@1"}
            """;
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterInvalidPasswordPatternReturns400() throws Exception {
        String body = """
            {"username":"lwazi.test","email":"lwazi.test@nwu.ac.za",
             "password":"weakpassword","role":"EMPLOYEE"}
            """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
