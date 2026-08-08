# MediSalud — Sistema de Agendamiento de Citas Médicas (API REST)

MVP backend para digitalizar el agendamiento de citas de la clínica MediSalud: registro de médicos y pacientes, reserva de citas con control de disponibilidad por franja horaria, y cancelaciones con penalización por ausentismo.

> Prueba técnica de backend. No incluye frontend ni autenticación/autorización (fuera de alcance, según el enunciado).

## Tabla de contenido

- [Tecnologías utilizadas](#tecnologías-utilizadas)
- [Arquitectura](#arquitectura)
- [Ejecución local](#ejecución-local)
- [Endpoints de la API](#endpoints-de-la-api)
  - [Médicos (RF-01)](#médicos-rf-01)
  - [Pacientes (RF-02)](#pacientes-rf-02)
  - [Citas (RF-03)](#citas-rf-03)
  - [Disponibilidad (RF-04)](#disponibilidad-rf-04)
  - [Cancelación (RF-05)](#cancelación-rf-05)
  - [Listado de citas (RF-06)](#listado-de-citas-rf-06)
- [Reglas de negocio implementadas](#reglas-de-negocio-implementadas)

## Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Java 17 + Spring Boot 3.3.2 | Framework principal de la API REST |
| Spring Data JPA (Hibernate) | Acceso a datos |
| H2 (en memoria) | Base de datos del MVP |
| Flyway | Versionado del esquema de base de datos y carga de datos semilla |
| Bean Validation (Jakarta Validation) | Validación de inputs en los DTOs |
| springdoc-openapi (Swagger UI) | Documentación interactiva de la API |
| JUnit 5 + Mockito + AssertJ | Pruebas automatizadas |
| Lombok | Reducción de boilerplate en entidades |
| Maven | Gestión de dependencias y build |

## Arquitectura

**Arquitectura por capas con inversión de dependencias**, organizada por módulo de dominio (`medico`, y los que se van sumando por cada requerimiento) más un paquete `shared` para lo transversal:

```
com.ceiba.medisalud
├── medico/                 # Un paquete por dominio de negocio
│   ├── Medico.java              (entidad JPA)
│   ├── MedicoRepository.java    (Spring Data JPA)
│   ├── MedicoService.java       (interfaz — contrato del caso de uso)
│   ├── MedicoServiceImpl.java   (reglas de negocio)
│   ├── MedicoController.java    (adaptador HTTP)
│   ├── MedicoMapper.java        (entidad <-> DTO)
│   └── dto/
├── paciente/                # Mismo patrón que medico/
├── cita/
│   ├── Cita.java, EstadoCita.java, Penalizacion.java
│   ├── CitaRepository.java, PenalizacionRepository.java
│   ├── HorarioAtencionPolicy.java   (RN-01: franjas de 30 min + horario laboral)
│   ├── HolidayPolicy.java           (interfaz — punto de extensión para festivos)
│   ├── CitaService.java / CitaServiceImpl.java   (RN-01 a RN-05)
│   ├── CitaController.java
│   └── dto/
├── shared/
│   ├── exception/           (excepciones de dominio + manejador global)
│   └── validation/          (validadores custom reutilizables, ej. teléfono)
└── MediSaludApplication.java
```

**Por qué esta arquitectura:**

- **Separación por capas** (`controller` → `service` → `repository` → `entity`): el controller solo traduce HTTP ↔ DTO, el service concentra las reglas de negocio, y el repository resuelve la persistencia. Esto permite testear las reglas de negocio (lo más importante en un dominio de agendamiento con reglas como RN-01 a RN-06) sin necesidad de levantar el contexto web completo.
- **DTOs separados de las entidades JPA**: el contrato de la API no queda acoplado al modelo de persistencia, y evita exponer detalles de la base de datos (como se ve en RF-01, `MedicoRequest`/`MedicoResponse` son distintos de la entidad `Medico`).
- **Inversión de dependencias (DIP)**: los controllers dependen de interfaces de servicio (`MedicoService`), no de sus implementaciones. Esto facilita el testing (mocks) y permite cambiar la implementación sin tocar el controller.
- **Organización por dominio en vez de por capa técnica** (`medico/` en lugar de `controllers/`, `services/`, `repositories/` separados): mantiene junto todo lo relacionado a un mismo caso de uso, lo que escala mejor a medida que se agregan RF-02 (pacientes), RF-03 (citas), etc.
- **Manejo de errores centralizado** (`GlobalExceptionHandler` con `@RestControllerAdvice`): todas las respuestas de error siguen el mismo formato (`ApiError`) y los controllers no necesitan try/catch repetido — las reglas de negocio simplemente lanzan `ResourceNotFoundException`, `ConflictException` o `BusinessRuleException` y se traducen automáticamente a 404/409/400.
- **Persistencia con Flyway + H2 en memoria**: para este MVP se eligió H2 en memoria por simplicidad de ejecución local (cero configuración externa), pero el esquema y los datos de ejemplo se versionan con **Flyway** (`src/main/resources/db/migration`) en vez de `data.sql`/`ddl-auto=update`. Esto asegura que cualquiera que clone el repo obtenga exactamente el mismo esquema y los mismos datos semilla de forma reproducible, y deja documentado el historial de cambios de base de datos. Migrar a PostgreSQL en el futuro solo requiere cambiar el `datasource` — las migraciones ya son SQL estándar.
- **Validaciones reutilizables** (`shared/validation`): reglas de formato que se repiten entre dominios (ej. "teléfono con mínimo 7 dígitos", requerido tanto en RF-01 como en RF-02) se implementan una sola vez como anotación custom de Bean Validation (`@PhoneNumber`), evitando duplicar lógica (DRY).
- **`HolidayPolicy` como punto de extensión (OCP)**: el enunciado exige que no haya atención en "domingos y festivos" pero no entrega un calendario de festivos. En vez de dejarlo sin resolver o inventar fechas, se modeló como una interfaz consumida por `HorarioAtencionPolicy`; la implementación por defecto (`SinFestivosConfiguradosPolicy`) no marca ningún día como festivo. Conectar el calendario real de festivos colombianos el día de mañana es agregar una implementación nueva, sin tocar la lógica de horarios.
- **Límite conocido de RN-02/RN-04 bajo concurrencia**: a diferencia de la unicidad del documento del paciente (que sí tiene un constraint único en base de datos), la no-duplicidad de citas por médico/paciente en una misma franja se valida a nivel de servicio dentro de la transacción, porque la condición depende del `estado` de la cita (`PROGRAMADA`), no de una combinación de columnas siempre única — un índice único parcial no es portable entre motores de base de datos de forma sencilla. Es una limitación aceptada y documentada para el alcance de este MVP.
- **`CitaSpecifications` (patrón Specification) para RF-06**: el listado de citas tiene 4 filtros opcionales combinables entre sí (médico, paciente, estado, rango de fechas). En vez de un método de repositorio con banderas nulas y una cadena de `if` armando SQL a mano, cada filtro es una `Specification<Cita>` independiente que se compone con `.and(...)` solo si el parámetro fue enviado. Agregar un filtro nuevo en el futuro es sumar una `Specification` más, sin tocar las que ya existen (OCP) — y cada una se puede probar por separado.

## Ejecución local

**Requisitos:** Java 17+ y Maven 3.9+ (o usar el wrapper `mvnw` si se agrega).

```bash
# Clonar el repositorio
git clone <url-del-repositorio>
cd medisalud

# Ejecutar la aplicación (compila, aplica migraciones Flyway y levanta el servidor)
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Consola H2** (para inspeccionar la base de datos en memoria): `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:medisalud`
  - Usuario: `sa` — Password: *(vacío)*

Si el puerto 8080 está ocupado por otra aplicación en tu máquina, puedes levantarla en otro puerto sin modificar la configuración del proyecto:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

### Correr las pruebas

```bash
mvn test
```

### Empaquetar y ejecutar el jar

```bash
mvn clean package
java -jar target/medisalud-0.0.1-SNAPSHOT.jar
```

## Endpoints de la API

### Médicos (RF-01)

Registro y consulta de médicos disponibles en la clínica.

#### `POST /api/medicos` — Registrar un médico

**Request**

```http
POST /api/medicos
Content-Type: application/json

{
  "nombreCompleto": "Dr. Luis Torres",
  "especialidad": "Neurologia",
  "telefono": "300-555-7788",
  "email": "luis.torres@medisalud.com"
}
```

`telefono` y `email` son opcionales.

**Response — 201 Created**

```http
Location: /api/medicos/036b0c23-bc47-40ce-ae5c-33341b0c1297
Content-Type: application/json

{
  "id": "036b0c23-bc47-40ce-ae5c-33341b0c1297",
  "nombreCompleto": "Dr. Luis Torres",
  "especialidad": "Neurologia",
  "telefono": "300-555-7788",
  "email": "luis.torres@medisalud.com",
  "creadoEn": "2026-08-08T16:46:04.387115800Z"
}
```

**Response — 400 Bad Request** (validación fallida, ej. nombre muy corto)

```json
{
  "timestamp": "2026-08-08T16:46:04.556882300Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Los datos enviados no son validos",
  "path": "/api/medicos",
  "validationErrors": [
    { "field": "nombreCompleto", "message": "el nombre completo debe tener entre 3 y 100 caracteres" }
  ]
}
```

#### `GET /api/medicos` — Listar médicos

Parámetro de query opcional: `especialidad`.

```http
GET /api/medicos?especialidad=Cardiologia
```

**Response — 200 OK**

```json
[
  {
    "id": "a1a1a1a1-0001-4000-8000-000000000001",
    "nombreCompleto": "Dra. Maria Gonzalez",
    "especialidad": "Cardiologia",
    "telefono": "555-1001",
    "email": "maria.gonzalez@medisalud.com",
    "creadoEn": "2026-08-08T16:44:56.727672Z"
  }
]
```

#### `GET /api/medicos/{id}` — Consultar un médico por id

**Response — 200 OK**: igual estructura que el objeto anterior.

**Response — 404 Not Found**

```json
{
  "timestamp": "2026-08-08T16:46:04.662618200Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un medico con id 00000000-0000-0000-0000-000000000000",
  "path": "/api/medicos/00000000-0000-0000-0000-000000000000",
  "validationErrors": null
}
```

---

### Pacientes (RF-02)

Registro y consulta de pacientes. El **documento de identidad es único** en el sistema: intentar registrar dos pacientes con el mismo documento responde `409 Conflict`.

> `fechaNacimiento` es opcional en el registro (no está en el enunciado de RF-02), pero se agrega desde ya porque **RN-03** exige poder calcular la edad del paciente al momento de agendar una cita. Si no se informa, se asume edad 0 al agendar (se implementará junto con RF-03).

#### `POST /api/pacientes` — Registrar un paciente

**Request**

```http
POST /api/pacientes
Content-Type: application/json

{
  "nombreCompleto": "Juan Perez",
  "documentoIdentidad": "1002003004",
  "telefono": "3001234567",
  "email": "juan.perez@mail.com",
  "fechaNacimiento": "1990-05-20"
}
```

**Response — 201 Created**

```http
Location: /api/pacientes/2c2da614-6663-46b9-9dc6-a3c4b84368f0
Content-Type: application/json

{
  "id": "2c2da614-6663-46b9-9dc6-a3c4b84368f0",
  "nombreCompleto": "Juan Perez",
  "documentoIdentidad": "1002003004",
  "telefono": "3001234567",
  "email": "juan.perez@mail.com",
  "fechaNacimiento": "1990-05-20",
  "creadoEn": "2026-08-08T17:09:36.954067600Z"
}
```

**Response — 409 Conflict** (documento ya registrado)

```json
{
  "timestamp": "2026-08-08T17:09:37.177828200Z",
  "status": 409,
  "error": "Conflict",
  "message": "Ya existe un paciente registrado con el documento 1002003004",
  "path": "/api/pacientes",
  "validationErrors": null
}
```

**Response — 400 Bad Request** (ej. fecha de nacimiento futura)

```json
{
  "timestamp": "2026-08-08T17:09:37.300432500Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Los datos enviados no son validos",
  "path": "/api/pacientes",
  "validationErrors": [
    { "field": "fechaNacimiento", "message": "la fecha de nacimiento no puede ser futura" }
  ]
}
```

#### `GET /api/pacientes` — Listar pacientes

**Response — 200 OK**

```json
[
  {
    "id": "2c2da614-6663-46b9-9dc6-a3c4b84368f0",
    "nombreCompleto": "Juan Perez",
    "documentoIdentidad": "1002003004",
    "telefono": "3001234567",
    "email": "juan.perez@mail.com",
    "fechaNacimiento": "1990-05-20",
    "creadoEn": "2026-08-08T17:09:36.954068Z"
  }
]
```

#### `GET /api/pacientes/{id}` — Consultar un paciente por id

**Response — 200 OK**: igual estructura que el objeto anterior.

**Response — 404 Not Found**: mismo formato que en médicos.

#### `GET /api/pacientes/documento/{documentoIdentidad}` — Consultar un paciente por documento de identidad

Pensado para el flujo real de la clínica: quien agenda una cita normalmente tiene el documento del paciente a mano, no su id interno.

**Request**

```http
GET /api/pacientes/documento/80224567
```

**Response — 200 OK**

```json
{
  "id": "57e451c9-d46f-4d2d-b1ae-84289e27436c",
  "nombreCompleto": "Juan Perez",
  "documentoIdentidad": "80224567",
  "telefono": "3001234567",
  "email": "juan.perez@mail.com",
  "fechaNacimiento": null,
  "creadoEn": "2026-08-08T17:20:31.149026Z"
}
```

**Response — 404 Not Found**

```json
{
  "timestamp": "2026-08-08T17:20:31.786027100Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un paciente con documento de identidad 00000000",
  "path": "/api/pacientes/documento/00000000",
  "validationErrors": null
}
```

---

### Citas (RF-03)

Reserva de citas con control de disponibilidad. `fechaHora` va en formato ISO 8601 local (sin zona horaria, ej. `2026-08-10T09:00:00`), ya que los horarios de atención (RN-01) se definen en hora local de la clínica.

#### `POST /api/citas` — Reservar una cita

**Request**

```http
POST /api/citas
Content-Type: application/json

{
  "pacienteId": "6db9772d-7168-476c-b19d-79b49507bba2",
  "medicoId": "a1a1a1a1-0001-4000-8000-000000000001",
  "fechaHora": "2026-08-10T09:00:00"
}
```

**Response — 201 Created**

```http
Location: /api/citas/eeb98398-9816-43ce-8234-f168f579266b
Content-Type: application/json

{
  "id": "eeb98398-9816-43ce-8234-f168f579266b",
  "pacienteId": "6db9772d-7168-476c-b19d-79b49507bba2",
  "pacienteNombre": "Ana Torres",
  "medicoId": "a1a1a1a1-0001-4000-8000-000000000001",
  "medicoNombre": "Dra. Maria Gonzalez",
  "medicoEspecialidad": "Cardiologia",
  "fechaHora": "2026-08-10T09:00:00",
  "estado": "PROGRAMADA",
  "fechaCancelacion": null,
  "creadoEn": "2026-08-08T17:51:28.186928200Z"
}
```

**Response — 404 Not Found**: si `pacienteId` o `medicoId` no existen.

**Response — 400 Bad Request** (RN-01, RN-03 o el bloqueo de RN-05): por ejemplo, franja fuera del horario de atención.

```json
{
  "timestamp": "2026-08-08T17:51:49.038935600Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La fecha y hora debe corresponder a una franja de 30 minutos dentro del horario de atencion (lunes a viernes 08:00-18:00, sabados 08:00-13:00)",
  "path": "/api/citas",
  "validationErrors": null
}
```

**Response — 409 Conflict** (RN-02 o RN-04): el médico ya tiene una cita en esa franja, o el paciente ya tiene **cualquier** cita (con ese médico u otro) en esa misma franja.

```json
{
  "timestamp": "2026-08-08T17:51:48.692947900Z",
  "status": 409,
  "error": "Conflict",
  "message": "El paciente ya tiene una cita programada en esa franja horaria",
  "path": "/api/citas",
  "validationErrors": null
}
```

#### `GET /api/citas/{id}` — Consultar una cita por id

**Response — 200 OK**: igual estructura que el objeto anterior. **404 Not Found** si no existe.

---

### Disponibilidad (RF-04)

Consulta las franjas de 30 minutos disponibles de un médico en un rango de fechas. Reutiliza `HorarioAtencionPolicy` (la misma política de RN-01 que usa RF-03 al reservar) y descarta tanto las franjas ya ocupadas por una cita `PROGRAMADA` como las que ya pasaron.

#### `GET /api/citas/disponibilidad` — Consultar franjas disponibles

**Request**

```http
GET /api/citas/disponibilidad?medicoId=a1a1a1a1-0001-4000-8000-000000000001&fechaInicio=2026-08-10&fechaFin=2026-08-10
```

**Response — 200 OK**

```json
{
  "medicoId": "a1a1a1a1-0001-4000-8000-000000000001",
  "medicoNombre": "Dra. Maria Gonzalez",
  "medicoEspecialidad": "Cardiologia",
  "fechaInicio": "2026-08-10",
  "fechaFin": "2026-08-10",
  "franjasDisponibles": [
    { "horaInicio": "2026-08-10T08:00:00", "horaFin": "2026-08-10T08:30:00" },
    { "horaInicio": "2026-08-10T08:30:00", "horaFin": "2026-08-10T09:00:00" },
    { "horaInicio": "2026-08-10T09:30:00", "horaFin": "2026-08-10T10:00:00" }
  ]
}
```

*(en este ejemplo la franja `09:00-09:30` no aparece porque ya está reservada)*

**Response — 400 Bad Request** (falta un parámetro obligatorio, o `fechaFin` es anterior a `fechaInicio`)

```json
{
  "timestamp": "2026-08-08T19:08:35.645099400Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Falta el parametro obligatorio 'medicoId'",
  "path": "/api/citas/disponibilidad",
  "validationErrors": null
}
```

**Response — 404 Not Found**: si `medicoId` no existe.

---

### Cancelación (RF-05)

Cancela una cita `PROGRAMADA` y aplica RN-05: si la cancelación ocurre con **menos de 2 horas** de antelación a la hora programada de la cita (o la cita ya pasó), se registra una `Penalizacion` para el paciente. La respuesta indica explícitamente si se registró la penalización, para que el consumidor de la API no tenga que inferirlo.

#### `PUT /api/citas/{id}/cancelar` — Cancelar una cita

**Request**: sin cuerpo, solo el id de la cita en la URL.

```http
PUT /api/citas/5c415597-ecd7-4928-8f81-ab7ddc3ecdc7/cancelar
```

**Response — 200 OK**

```json
{
  "citaId": "5c415597-ecd7-4928-8f81-ab7ddc3ecdc7",
  "estado": "CANCELADA",
  "fechaCancelacion": "2026-08-08T19:26:47.210449200Z",
  "penalizacionRegistrada": false
}
```

**Response — 404 Not Found**: si la cita no existe.

**Response — 409 Conflict**: si la cita no está en estado `PROGRAMADA` (ya fue cancelada o ya fue atendida).

```json
{
  "timestamp": "2026-08-08T19:26:47.299510200Z",
  "status": 409,
  "error": "Conflict",
  "message": "Solo se pueden cancelar citas en estado PROGRAMADA",
  "path": "/api/citas/5c415597-ecd7-4928-8f81-ab7ddc3ecdc7/cancelar",
  "validationErrors": null
}
```

---

### Listado de citas (RF-06)

Lista citas con 4 filtros opcionales y combinables entre sí: `medicoId`, `pacienteId`, `estado` (`PROGRAMADA`, `CANCELADA`, `ATENDIDA`) y rango de fechas (`fechaInicio`, `fechaFin`). Sin filtros, devuelve todas las citas ordenadas por fecha.

#### `GET /api/citas` — Listar citas

**Request** (ejemplo: citas de un médico específico)

```http
GET /api/citas?medicoId=a1a1a1a1-0001-4000-8000-000000000001
```

**Response — 200 OK**

```json
[
  {
    "id": "a15639ae-65ab-4222-b901-69b6e46fff0d",
    "pacienteId": "e0e98fef-3eb8-46ed-9626-a39bb075b405",
    "pacienteNombre": "Marta Salas",
    "medicoId": "a1a1a1a1-0001-4000-8000-000000000001",
    "medicoNombre": "Dra. Maria Gonzalez",
    "medicoEspecialidad": "Cardiologia",
    "fechaHora": "2026-08-10T08:00:00",
    "estado": "CANCELADA",
    "fechaCancelacion": "2026-08-08T21:35:28.178601Z",
    "creadoEn": "2026-08-08T21:35:27.617602Z"
  },
  {
    "id": "07834953-4de9-4558-b87c-3bc5ab65fa55",
    "pacienteId": "ef998532-ffb9-44d7-82a3-1fd5ff62b7a7",
    "pacienteNombre": "Nestor Ibarra",
    "medicoId": "a1a1a1a1-0001-4000-8000-000000000001",
    "medicoNombre": "Dra. Maria Gonzalez",
    "medicoEspecialidad": "Cardiologia",
    "fechaHora": "2026-08-10T08:30:00",
    "estado": "PROGRAMADA",
    "fechaCancelacion": null,
    "creadoEn": "2026-08-08T21:35:27.926032Z"
  }
]
```

Otros ejemplos válidos: `GET /api/citas?estado=CANCELADA`, `GET /api/citas?pacienteId=...&fechaInicio=2026-08-01&fechaFin=2026-08-31`, o combinando los 4 filtros a la vez.

**Response — 400 Bad Request**: si `fechaFin` es anterior a `fechaInicio`.

## Reglas de negocio implementadas

| Regla | Estado | Dónde se aplica |
|---|---|---|
| RN-01 — Franjas de 30 min, horario laboral (L-V 08-18, Sáb 08-13, sin domingos/festivos) | ✅ | `HorarioAtencionPolicy`, al reservar (RF-03) y al consultar disponibilidad (RF-04) |
| RN-02 — Un médico no puede tener dos citas en la misma franja | ✅ | `CitaService.reservar` → `409` |
| RN-03 — Edad mínima 0 años, sin fechas de nacimiento futuras | ✅ | Validado en el registro del paciente (RF-02) y defensivamente al reservar |
| RN-04 — Un paciente no puede tener dos citas en la misma franja, sin importar el médico (conflicto global de agenda) | ✅ | `CitaService.reservar` → `409` |
| RN-05 (parte 1) — Bloquear el agendamiento si el paciente tiene 3+ penalizaciones en 30 días | ✅ | `CitaService.reservar` → `400` |
| RN-05 (parte 2) — Registrar la penalización al cancelar con menos de 2h de antelación | ✅ | `CitaService.cancelar` → `PUT /api/citas/{id}/cancelar` |
| RN-06 — Reprogramación (cancelar + crear nueva, validando disponibilidad) | ⏳ Pendiente | Se implementa a continuación |

*La sección de Citas se seguirá ampliando con RN-06 (reprogramación).*
