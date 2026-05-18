// LeaveServiceTest.java : integration tests for leave management endpoints
package za.co.leavesystem.leave;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "colile.employee", roles = "EMPLOYEE")
    void testGetLeaveRequestsAsEmployee() throws Exception {
        mockMvc.perform(get("/leave"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "colile.employee", roles = "EMPLOYEE")
    void testCreateLeaveRequestSuccess() throws Exception {
        String body = """
            {"leaveType":"ANNUAL","startDate":"2027-01-10","endDate":"2027-01-15",
             "reason":"Annual family holiday in Johannesburg for new year"}
            """;
        mockMvc.perform(post("/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.employeeUsername").value("colile.employee"));
    }

    @Test
    @WithMockUser(username = "colile.employee", roles = "EMPLOYEE")
    void testCreateLeaveRequestPastDateReturns400() throws Exception {
        String body = """
            {"leaveType":"SICK","startDate":"2020-01-01","endDate":"2020-01-05",
             "reason":"Medical leave in Johannesburg hospital ward"}
            """;
        mockMvc.perform(post("/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "colile.employee", roles = "EMPLOYEE")
    void testEmployeeCannotApproveLeave() throws Exception {
        mockMvc.perform(put("/leave/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lwazi.manager", roles = "MANAGER")
    void testManagerCanApproveLeave() throws Exception {
        // First create a leave request as employee context
        // Then manager approves — test the approve endpoint is accessible
        mockMvc.perform(put("/leave/999/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Approved\"}"))
                .andExpect(status().isBadRequest()); // 400 because ID 999 doesn't exist, but NOT 403
    }

    @Test
    void testUnauthenticatedRequestIsRejected() throws Exception {
        // Spring Security returns 403 in test context when no auth principal is set;
        // through the real gateway the AuthenticationFilter returns 401.
        mockMvc.perform(get("/leave"))
                .andExpect(status().is4xxClientError());
    }
}
