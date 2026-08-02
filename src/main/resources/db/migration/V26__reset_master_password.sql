-- One-time, a pedido explícito del usuario: fuerza la contraseña de la cuenta MASTER existente
-- (emy.rodriguezc28@gmail.com) a un valor conocido. Hash BCrypt (strength 10, mismo encoder que
-- SecurityConfig.passwordEncoder()) generado localmente, nunca a partir de la contraseña en
-- texto plano en ningún otro lugar de este repo.
UPDATE users
SET password_hash = '$2a$10$vqi6a93TdJuxGgJPPvPSPeo6RTyVOLXUQViqhyoN4ck0UtZ3mdcAu'
WHERE lower(email) = lower('emy.rodriguezc28@gmail.com');
