# Secure Leave Management System
## ITRI615 Project Brief

### Module
ITRI615 – Computer Security 1

### Project Title
Secure Leave Management Microservice System

---

# 1. Project Overview

The Secure Leave Management System is a microservice-based web application designed to manage employee leave requests securely within an organization. The system allows employees to submit leave applications while managers and administrators review, approve, or reject requests based on organizational policies.

The primary focus of this project is not only the implementation of microservices, but also the application of modern cybersecurity principles to protect sensitive employee and organizational data. The project demonstrates authentication, authorization, API gateway integration, secure communication, logging and monitoring, rate limiting, and secure software design practices.

The system will consist of a frontend client, an API Gateway, an Authentication Service, and a Leave Management Service. All communication between clients and backend services will pass through the API Gateway to ensure centralized routing and security enforcement.

---

# 2. Problem Statement

Organizations handle sensitive employee information such as leave balances, medical leave requests, employee identities, and approval records. If these systems are not properly secured, attackers or unauthorized users may gain access to confidential data, manipulate leave records, or perform unauthorized actions.

Traditional monolithic systems often expose excessive functionality and become difficult to secure and maintain. A microservice architecture separates concerns into smaller independent services, allowing better scalability, maintainability, and security management.

This project aims to design and implement a secure leave management platform that demonstrates modern security practices within a microservice architecture.

---

# 3. Project Objectives

The objectives of this project are:

- Design and implement a secure microservice-based leave management system.
- Implement JWT-based authentication for secure login sessions.
- Apply role-based access control (RBAC) to restrict unauthorized actions.
- Protect all backend services behind an API Gateway.
- Validate and sanitize user input to prevent common attacks such as XSS and injection attacks.
- Implement request rate limiting to mitigate brute-force and denial-of-service attacks.
- Implement centralized logging and monitoring for auditing and security visibility.
- Develop a simple frontend interface to demonstrate secure communication between frontend and backend services.
- Apply formal security patterns and best practices.

---

# 4. System Scenario

The system is intended for use within an organization where employees apply for leave electronically instead of using paper-based processes.

Employees can:

- Log into the system
- Submit leave requests
- View their own leave history
- Cancel pending requests

Managers or administrators can:

- View all employee leave requests
- Approve or reject leave applications
- Monitor leave request activity

Because the system handles employee personal information and organizational approval workflows, maintaining confidentiality, integrity, and access control is critically important.

---

# 5. Functional Requirements

## 5.1 Authentication

- Users must log in using a username and password.
- Successful authentication generates a JWT token.
- JWT tokens must be included in all protected API requests.

## 5.2 Authorization

The system will implement role-based access control.

### Employee Role
Employees can:
- Create leave requests
- View their own leave requests
- Cancel their own pending requests

Employees cannot:
- Approve leave requests
- Access other employee records
- Access administrator endpoints

### Manager/Admin Role
Managers and administrators can:
- View all leave requests
- Approve or reject requests
- Access audit information

## 5.3 Leave Management

Employees must be able to:
- Submit leave requests
- Specify leave type
- Enter start and end dates
- Provide a leave reason

Managers must be able to:
- Approve requests
- Reject requests
- Add approval comments

## 5.4 Logging and Monitoring

The system must log:
- Login attempts
- Failed authentication attempts
- Leave submissions
- Leave approvals and rejections
- Unauthorized access attempts

## 5.5 Frontend Integration

A simple frontend application will:
- Allow secure login
- Display leave forms
- Display leave request history
- Allow managers to review requests

---

# 6. Non-Functional Requirements

The system should:

- Maintain secure communication between frontend and backend
- Be modular and maintainable
- Follow microservice design principles
- Provide fast API response times
- Support scalability through independent services
- Prevent unauthorized access to sensitive data
- Maintain auditability through logging

---

# 7. Microservice Architecture

The system will use a simplified microservice architecture consisting of the following components:

## 7.1 API Gateway

The API Gateway acts as the single entry point into the system.

Responsibilities include:
- Request routing
- JWT token validation
- Rate limiting
- Security filtering
- Centralized access control

## 7.2 Authentication Service

Responsible for:
- User authentication
- JWT token generation
- User credential validation
- Role management

## 7.3 Leave Management Service

Responsible for:
- Leave request operations
- Approval workflows
- Leave record management
- Business logic enforcement

## 7.4 Frontend Client

The frontend communicates only with the API Gateway and never directly with backend services.

---

# 8. Technologies

## Backend
- Java
- Spring Boot
- Spring Security
- Maven

## Frontend
- React.js
- JavaScript

## Database
- PostgreSQL

## Security
- JWT Authentication
- RBAC Authorization
- BCrypt password hashing

## Logging and Monitoring
- SLF4J
- Logback
- Prometheus (optional)

## API Gateway
- Spring Cloud Gateway

## Version Control
- GitHub

---

# 9. API Endpoints

## Authentication Endpoints

### POST /auth/login
Authenticates users and returns JWT tokens.

### POST /auth/register
Creates new users.

---

## Leave Management Endpoints

### GET /leave
Returns leave requests.

- Employees only see their own requests.
- Managers/Admins can view all requests.

### POST /leave
Creates a new leave request.

### PUT /leave/{id}
Updates an existing leave request.

### DELETE /leave/{id}
Cancels a leave request.

### PUT /leave/{id}/approve
Approves a leave request.

### PUT /leave/{id}/reject
Rejects a leave request.

---

# 10. Database Design

## User Table

Fields:
- id
- username
- password
- role

## Leave Request Table

Fields:
- id
- employeeId
- leaveType
- startDate
- endDate
- reason
- status
- managerComment

## Audit Log Table

Fields:
- id
- action
- username
- timestamp
- details

---

# 11. Security Features

## 11.1 JWT Authentication

Users authenticate using JWT tokens to ensure stateless and secure sessions.

## 11.2 Role-Based Access Control (RBAC)

Authorization policies restrict access based on user roles.

## 11.3 Password Hashing

Passwords are hashed using BCrypt before storage.

## 11.4 Input Validation and Sanitization

The system validates:
- Leave dates
- Required fields
- Input lengths
- Leave reasons

Potentially malicious input such as scripts or HTML tags will be sanitized to prevent XSS attacks.

## 11.5 Rate Limiting

Rate limiting will restrict:
- Login attempts
- Excessive leave submissions
- Repeated API requests

This helps mitigate brute-force and denial-of-service attacks.

## 11.6 Secure Communication

HTTPS will be used where possible during deployment.

---

# 12. Logging and Monitoring

The system will maintain logs for:

- Authentication activity
- Failed login attempts
- Leave request creation
- Leave approvals and rejections
- Unauthorized access attempts
- API errors and exceptions

Prometheus metrics may also be implemented to monitor:

- API request counts
- Authentication failures
- Request response times
- Rate limiting activity

---

# 13. Security Patterns Applied

## API Gateway Pattern

All traffic passes through a centralized gateway that enforces security policies.

## Role-Based Access Control Pattern

User permissions are enforced according to predefined roles.

## Secure Session Pattern

JWT tokens securely manage authenticated sessions.

## Input Validation Pattern

All incoming data is validated and sanitized before processing.

## Least Privilege Principle

Users receive only the minimum permissions required to perform their tasks.

---

# 14. Deployment Considerations

The application may optionally be deployed to a cloud platform such as:

- Render
- Railway
- Azure
- AWS

Environment variables will be used for:
- JWT secrets
- Database credentials
- API keys

Sensitive credentials will never be committed to GitHub.

---

# 15. Expected Outcomes

By completing this project, the system should demonstrate:

- A functional microservice architecture
- Secure authentication and authorization
- Proper API Gateway implementation
- Effective logging and monitoring
- Secure input validation
- Rate limiting against abusive requests
- Practical application of cybersecurity principles
- A secure frontend-to-backend communication workflow

The project will also demonstrate the importance of securing enterprise systems that manage confidential employee information.

---

# 16. Conclusion

The Secure Leave Management System provides a practical implementation of secure microservice architecture principles within a real-world organizational scenario. The project demonstrates how authentication, authorization, logging, validation, and API security can be integrated into modern distributed systems.

The solution balances simplicity, maintainability, and security while satisfying the practical and theoretical requirements of the ITRI615 project.

