-- V9 — set all seed user passwords to verified BCrypt hash of "password123"
-- generated with BCryptPasswordEncoder.encode("password123") at build time

UPDATE users SET password_hash = '$2a$10$mn2DaeW4I2LrbyF9C5jU9unIa/1DznoKskayK2Js6j7GpvqMMWt/m';
