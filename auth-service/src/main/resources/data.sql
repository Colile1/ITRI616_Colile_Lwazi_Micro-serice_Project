-- Seed default users for the Leave Management System
-- Passwords are BCrypt hashed. Plain text passwords:
--   admin123  -> ADMIN role
--   manager123 -> MANAGER role
--   employee123 -> EMPLOYEE role

INSERT INTO users (username, email, password, role, enabled) VALUES
('admin', 'admin@leavesystem.co.za', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true),
('lwazi.manager', 'lwazi@leavesystem.co.za', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'MANAGER', true),
('colile.employee', 'colile@leavesystem.co.za', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'EMPLOYEE', true),
('thabo.employee', 'thabo@leavesystem.co.za', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'EMPLOYEE', true);
