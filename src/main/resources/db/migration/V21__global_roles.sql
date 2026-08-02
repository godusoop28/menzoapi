-- Amplía el enum de roles globales de USER/ADMIN (nunca usado por ninguna lógica de autorización)
-- a USER/CURATOR/LEADER/MASTER. El CHECK original (V1__init_schema.sql) solo permitía
-- 'USER'/'ADMIN' — hay que soltarlo o cualquier escritura de CURATOR/LEADER/MASTER falla a nivel
-- de base de datos aunque el enum de Java ya lo permita. No se vuelve a agregar un CHECK más
-- angosto: @Enumerated(EnumType.STRING) ya da seguridad de tipos para todo lo que la app escribe.
ALTER TABLE users DROP CONSTRAINT users_role_check;

ALTER TABLE users ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT false;

-- Bootstrap inicial de la cuenta MASTER si ya existe (registro futuro lo cubre AuthService.register()
-- y todo boot lo re-verifica MasterAccountBootstrap; esto solo cubre el caso de que la cuenta ya
-- exista al desplegar esta migración).
UPDATE users SET role = 'MASTER' WHERE lower(email) = lower('emy.rodriguezc28@gmail.com');
