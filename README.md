# Printlok PDP API

A **Spring Boot backend project** for Printlok PDP — designed with **domain-driven structure**, modular services, and clean separation of concerns. This backend provides authentication, user management, product catalog, ordering, notifications, and more.

 **Note:** The project is still **under active development**. Many features are incomplete or subject to change.

---

##  Tech Stack

- **Java 17+** - Modern Java runtime
- **Spring Boot 3** - Application framework
- **Spring Security** - JWT-based authentication & authorization
- **Hibernate/JPA** - Object-relational mapping
- **MySQL** - Database
- **Lombok** - Boilerplate code reduction
- **Maven** - Dependency management & build tool

---

##  Project Structure

```
pdp/
├── admin/
│   ├── AdminOrderController.java
│   ├── AdminService.java
│   └── dto/
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── dto/
│   │   ├── ForgotPasswordRequest.java
│   │   ├── JwtResponse.java
│   │   ├── LoginRequest.java
│   │   ├── PasswordConfirmationRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   └── SignUpRequest.java
│   ├── models/
│   │   ├── JwtBlacklist.java
│   │   ├── LoginAttemptId.java
│   │   ├── LoginAttempt.java
│   │   └── RefreshToken.java
│   └── repositories/
│       ├── JwtBlacklistRepository.java
│       ├── LoginAttemptRepository.java
│       └── RefreshTokenRepository.java
├── bootstrap/
│   ├── DataSeeder.java
│   └── RoleSeeder.java
├── catalog/
│   ├── dto/
│   │   ├── CategoryResponse.java
│   │   └── ProductResponse.java
│   ├── models/
│   │   ├── Category.java
│   │   └── Product.java
│   ├── ProductController.java
│   ├── ProductService.java
│   └── repositories/
│       ├── CategoryRepository.java
│       └── ProductRepository.java
├── common/
│   ├── dao/
│   │   └── UserDao.java
│   ├── dto/
│   │   └── ResponseStructure.java
│   ├── enums/
│   │   ├── AccountStatus.java
│   │   ├── ERole.java
│   │   ├── OrderStatus.java
│   │   └── RequestStatus.java
│   └── utils/
│       └── HashUtils.java
├── config/
│   ├── EmailConfig.java
│   └── SecurityConfig.java
├── exceptions/
│   ├── DuplicateEmailException.java
│   ├── DuplicatePhoneException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidCredentialsException.java
│   ├── InvalidRoleException.java
│   ├── TokenCompromiseException.java
│   ├── UnauthorizedException.java
│   └── UserNotFoundException.java
├── misc/
│   ├── DebugController.java
│   └── Hello.java
├── notification/
│   ├── EmailService.java
│   └── LinkGeneratorService.java
├── operator/
│   ├── dto/
│   │   └── OperatorResponse.java
│   ├── OperatorOrderController.java
│   └── OperatorService.java
├── order/
│   ├── dto/
│   │   ├── ApproveOrderRequest.java
│   │   ├── OrderItemRequest.java
│   │   ├── OrderItemResponse.java
│   │   ├── OrderRequest.java
│   │   └── OrderResponse.java
│   ├── models/
│   │   ├── OrderItem.java
│   │   └── Order.java
│   ├── OrderController.java
│   ├── OrderService.java
│   └── repositories/
│       ├── OrderItemRepository.java
│       └── OrderRepository.java
├── role/
│   ├── dto/
│   │   ├── RoleUpgradeRequest.java
│   │   └── RoleUpgradeResponse.java
│   ├── models/
│   │   ├── Role.java
│   │   └── RoleUpgradeRequest.java
│   ├── repositories/
│   │   ├── RoleRepository.java
│   │   └── RoleUpgradeRequestRepository.java
│   ├── RoleUpgradeRequestController.java
│   └── RoleUpgradeRequestService.java
├── security/
│   ├── AuthEntryPointJwt.java
│   ├── AuthTokenFilter.java
│   └── JwtUtils.java
├── user/
│   ├── dto/
│   │   ├── UserRequest.java
│   │   ├── UserResponse.java
│   │   └── UserUpdateRequest.java
│   ├── models/
│   │   └── User.java
│   ├── repositories/
│   │   └── UserRepository.java
│   ├── UserController.java
│   ├── UserDetailsImpl.java
│   ├── UserServiceImpl.java
│   └── UserService.java
└── PdpApplication.java
```

---

##  Features

###  Authentication & Authorization
- JWT-based authentication with refresh tokens
- Role-based access control (Admin, Operator, User)
- Login attempt tracking and rate limiting
- Password reset via email verification
- Token blacklisting for secure logout

###  User Management
- User registration and profile management
- Role upgrade request system
- Account status management
- User data validation and sanitization

###  Product Catalog
- Category and product management
- Product search and filtering
- Inventory tracking
- Admin product management interface

###  Order Management
- Order creation and tracking
- Order item management
- Status updates and approvals
- Admin and operator order interfaces

###  Notifications
- Email service integration
- Dynamic link generation
- Template-based notifications
- Password reset emails

###  Security Features
- Global exception handling
- Input validation
- SQL injection prevention
- CORS configuration
- Rate limiting

---

##  Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/printlok-pdp-backend.git
cd printlok-pdp-backend
```

### 2. Database Configuration
Create a MySQL database and update your `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/printlok
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
app.jwtSecret=printlokSecretKey
app.jwtExpirationMs=86400000

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 3. Run the Application
```bash
mvn clean install
mvn spring-boot:run
```

The backend will start at: **http://localhost:8080**

### 4. Initial Setup
The application will automatically seed initial roles and admin user on first run.

---

##  API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| `POST` | `/api/auth/signup` | Register new user | Public |
| `POST` | `/api/auth/login` | User authentication | Public |
| `POST` | `/api/auth/refresh` | Refresh JWT token | Public |
| `POST` | `/api/auth/logout` | User logout | Authenticated |
| `POST` | `/api/auth/forgot-password` | Request password reset | Public |
| `POST` | `/api/auth/reset-password` | Reset password | Public |

### User Management Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| `GET` | `/api/users/{id}` | Get user details | User/Admin |
| `PUT` | `/api/users/{id}` | Update user profile | User/Admin |
| `DELETE` | `/api/users/{id}` | Delete user account | Admin |
| `GET` | `/api/users` | List all users | Admin |

### Product Catalog Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| `GET` | `/api/products` | List all products | Public |
| `GET` | `/api/products/{id}` | Get product details | Public |
| `POST` | `/api/products` | Create new product | Admin |
| `PUT` | `/api/products/{id}` | Update product | Admin |
| `DELETE` | `/api/products/{id}` | Delete product | Admin |

### Order Management Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| `GET` | `/api/orders` | List user orders | User |
| `POST` | `/api/orders` | Create new order | User |
| `GET` | `/api/orders/{id}` | Get order details | User/Operator |
| `PUT` | `/api/orders/{id}/status` | Update order status | Operator/Admin |

---

##  Example Usage

### User Registration
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sandy",
    "email": "sandy@email.com",
    "password": "SecurePass123",
    "firstName": "Sandy",
    "lastName": "Kumar"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "User registered successfully",
  "data": {
    "userId": 1,
    "username": "sandy",
    "email": "sandy@email.com"
  }
}
```

### User Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sandy",
    "password": "SecurePass123"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "expiresIn": 86400,
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": 1,
      "username": "sandy",
      "email": "sandy@email.com",
      "roles": ["ROLE_USER"]
    }
  }
}
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "customization": "Custom text here"
      }
    ],
    "deliveryAddress": "123 Main St, City, State"
  }'
```

---

##  Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Coverage Report
```bash
mvn jacoco:report
```

---

##  Deployment

### Docker Support
```bash
# Build Docker image
docker build -t printlok-pdp-api .

# Run with Docker Compose
docker-compose up -d
```

### Environment Variables
```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=printlok
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=yourpassword
export JWT_SECRET=your-jwt-secret
export EMAIL_HOST=smtp.gmail.com
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

---

##  Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow Google Java Style Guide
- Write unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

---

##  Roadmap

### Phase 1 - Core Features 
- [x] Authentication system
- [x] User management
- [x] Basic security implementation
- [x] Database setup and models

### Phase 2 - Business Logic 
- [x] Product catalog
- [x] Order management
- [ ] Payment integration
- [ ] Inventory management

### Phase 3 - Advanced Features 
- [ ] Real-time notifications
- [ ] File upload/download
- [ ] Advanced reporting
- [ ] API rate limiting
- [ ] Caching implementation

### Phase 4 - Production Ready 
- [ ] Docker containerization
- [ ] CI/CD pipeline
- [ ] Performance optimization
- [ ] Security audit
- [ ] Comprehensive documentation

---

##  Known Issues

- Password reset email templates need improvement
- File upload functionality is not yet implemented
- Some error messages need localization
- Rate limiting is basic and needs enhancement

---

##  License

This project is private and proprietary. All rights reserved.

---

##  Support

For questions or support, please contact:
- **Email:** support@printlok.com
- **Issue Tracker:** [GitHub Issues](https://github.com/your-username/printlok-pdp-backend/issues)

---

##  Acknowledgments

- Spring Boot team for the excellent framework
- MySQL team for the robust database
- JWT.io for token standards
- All contributors and testers