// ReviewRequest.java : DTO for manager approve/reject actions
package za.co.leavesystem.leave.dto;

import jakarta.validation.constraints.Size;

public class ReviewRequest {

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
