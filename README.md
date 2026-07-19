# Menzo API

Backend de **Menzo** (Spring Boot 4 + PostgreSQL) pensado para alimentar la app móvil
[`menzomovil`](../menzomovil) (Expo / React Native). Incluye autenticación JWT completa,
y todos los endpoints necesarios para reemplazar el almacenamiento local (AsyncStorage)
de la app por un backend real y multiusuario.

La app móvil todavía no fue tocada: sus `data/repositories/*Repository.ts` (interfaces)
y `store/reducer.ts` (acciones) ya están diseñados como el punto de enchufe para un
cliente HTTP futuro — este backend fue modelado para calzar 1:1 con esos contratos
(mismos nombres de campo, mismos flujos de "onboarding", "toggle like", "toggle follow", etc.).

## Stack

- **Java 23**, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation)
- **PostgreSQL** con migraciones **Flyway**
- **JWT** (access token corto + refresh token opaco rotable, almacenado hasheado en BD)
- **springdoc-openapi** (Swagger UI en `/docs`)

## Requisitos

- JDK 23
- Docker (para Postgres local) o una instancia Postgres propia

## Cómo correrlo en local

```bash
# 1. Levantar Postgres
docker compose up -d

# 2. Copiar variables de entorno
cp .env.example .env
# (ajusta JWT_SECRET si vas a exponerlo fuera de tu máquina)

# 3. Arrancar la API con datos de ejemplo (perfil "dev")
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

La API queda en `http://localhost:8080`. Swagger UI: `http://localhost:8080/docs`.

Sin el perfil `dev`, la base arranca vacía (solo con los catálogos de auras/intereses/
badges y la sala principal, sembrados por Flyway). Con `dev`, `DevDataSeeder` crea además
3 usuarios de ejemplo (`emy@menzo.dev`, `dais@menzo.dev`, `ren@menzo.dev`, contraseña
`Menzo123!`), posts, un evento y notificaciones — todo idempotente (no vuelve a sembrar
si ya hay usuarios).

Para producción, configura las variables de `.env.example` como variables de entorno reales
(especialmente `JWT_SECRET`, `DB_*` y `CORS_ALLOWED_ORIGINS`) y arranca sin el perfil `dev`.

## Autenticación

Flujo pensado para calzar con el `OnboardingPayload` que ya existe en la app móvil:

1. `POST /api/auth/register {email, password}` — crea la cuenta con un username/nombre
   provisional (`onboardingCompleted: false`) y devuelve tokens.
2. `POST /api/users/me/onboarding {displayName, username, aura, avatarUri, avatarGradient, interests}`
   — mismo shape que `OnboardingPayload` / la acción `COMPLETE_ONBOARDING` del reducer.
   Marca `onboardingCompleted: true`, otorga el badge `recien-llegado`, une al usuario a la
   sala principal y genera la notificación de bienvenida (igual que el reducer local).
3. `POST /api/auth/login {email, password}` en sesiones futuras.
4. `POST /api/auth/refresh {refreshToken}` — rota el refresh token (uso único).
5. `POST /api/auth/logout {refreshToken}` — revoca el refresh token.

El access token (JWT, ~15 min) va en `Authorization: Bearer <token>` en cada request
autenticado. El refresh token es un valor opaco de alta entropía (no es un JWT); solo se
guarda su hash SHA-256 en BD, con expiración y rotación en cada uso.

Los endpoints de **lectura** de contenido público (feed, perfiles, salas, eventos,
config de comunidad) no requieren login, pero si se manda un token válido, la respuesta
incluye campos dependientes del usuario (`likedByMe`, `followedByMe`, etc.). Todo lo que
modifica estado (crear, dar like, seguir, comentar, votar, enviar mensajes...) requiere
autenticación.

## Endpoints principales

| Área | Endpoints |
|---|---|
| Auth | `POST /api/auth/{register,login,refresh,logout}` |
| Perfil | `GET/PATCH /api/users/me`, `POST /api/users/me/onboarding`, `POST /api/users/me/heartbeat`, `GET/PATCH /api/users/me/settings` |
| Usuarios | `GET /api/users/{id}`, `GET /api/users/search?query=`, `PUT/DELETE /api/users/{id}/follow`, `GET /api/users/{id}/{followers,following,posts,wall}` |
| Catálogos | `GET /api/lookups/{auras,interests,badges}` |
| Posts | `GET/POST /api/posts`, `GET /api/posts/{featured,bookmarked,search}`, `GET/DELETE /api/posts/{id}`, `PUT/DELETE /api/posts/{id}/{like,bookmark}`, `POST /api/posts/{id}/vote`, `GET/POST /api/posts/{id}/comments` |
| Chat | `GET /api/chat/rooms`, `GET /api/chat/rooms/{id}`, `POST /api/chat/rooms/{id}/{join,leave}`, `PUT/DELETE /api/chat/rooms/{id}/favorite`, `GET/POST /api/chat/rooms/{id}/messages` |
| Comunidad | `GET /api/community/config`, `GET/POST /api/community/events`, `PUT/DELETE /api/community/events/{id}/attend` |
| Notificaciones | `GET /api/notifications`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` |
| Actividad | `GET/POST /api/activity/recently-viewed`, `GET/POST/DELETE /api/activity/recent-searches` |
| Archivos | `POST /api/uploads` (multipart, devuelve URL pública servida en `/files/**`) |

Todas las listas paginables devuelven `{items, page, size, totalElements, totalPages, hasNext}`
(`?page=&size=&sort=`).

## Modelo de datos

Ver `src/main/resources/db/migration/V1__init_schema.sql` para el esquema completo y
`V2__seed_reference_data.sql` para los catálogos (auras, intereses, badges, config de
comunidad, sala principal). Resumen de entidades: usuarios, follows, visitas a perfil,
posts (texto/imagen/encuesta/pregunta/evento) con tags/likes/bookmarks/comentarios,
encuestas con opciones y votos, salas de chat con miembros/favoritos/mensajes, muro de
perfil, eventos con asistentes, notificaciones, configuración de comunidad, vistos
recientemente y búsquedas recientes por usuario.

## Notas para conectar la app móvil

- Los DTOs de perfil/onboarding usan los mismos nombres de campo que
  `menzomovil/src/types/*.ts` (`displayName`, `avatarGradient`, `aura`, `interests`,
  `bookmarkedBy` → expresado como `bookmarkedByMe` por ser una vista por-usuario, etc.),
  para que escribir el cliente HTTP futuro (que implementará las interfaces
  `ProfileRepository`, `PostRepository`, `ChatRepository`, `CommunityRepository`) sea
  un mapeo casi directo.
- `favorite` en salas y `attendees`/`likes`/`bookmarkedBy` en el mock eran listas globales
  porque la app corre con un solo usuario local; en la API son relaciones por-usuario
  (`favorite`, `likedByMe`, `bookmarkedByMe`, `attendingByMe`, `followedByMe`) porque el
  backend sí es multiusuario.
- `MAIN_ROOM_ID` del store local corresponde a la sala con `slug: "main"` en esta API.
"# menzoapi" 
