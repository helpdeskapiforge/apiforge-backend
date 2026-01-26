# APIForge - Backend 🛡️

The **APIForge Backend** is the robust engine behind the APIForge platform. Built with **Java Spring Boot**, it handles authentication, data persistence, request proxying, and the core logic for the **Dynamic Mock Simulator**.

It is designed to be stateless, scalable, and secure, utilizing **JWT** for session management and **PostgreSQL** for relational data integrity and reliability.

---

## ⚡️ Core Engines

### 1. 🎭 Dynamic Mock Simulator

This is the heart of APIForge. When a user hits a mock endpoint (e.g., `/api/mock/simulator/{path}`):

* **Route Matching:** It looks up the user-defined route configuration from the PostgreSQL database using efficient JPA queries.
* **Latency Injection:** If a delay is configured (e.g., 2000ms), the thread sleeps to simulate slow networks.
* **Chaos Monkey:** If enabled, it calculates probability (e.g., 10% failure rate) and intentionally throws 500 errors to test client resilience.
* **Response Generation:** Returns the user-defined JSON body and headers.

### 2. 🌐 Request Proxy Service

To bypass CORS issues when testing third-party APIs from the browser:

* The frontend sends the request details to the backend.
* The backend creates a real HTTP request (using `RestTemplate` or `WebClient`).
* It executes the call, captures the exact timing, status, and size, and returns the metadata to the frontend.

### 3. 🔐 Security & Auth

* **Spring Security:** Stateless session management.
* **JWT (JSON Web Tokens):** Secure token generation and validation filters for protected routes.
* **Password Hashing:** BCrypt encryption for user credentials.

---

## 🛠️ Tech Stack

* **Language:** Java 17+
* **Framework:** Spring Boot 3.x
* **Database:** PostgreSQL 14+
* **ORM:** Spring Data JPA / Hibernate
* **Security:** Spring Security & JWT
* **Build Tool:** Maven

---

## 🚀 Getting Started

Follow these steps to run the backend locally.

### Prerequisites

* **JDK 17** or higher installed.
* **Maven** installed (or use the included `mvnw` wrapper).
* **PostgreSQL** running locally or a cloud instance (e.g., Supabase, Neon, AWS RDS).

### 1. Clone the Repository

```bash
git clone https://github.com/helpdeskapiforge/apiforge-backend.git
cd apiforge-backend

```

### 2. Configuration

Create an `application.properties` file in `src/main/resources/` (or rename `application.example.properties`) and configure your environment variables:

```properties
# Server Configuration
server.port=8080

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/apiforge_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false

# JWT Secret Key (Must be 256-bit / 32+ characters)
app.jwt.secret=YourSuperSecretKeyForSigningTokens12345!

# JWT Expiration (in milliseconds, e.g., 24 hours)
app.jwt.expiration=86400000

# CORS Configuration (Allow Frontend URL)
app.cors.allowed-origins=http://localhost:3000,https://apiforge-frontend.vercel.app

```

### 3. Build the Project

```bash
# Using Maven Wrapper (Recommended)
./mvnw clean install

```

### 4. Run the Application

```bash
./mvnw spring-boot:run

```

The server will start on `http://localhost:8080`.

---

## 🔌 API Endpoints (Overview)

| Module | Method | Endpoint | Description |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/api/auth/signup` | Register a new user |
| **Auth** | `POST` | `/api/auth/signin` | Login and get JWT |
| **Mocks** | `POST` | `/mocks/servers` | Create a new Mock Server |
| **Routes** | `POST` | `/mocks/routes` | Add a route to a server |
| **Simulator** | `ALL` | `/api/mock/simulator/**` | **The Simulation Endpoint** (Matches any path) |
| **Proxy** | `POST` | `/proxy/execute` | Execute a real HTTP request |

---

## 🐳 Docker Support

You can containerize the backend easily. A `Dockerfile` is included in the root.

```bash
# Build the image
docker build -t apiforge-backend .

# Run the container (Ensure DB_URL points to a reachable Postgres instance)
docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/apiforge_db apiforge-backend

```

---

## 🤝 Contributing

1. Fork the repo.
2. Create your feature branch (`git checkout -b feature/cool-feature`).
3. Commit your changes.
4. Push to the branch.
5. Create a Pull Request.

---

Made with ☕️ and Java by [Sumit Shresht](https://github.com/sumitshresht)
