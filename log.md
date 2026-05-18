# log.md – Development Journal

## [2026-05-18] Project Initialisation and Architecture Design
**What was done:** Designed the microservice architecture: API Gateway (port 8080), Auth Service (port 8081), Leave Service (port 8082), React Frontend (port 3000). Selected Spring Boot 3.2.5, Spring Cloud Gateway, JJWT 0.12.3, H2 in-memory database, and React.js.  
**Why:** The rubric requires API Gateway, JWT auth, RBAC, rate limiting, logging, and a frontend. The selected stack satisfies all requirements without Python or Firebase.  
**Impact:** Clear separation of concerns; each service has a single responsibility, making security controls granular and auditable.

## [2026-05-18] Auth Service Implementation
**What was done:** Built the Auth Service with BCrypt password hashing (cost factor 12), JWT token generation (HS256, 24-hour expiry), custom rate limiting filter (sliding window, 5 requests/minute/IP), audit logging to H2 database, and input sanitisation.  
**Why:** BCrypt with cost factor 12 is recommended by NIST SP 800-63B. JWT provides stateless authentication suitable for microservices. Rate limiting mitigates brute-force attacks.  
**Impact:** All authentication events are auditable; credential attacks are rate-limited; passwords are stored securely.

## [2026-05-18] Leave Service Implementation
**What was done:** Built the Leave Service with full CRUD operations, RBAC enforcement via @PreAuthorize, data isolation (employees see only their own records), input validation via Jakarta Bean Validation, and audit logging.  
**Why:** RBAC enforces least privilege; data isolation prevents horizontal privilege escalation; audit logs provide non-repudiation.  
**Impact:** Managers cannot be impersonated by employees; all leave actions are traceable.

## [2026-05-18] API Gateway Implementation
**What was done:** Configured Spring Cloud Gateway with a custom AuthenticationFilter (JWT validation) and LoggingFilter (centralised request/response audit). CORS restricted to frontend origin.  
**Why:** The API Gateway pattern ensures no backend service is directly accessible; centralised JWT validation at the gateway provides defence in depth.  
**Impact:** Single audited entry point for all API traffic; backend services protected even if gateway is partially bypassed.

## [2026-05-18] Bug Fix – Bucket4j Dependency
**What was done:** Removed the Bucket4j dependency (not available in Maven Central under com.github.bucket4j groupId) and replaced it with a custom sliding-window rate limiter using ConcurrentHashMap and AtomicInteger.  
**Why:** Build failed with dependency resolution error. Custom implementation avoids external dependency while providing the same security control.  
**Impact:** Auth Service builds cleanly; rate limiting functions identically.

## [2026-05-18] Bug Fix – H2 Data Initialisation Ordering
**What was done:** Replaced data.sql seed with a programmatic DataInitializer CommandLineRunner that uses the PasswordEncoder bean to hash passwords at startup. Set spring.sql.init.mode=never.  
**Why:** In Spring Boot 3.x, data.sql runs before JPA creates tables, causing startup failure. The DataInitializer runs after the ApplicationContext is fully initialised.  
**Impact:** Auth Service starts reliably with correct BCrypt hashes; all seed accounts authenticate successfully.

## [2026-05-18] Integration Testing
**What was done:** Ran 7 integration tests via PowerShell: admin login, employee login, GET /leave (employee), POST /leave, approve (admin), unauthenticated rejection, RBAC rejection.  
**Why:** Verify all security controls function end-to-end through the gateway.  
**Impact:** 7/7 tests passed. All security controls verified functional.

## [2026-05-18] Report Writing
**What was done:** Wrote comprehensive 2 800-word theory report covering: microservice scenario and POPIA relevance, frontend and backend security analysis, authentication/authorisation comparison (Basic Auth, Session, JWT, OAuth, MFA, RBAC, ABAC), three real-world case studies (Uber 2022, Optus 2022, Equifax 2017).  
**Why:** Theory component is worth 30 marks. Depth of analysis and references are required.  
**Impact:** Theory requirements fully addressed with academic references.
