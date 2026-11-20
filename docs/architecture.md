# System Architecture – Tax Gap Detection & Compliance Validation Service

## 1. Introduction

The Tax Gap Detection & Compliance Validation Service is a Java Spring Boot backend application designed to help tax auditors validate financial transactions, calculate expected tax, identify tax discrepancies and detect compliance violations.

The application follows a layered architecture that separates API handling, business logic, persistence and domain models.

## 2. Technology Stack

- Java 17
- Spring Boot 4.1.2
- Spring Web
- Spring Data JPA and Hibernate
- Spring Security
- PostgreSQL
- Maven
- JUnit 5 and Mockito
- JaCoCo

## 3. Application Architecture

The application follows the Controller → Service → Repository → Entity architecture.

### Controller Layer

The controller layer exposes REST APIs and handles incoming client requests.

The project includes:

- TransactionController
- ExceptionController
- ReportController

The transaction controller supports batch uploads and retrieval of transactions. The exception controller supports exception retrieval and filtering. The report controller provides customer tax reports and exception summary reports.

### Service Layer

The service layer contains the main business logic.

**TransactionService**

- Receives batches of transactions.
- Validates required fields, dates, amounts, tax rates and transaction types.
- Stores raw transaction data and validation results.
- Invokes tax calculation and rule evaluation for valid transactions.
- Records ingestion audit events.

**TaxCalculationService**

- Calculates expected tax using amount multiplied by tax rate.
- Calculates the tax gap.
- Determines compliance status.
- Records tax computation audit events.

**RuleEngineService**

- Retrieves active rules from the database.
- Reads JSON-based rule configurations.
- Executes high-value transaction, refund validation and GST slab validation.
- Creates exception records when rule violations are detected.
- Records rule execution audit events.

**AuditService**

- Creates audit log entries for important business operations.

**RuleService**

- Retrieves enabled tax rules from the database.

**ExceptionService and ReportService**

- Support exception retrieval and filtering.
- Generate customer-wise tax reports and exception summaries.

### Repository Layer

The repository layer uses Spring Data JPA to interact with PostgreSQL.

The project includes repositories for transactions, tax rules, tax exceptions, audit logs and application users.

### Entity Layer

The entity layer represents the application's database tables:

- Transaction
- TaxRule
- TaxException
- AuditLog
- AppUser

## 4. Database Design

The application uses PostgreSQL for persistent storage.

The main database tables are:

- `transactions`: transaction details, raw input, validation status, expected tax, tax gap and compliance status.
- `tax_rules`: rule names, enabled status and JSON configuration.
- `tax_exceptions`: detected compliance violations, severity and related transaction information.
- `audit_logs`: audit event type, transaction ID, timestamp and event details.
- `app_users`: usernames, encrypted password values, roles and enabled status.

The application uses JPA entities and repositories to manage persistence.

## 5. Transaction Processing Flow

1. A client submits a batch of financial transactions through the transaction upload API.
2. TransactionService reads and validates each transaction.
3. Raw transaction data is retained for traceability.
4. Invalid transactions are stored with FAILURE validation status and failure reasons.
5. Valid transactions receive SUCCESS validation status.
6. TaxCalculationService calculates expected tax, tax gap and compliance status.
7. RuleEngineService retrieves and executes active configured rules.
8. Detected violations create tax exception records.
9. AuditService records transaction ingestion, tax computation and rule execution events.
10. Processed transaction results are returned to the client.

## 6. Tax Compliance Rules

The rule engine supports database-configured rules.

**High-value transaction rule:** Creates an exception when the transaction amount exceeds the configured threshold.

**Refund validation rule:** Checks whether a refund exceeds the original sale amount and validates the original transaction reference.

**GST slab violation rule:** Checks whether the tax rate is below the configured minimum rate for the applicable amount slab.

Rules are stored with JSON configuration and an enabled/disabled status.

## 7. REST API Layer

The application exposes the following REST endpoints:

| Method | 			Endpoint | Purpose |
|---|---|---|
| POST | `/api/transactions/upload` | Upload a batch of transactions |
| GET | `/api/transactions` | Retrieve transactions |
| GET | `/api/exceptions` | Retrieve or filter exceptions |
| GET | `/api/exceptions/{id}` | Retrieve an exception by ID |
| GET | `/api/reports/customers/{customerId}` | Generate a customer tax report |
| GET | `/api/reports/exceptions` | Generate an exception summary |

## 8. Security

Spring Security is used for application authentication and authorization.

Application user information is maintained through the `app_users` table. The application initializer configures the initial administrator account, while password encoding is handled using BCrypt.

The exact access rules are defined in SecurityConfig.

## 9. Testing and Code Coverage

Unit tests are implemented using JUnit 5 and Mockito.

The tested service classes include:

- TaxCalculationService
- TransactionService
- RuleEngineService
- AuditService

The latest test execution completed with:

- 27 tests
- 0 failures
- 0 errors
- 0 skipped

JaCoCo coverage results:

- Overall instruction coverage: 54%
- Service package instruction coverage: 58%
- TransactionService: 74%
- RuleEngineService: 96%

## 10. Conclusion

The application provides a layered backend solution for transaction validation, tax gap calculation, configurable compliance rule execution, exception management, audit logging and reporting. Java, Spring Boot, Spring Data JPA and PostgreSQL support the application's business logic and persistent data management.