-- src/test/resources/sql/insert-test-user.sql
INSERT INTO users (id, email, password, role, first_name, last_name)
VALUES (1, 'testuser@example.com', '$2a$10$dXJ3SWc.F.R.S.Y.Z.IXd.1z7X/..7.X.X/..7.X.X/..7.X.X/..7X', 'ROLE_USER', 'Test', 'User')
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email;
-- Use a dummy bcrypt hash for the password; @WithUserDetails doesn't check it.
-- ON CONFLICT helps if running multiple tests that might try to insert the same user ID.