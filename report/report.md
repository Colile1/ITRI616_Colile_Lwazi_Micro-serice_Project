# ITRI615 – Computer Security 1
## Secure Leave Management Microservice System
### Project Report

**Student:** Colile Lwazi Sibanda  
**Module:** ITRI615 – Computer Security 1  
**Institution:** North-West University (NWU)  
**Date:** May 2026  
**Lecturer:** Prof. Lynette Drevin  
**Facilitator:** Mr. Bernard Swanepoel  

---

## Table of Contents

1. Microservice Scenario
2. Security Analysis
3. Authentication and Authorisation Methods
4. Real-World Security Failures and Analysis

---

## 1. Microservice Scenario (3 Marks)

### 1.1 System Overview

The Secure Leave Management System is a microservice-based web application designed for organisations to manage employee leave applications electronically. Employees submit leave requests via a React.js frontend, which communicates exclusively through a Spring Cloud API Gateway. The backend is divided into two independent microservices: an Authentication Service and a Leave Management Service, both built with Spring Boot.

### 1.2 Why Is Security Critical for This System?

Leave management systems handle sensitive personal and organisational data. The following considerations demonstrate why robust security is essential:

**Confidential Personal Information:** Leave records contain sensitive employee data including medical leave reasons, family circumstances, and employment patterns. Unauthorised access to this information violates employee privacy and may breach South African data protection legislation such as the Protection of Personal Information Act (POPIA), Act 4 of 2013.

**Financial Impact:** Leave balances directly affect payroll calculations. An attacker who manipulates leave records could fraudulently approve additional leave, causing significant financial losses to the organisation.

**Operational Disruption:** Unauthorised modification of leave approvals could result in critical staff being absent simultaneously, disrupting business operations. For example, if a threat actor approved leave for all nurses in a hospital simultaneously, patient safety could be compromised.

**Identity and Access Threats:** Without proper authentication, an employee could impersonate a manager to approve their own leave requests, or view colleagues' confidential medical leave records.

**Regulatory Compliance:** Organisations subject to ISO 27001 or King IV governance frameworks are obligated to protect HR systems from unauthorised access and demonstrate auditability.

---

## 2. Security Analysis (7 Marks)

### 2.1 System Architecture

The system implements a layered security architecture:

```
[React Frontend (Port 3000)]
         ↓  HTTPS / Bearer Token
[API Gateway (Port 8080)] ← JWT Validation, Rate Limiting, Centralised Logging
         ↓                           ↓
[Auth Service (Port 8081)]   [Leave Service (Port 8082)]
         ↓                           ↓
   [H2 In-Memory DB]          [H2 In-Memory DB]
```

All client requests must pass through the API Gateway. Backend services are not exposed directly to the frontend, enforcing the **API Gateway security pattern**.

### 2.2 Frontend Security

The React.js frontend implements the following security measures:

**Client-Side Input Validation and Sanitisation:** All user inputs are validated and sanitised before submission. The `sanitiseInput()` function strips dangerous characters (`<>"'%;()&+`) that could be used in XSS or injection attacks:

```javascript
const sanitiseInput = (value) => {
  if (typeof value !== 'string') return value;
  return value.replace(/[<>"'%;()&+]/g, '').trim();
};
```

**Content Security Policy (CSP):** The `index.html` sets a strict Content Security Policy:
```html
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; script-src 'self'; connect-src 'self' http://localhost:8080" />
```
This prevents injection of external scripts and restricts API calls to the gateway only.

**No Sensitive Data in localStorage:** Only the JWT token and non-sensitive user metadata (username, role) are stored in localStorage. No passwords or personally identifiable information is persisted client-side.

**Secure Token Handling:** The `AuthContext` manages token lifecycle. On logout, `localStorage.removeItem('token')` clears the token immediately, preventing session fixation.

**Rate Limit Feedback:** The frontend detects HTTP 429 (Too Many Requests) responses and displays user-friendly messages, discouraging brute-force attempts.

### 2.3 API Gateway Security

The API Gateway (Spring Cloud Gateway) enforces the following:

**JWT Validation Filter:** The custom `AuthenticationFilter` validates JWT tokens on every request to protected routes (`/leave/**`) before forwarding them to the Leave Service. Invalid or expired tokens return HTTP 401 immediately:

```java
Claims claims = Jwts.parser().verifyWith(buildSigningKey())
    .build().parseSignedClaims(token).getPayload();
```

**Centralised Logging:** The `LoggingFilter` records every request and response including HTTP method, path, status code, client IP, and response time. This provides a complete audit trail at the gateway level.

**CORS Configuration:** The gateway restricts cross-origin requests to the frontend origin (`http://localhost:3000`) only, preventing unauthorised cross-origin API calls.

**Single Entry Point:** No backend service is accessible directly from the internet. All traffic must pass through the gateway, which is the **API Gateway security pattern** in practice.

### 2.4 Authentication Service Security

**BCrypt Password Hashing:** Passwords are hashed using BCrypt with a cost factor of 12. BCrypt is an adaptive hashing algorithm designed to be computationally expensive, making brute-force attacks impractical:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

**JWT Token Generation:** Upon successful authentication, the service generates a signed JWT containing the username, role, and expiry timestamp. The token is signed with HMAC-SHA256 using a 256-bit secret key:

```java
return Jwts.builder()
    .claims(claims)
    .subject(user.getUsername())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
    .signWith(buildSigningKey())
    .compact();
```

**Rate Limiting:** The `RateLimitFilter` implements a sliding window counter that restricts login attempts to 5 per minute per IP address. Exceeding this limit returns HTTP 429:

```java
if (count > MAX_REQUESTS) {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    // ...
}
```

**Stateless Sessions:** Spring Security is configured with `SessionCreationPolicy.STATELESS`, eliminating server-side session management and the associated session hijacking risks.

**Input Sanitisation:** The `AuthService.sanitiseInput()` method strips HTML and script characters from all user-submitted strings before processing.

**Security Headers:** The HTTP response includes security headers including `Content-Security-Policy` and frame options via Spring Security.

**Audit Logging:** Every login attempt (successful and failed), registration, and security event is recorded in the `audit_log` table with username, IP address, timestamp, and outcome.

### 2.5 Leave Service Security

**JWT Re-Validation:** The Leave Service independently re-validates the JWT token via its `JwtAuthFilter`. Even if the API Gateway were compromised, the Leave Service would reject unauthenticated requests.

**Role-Based Access Control (RBAC):** Spring Security's `@PreAuthorize` annotations enforce method-level access control. The approve and reject endpoints are restricted to MANAGER and ADMIN roles:

```java
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public ResponseEntity<?> approveLeaveRequest(...) { ... }
```

**Data Isolation:** Employees can only view their own leave requests. The `getLeaveRequests()` method checks the authenticated user's role and filters accordingly, preventing horizontal privilege escalation.

**Input Validation:** Jakarta Bean Validation annotations enforce field-level constraints on all DTO classes:
- `@NotBlank`, `@Size` for text fields
- `@FutureOrPresent` for date fields
- `@Pattern` for enum validation

**XSS Prevention:** All string inputs are sanitised through `sanitiseInput()` before persistence, removing characters that could enable XSS attacks when data is rendered.

**Audit Logging:** All leave operations (create, cancel, approve, reject) are logged with the responsible user and timestamp.

**Prometheus Metrics:** The service exposes metrics via `/actuator/prometheus` for operational monitoring including request counts, response times, and error rates.

### 2.6 Security Patterns Applied

| Pattern | Implementation |
|---------|---------------|
| API Gateway Pattern | Spring Cloud Gateway routes all traffic; backend not directly accessible |
| Secure Session Pattern | Stateless JWT tokens; no server-side sessions |
| Role-Based Access Control | Spring Security `@PreAuthorize`; EMPLOYEE, MANAGER, ADMIN roles |
| Input Validation Pattern | Jakarta Bean Validation + custom sanitisation at all boundaries |
| Least Privilege Principle | Employees restricted to own data; managers restricted to approval functions |
| Audit Trail Pattern | All security events logged to `audit_log` table |
| Defence in Depth | JWT validated at both gateway and service level |

---

## 3. Authentication and Authorisation Methods (10 Marks)

### 3.1 Overview of Authentication Methods

**3.1.1 Basic Authentication**

Basic Authentication transmits credentials as a Base64-encoded string in the HTTP `Authorization` header (RFC 7617). While simple to implement, it is fundamentally insecure unless used over HTTPS because:

- Credentials are transmitted with every request
- Base64 is encoding, not encryption — trivially reversible
- No token expiry mechanism
- Susceptible to credential interception on insecure connections

Basic Authentication is appropriate only for internal tool access in controlled environments. It is unsuitable for internet-facing systems handling sensitive data (Fielding & Reschke, 2014).

**3.1.2 Session-Based Authentication**

Session-based authentication stores session identifiers server-side, typically in an in-memory store or database, and uses cookies to maintain state. Limitations include:

- **Scalability:** Sessions must be shared across multiple server instances (requires sticky sessions or distributed session store)
- **Session Hijacking:** Cookie theft allows account takeover
- **CSRF Vulnerability:** Cross-Site Request Forgery attacks can exploit session cookies
- **Memory Overhead:** Large user bases require significant server memory for session storage

This approach is well-suited to traditional monolithic web applications but is problematic in distributed microservice architectures (Rescorla, 2018).

**3.1.3 JSON Web Tokens (JWT)**

JWT (RFC 7519) is a compact, URL-safe means of representing claims between two parties. A JWT consists of three Base64URL-encoded segments: Header, Payload, and Signature. The signature is computed using a secret key (HMAC-SHA256) or asymmetric keys (RS256).

**Advantages:**
- **Stateless:** The token contains all necessary information; no server-side storage required
- **Scalable:** Any service can validate the token independently using the shared secret
- **Cross-Domain:** Suitable for microservice architectures where services run on different hosts
- **Expiry:** Built-in `exp` claim enforces token lifetime

**Disadvantages:**
- **Token Revocation:** Stateless tokens cannot be invalidated before expiry without maintaining a denylist
- **Size:** JWTs are larger than session IDs
- **Secret Management:** The signing secret must be securely managed across all services

**3.1.4 OAuth 2.0**

OAuth 2.0 (RFC 6749) is an authorisation framework enabling third-party applications to access user resources without exposing credentials. It involves four roles: Resource Owner, Resource Server, Client, and Authorisation Server.

OAuth 2.0 is ideal for third-party integrations and federated identity (e.g., "Sign in with Google"). However, for an internal enterprise system with first-party authentication, OAuth adds significant complexity without proportionate benefit.

**3.1.5 Multi-Factor Authentication (MFA)**

MFA requires users to provide multiple verification factors: something they know (password), something they have (OTP), or something they are (biometric). MFA significantly reduces account compromise risk; the Microsoft Security Report (2019) indicates MFA blocks over 99.9% of automated account attacks. However, it adds friction to the user experience.

### 3.2 Overview of Authorisation Methods

**3.2.1 Role-Based Access Control (RBAC)**

RBAC assigns permissions to roles, and roles to users. This model is intuitive, easy to audit, and widely supported by frameworks like Spring Security. It is well-suited to hierarchical organisational structures (Sandhu et al., 1996).

**3.2.2 Attribute-Based Access Control (ABAC)**

ABAC evaluates policies based on attributes of the subject, resource, action, and environment. It is more flexible than RBAC but more complex to implement and audit (Hu et al., 2014).

**3.2.3 Permission-Based Access Control**

Fine-grained permissions are assigned directly to users or groups. Provides maximum flexibility but is administratively burdensome at scale.

### 3.3 Justification for Chosen Methods

**Choice: JWT Authentication + RBAC Authorisation**

**Why JWT over session-based authentication?**

The system uses a microservice architecture with three independent services (API Gateway, Auth Service, Leave Service). Session-based authentication would require a shared session store (e.g., Redis) accessible to all services, introducing a single point of failure and additional infrastructure complexity. JWT tokens are self-contained and can be validated by any service using the shared secret, eliminating this dependency.

The token expiry (`exp` claim set to 24 hours) limits the window of token misuse. The signing key (`HS256`) ensures token integrity — any modification invalidates the signature.

**Why RBAC over ABAC or permission-based?**

The leave management domain maps naturally to three clearly defined roles: EMPLOYEE, MANAGER, and ADMIN. RBAC provides a clean, auditable permission model:

| Role | Permissions |
|------|-------------|
| EMPLOYEE | Create/view/cancel own leave requests |
| MANAGER | View all requests, approve/reject |
| ADMIN | Full access including user management |

ABAC would add complexity without benefit for this use case. The role hierarchy is stable and well-understood by the organisation.

**Why BCrypt over MD5/SHA-1?**

BCrypt incorporates a salt and a cost factor, making rainbow table attacks infeasible and GPU-accelerated brute-force attacks impractical. SHA-1 and MD5 are cryptographically broken and trivially reversed using pre-computed tables. The National Institute of Standards and Technology (NIST SP 800-63B) recommends adaptive hashing algorithms such as BCrypt, Argon2, or scrypt for password storage (Grassi et al., 2017).

**References:**

- Fielding, R. & Reschke, J. (2014). *RFC 7235: Hypertext Transfer Protocol (HTTP/1.1): Authentication*. IETF.
- Jones, M., Bradley, J. & Sakimura, N. (2015). *RFC 7519: JSON Web Token*. IETF.
- Rescorla, E. (2018). *RFC 8446: The Transport Layer Security (TLS) Protocol Version 1.3*. IETF.
- Sandhu, R., Coyne, E., Feinstein, H. & Youman, C. (1996). *Role-Based Access Control Models*. IEEE Computer, 29(2), pp. 38–47.
- Grassi, P. et al. (2017). *NIST Special Publication 800-63B: Digital Identity Guidelines*. NIST.
- Hu, V. et al. (2014). *Guide to Attribute Based Access Control (ABAC) Definition and Considerations*. NIST SP 800-162.

---

## 4. Real-World Security Failures and Analysis (10 Marks)

### 4.1 Case Study 1: Uber Data Breach (2022)

**What Happened:**

In September 2022, a threat actor gained access to Uber's internal systems including AWS, Google Cloud, HackerOne bug bounty reports, and Slack. The attacker used **social engineering** to obtain credentials from an Uber contractor via WhatsApp, then found hardcoded AWS credentials in a PowerShell script on Uber's internal network (Robertson & Riley, 2022).

**Security Failures:**

1. **Hardcoded Credentials:** AWS access keys stored in plaintext inside a PowerShell script. This is a fundamental secret management failure.
2. **Insufficient MFA:** The attacker bypassed MFA using MFA fatigue — repeatedly sending push notifications until the contractor accepted.
3. **Excessive Privileges:** The compromised contractor account had access far beyond what was needed for their role (violating least privilege).
4. **Lack of Anomaly Detection:** No alerting triggered when credentials were used from an unusual location or accessed sensitive resources in an unusual pattern.

**Preventive Measures:**

- **Secret Management:** Secrets such as API keys and database credentials should never be hardcoded. Tools such as HashiCorp Vault, AWS Secrets Manager, or environment variables should be used.
- **Phishing-Resistant MFA:** FIDO2/WebAuthn hardware tokens resist MFA fatigue attacks because they require physical presence.
- **Zero Trust Architecture:** Network access should require continuous verification, not just initial authentication. Micro-segmentation limits lateral movement after compromise.
- **Privileged Access Management (PAM):** Contractors should receive time-limited, scoped credentials through a PAM system.

**Relevance to This Project:**

The Secure Leave Management System uses environment variables (`JWT_SECRET`, `DATABASE_PASSWORD`) for all credentials, with no hardcoded secrets in source code. The `.gitignore` excludes configuration files containing credentials. This directly addresses the credential exposure failure that enabled the Uber breach.

---

### 4.2 Case Study 2: Optus Data Breach (2022)

**What Happened:**

In September 2022, Australian telecommunications company Optus suffered a breach exposing the personal data of approximately 9.8 million customers, including passport numbers, driver's licence numbers, and dates of birth. The attacker exploited an **unauthenticated API endpoint** that was accessible from the public internet (Ferguson, 2022).

**Security Failures:**

1. **Unauthenticated API Endpoint:** A customer data API was exposed without requiring authentication tokens. Any internet user could query it.
2. **No Rate Limiting:** The attacker was able to systematically enumerate customer records without triggering any rate limiting or anomaly detection.
3. **Excessive Data Exposure:** The API returned full customer PII when only a subset was needed by legitimate clients.
4. **No API Gateway:** Without a gateway enforcing authentication and rate limiting, the endpoint was completely unprotected.

**Preventive Measures:**

- **Authentication on All Endpoints:** Every API endpoint should require authentication. Even "public" endpoints should be monitored.
- **API Gateway:** A gateway enforcing JWT validation and rate limiting would have prevented both the unauthenticated access and the systematic enumeration.
- **Rate Limiting:** Automatic throttling at 5–10 requests per minute per IP would have made bulk data extraction infeasible.
- **Data Minimisation:** API responses should return only the minimum data necessary for the requesting client's purpose (OWASP API Security Top 10 – API3: Excessive Data Exposure).

**Relevance to This Project:**

The Secure Leave Management System addresses these failures directly:
- All `/leave/**` endpoints require a valid JWT token, validated at both the API Gateway and the Leave Service.
- Rate limiting (5 requests/minute/IP) is enforced on the login endpoint.
- Data isolation ensures employees cannot access other employees' records.
- The API Gateway provides a single, audited entry point — no service is directly accessible.

---

### 4.3 Case Study 3: Equifax Data Breach (2017)

**What Happened:**

The Equifax breach exposed the personal financial records of 147 million Americans. Attackers exploited an unpatched **Apache Struts vulnerability** (CVE-2017-5638) — a known critical vulnerability for which a patch had been available for two months before the breach (Fruhlinger, 2020).

**Security Failures:**

1. **Unpatched Dependencies:** The vulnerability was publicly known and patched, but Equifax failed to apply the update.
2. **Inadequate Vulnerability Management:** No automated scanning process identified the vulnerable component in production.
3. **Lack of Network Segmentation:** Once inside, attackers moved laterally through 51 different systems without restriction.
4. **Insufficient Monitoring:** The attack went undetected for 78 days because SSL inspection on the monitoring system had lapsed.
5. **Excessive Data Retention:** Equifax retained data far beyond its useful life, maximising breach impact.

**Preventive Measures:**

- **Automated Dependency Scanning:** Tools such as OWASP Dependency-Check, Snyk, or GitHub Dependabot should scan all third-party libraries for known vulnerabilities (CVEs) continuously.
- **Patch Management Policy:** Critical CVEs should be patched within 72 hours. Patches should be tested in staging before production deployment.
- **Network Segmentation / Micro-Segmentation:** Services should only be able to communicate with explicitly authorised peers. A microservice architecture with service mesh (e.g., Istio) enforces this.
- **Centralised Monitoring:** Prometheus, Grafana, or SIEM tools should alert on anomalous query volumes, access to sensitive tables, or lateral movement indicators.

**Relevance to This Project:**

The Secure Leave Management System uses Spring Boot 3.2.5 with explicitly declared dependency versions. The Maven Bill of Materials (BOM) approach ensures dependency versions are tracked. The project uses separate database schemas per service, limiting blast radius if one service is compromised. Prometheus metrics are exposed, enabling future integration with alerting systems.

---

### 4.4 Summary Table

| Failure | Root Cause | Preventive Control | System Implementation |
|---------|-----------|-------------------|----------------------|
| Uber 2022 | Hardcoded credentials | Secret management (env vars) | JWT_SECRET via environment variable |
| Optus 2022 | Unauthenticated API | API Gateway + authentication | Spring Cloud Gateway + JWT filter |
| Optus 2022 | No rate limiting | Rate limiting | 5 requests/min/IP on login |
| Equifax 2017 | Unpatched dependency | Dependency management | Pinned Maven versions, BOM |
| Equifax 2017 | No monitoring | Centralised logging + metrics | SLF4J + Prometheus actuator |

---

## Conclusion

The Secure Leave Management System demonstrates a comprehensive application of modern cybersecurity principles within a microservice architecture. The system addresses the OWASP API Security Top 10 including broken authentication (A1), broken object-level authorisation (A2), excessive data exposure (A3), lack of rate limiting (A4), and security misconfiguration (A7). The theoretical analysis demonstrates an understanding of authentication and authorisation trade-offs and lessons learnt from real-world security failures.

The project satisfies all practical requirements: a functional microservice backend, API Gateway, JWT authentication, RBAC authorisation, input validation, rate limiting, centralised logging, Prometheus monitoring, and a React.js frontend — all implemented without Python or Firebase.

---

*Word count: approximately 2 800 words.*
