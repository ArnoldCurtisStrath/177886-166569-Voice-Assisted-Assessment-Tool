-- V3 — reset seed user passwords to correct BCrypt hash of "password123"
-- Original V2 hashes were for an unknown password — regenerated now

UPDATE users
SET password_hash = '$2a$10$FSjYp56p2alQZMjGaAWbmu4gAjYAdVdpSGqUQ7wyV6BY8q38yvcL.'
WHERE email = 'admin@strathmore.ac.ke';

UPDATE users
SET password_hash = '$2a$10$FSjYp56p2alQZMjGaAWbmu4gAjYAdVdpSGqUQ7wyV6BY8q38yvcL.'
WHERE email = 'teacher@strathmore.ac.ke';

UPDATE users
SET password_hash = '$2a$10$FSjYp56p2alQZMjGaAWbmu4gAjYAdVdpSGqUQ7wyV6BY8q38yvcL.'
WHERE email = 'parent@example.com';

UPDATE users
SET password_hash = '$2a$10$FSjYp56p2alQZMjGaAWbmu4gAjYAdVdpSGqUQ7wyV6BY8q38yvcL.'
WHERE email = 'student@example.com';
