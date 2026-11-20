# Tax Gap Detection & Compliance Validation Service

## 1. Project Overview

The Tax Gap Detection & Compliance Validation Service is a Java Spring Boot backend application designed to help tax auditors validate financial transactions, calculate expected tax, identify tax discrepancies, execute configurable compliance rules, generate exceptions, maintain audit logs and provide summary reports.

## 2. Technology Stack

- Java 17
- Spring Boot 4.1.2
- Spring Web
- Spring Data JPA
- Spring Security
- Hibernate
- PostgreSQL
- Maven
- JUnit 5
- Mockito
- JaCoCo

## 3. Main Features

- Batch transaction upload and validation
- Expected tax and tax gap calculation
- Compliance classification: COMPLIANT, UNDERPAID, OVERPAID and NON_COMPLIANT
- Database-driven configurable rule engine
- High-value transaction validation
- Refund validation
- GST slab violation validation
- Exception creation and filtering
- Audit logging
- Customer-wise tax reports
- Overall and customer-wise exception reports

## 4. Project Architecture

The application follows a layered architecture:

- Controller layer
- Service layer
- Repository layer
- Entity and DTO layer
- Configuration and security layer

## 5. Prerequisites

- Java 17 or later
- Maven
- PostgreSQL
- Spring Tool Suite or IntelliJ IDEA
- Postman or cURL

## 6. Database Setup

1. Install and start PostgreSQL.
2. Create a database for the application.
3. Configure the PostgreSQL connection in:

   `src/main/resources/application.properties`

4. Configure the database URL, username and password using your local environment settings.
5. Run the application so JPA/Hibernate can initialize the database according to the project's configuration.

Do not commit database credentials or other secrets to Git.

## 7. How to Run the Application

Clone the repository:

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
```

Navigate to the project directory:

```bash
cd tax-compliance-service
```

Build the application:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```

The default Spring Boot port is 8080 unless changed in the application configuration.

## 8. REST API Endpoints

Base URL:

`http://localhost:8080`

### 8.1 Upload a batch of transactions

- Method: POST
- Endpoint: `/api/transactions/upload`
- Content-Type: `application/json`
- Success status: 201 Created

Example:

```bash
curl -X POST http://localhost:8080/api/transactions/upload \
-H "Content-Type: application/json" \
-d @docs/sample-transaction-upload.json
```

The request body is an array of transaction objects.

### 8.2 Get all transactions

- Method: GET
- Endpoint: `/api/transactions`
- Success status: 200 OK

```bash
curl http://localhost:8080/api/transactions
```

### 8.3 Get all exceptions

- Method: GET
- Endpoint: `/api/exceptions`
- Success status: 200 OK

```bash
curl http://localhost:8080/api/exceptions
```

### 8.4 Filter exceptions

Supported query parameters:

- customerId
- severity
- ruleName

Example:

```bash
curl "http://localhost:8080/api/exceptions?customerId=CUS1001&severity=HIGH"
```

### 8.5 Get an exception by ID

- Method: GET
- Endpoint: `/api/exceptions/{id}`

Example:

```bash
curl http://localhost:8080/api/exceptions/1
```

### 8.6 Get customer-wise tax report

- Method: GET
- Endpoint: `/api/reports/customers/{customerId}`

Example:

```bash
curl http://localhost:8080/api/reports/customers/CUS1001
```

### 8.7 Get exception summary report

- Method: GET
- Endpoint: `/api/reports/exceptions`

Example:

```bash
curl http://localhost:8080/api/reports/exceptions
```

Customer-specific example:

```bash
curl "http://localhost:8080/api/reports/exceptions?customerId=CUS1001"
```

**Authentication note:** Spring Security is part of this application. Supply the credentials or authorization header configured by your actual SecurityConfig when an endpoint requires authentication. The examples above show the route and request format, not verified authentication credentials.

## 9. Sample Transaction Upload JSON

The sample request is available at:

`docs/sample-transaction-upload.json`

It contains SALE and REFUND transaction examples.

## 10. Tax Calculation Logic

Expected tax:

`expectedTax = amount * taxRate`

Tax gap:

`taxGap = expectedTax - reportedTax`

Compliance classification:

| Condition | Compliance status |
|---|---|
| Absolute tax gap <= 1 | COMPLIANT |
| Tax gap > 1 | UNDERPAID |
| Tax gap < -1 | OVERPAID |
| Reported tax missing | NON_COMPLIANT |

## 11. Configurable Rule Engine

The rule engine executes enabled rules stored in the database with JSON configuration.

Mandatory rules:

1. High-value transaction: flags transactions exceeding the configured threshold.
2. Refund validation: checks that refund amounts do not exceed the original sale amount.
3. GST slab violation: flags transactions where the tax rate is below the configured rate for the applicable amount slab.

## 12. Unit Testing and Code Coverage

JUnit 5 and Mockito are used for unit testing.

Tested service classes:

- TaxCalculationService
- TransactionService
- RuleEngineService
- AuditService

Latest test execution:

- Tests run: 27
- Failures: 0
- Errors: 0
- Skipped: 0

JaCoCo coverage:

- Overall instruction coverage: 54%
- Service package instruction coverage: 58%
- TransactionService: 74%
- RuleEngineService: 96%

Generate the report:

```bash
mvn clean verify
```

Report location:

`target/site/jacoco/index.html`

Coverage screenshots are available in the `docs` folder.

## 13. Project Documentation

The `docs` directory contains:

- `architecture.md`
- `database-schema.mmd`
- `database-schema.mmd.png`
- `sample-transaction-upload.json`
- `Overall_JaCoCo_Coverage.png`
- `Service_Layer_Coverage.png`

## 14. Conclusion

The application provides a layered backend solution for transaction validation, tax gap calculation, configurable compliance rule execution, exception management, audit logging and reporting using Java, Spring Boot and PostgreSQL.