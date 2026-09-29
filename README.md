# Rewards API

A Spring Boot REST API for a retailer's rewards program. It calculates the reward points each customer earned per month, and in total, over a three-month period.

## Reward rule

For each purchase, a customer earns:

- **2 points** for every dollar spent over $100, plus
- **1 point** for every dollar spent between $50 and $100.

Example: a $120 purchase earns 2 × 20 + 1 × 50 = **90 points**.

| Purchase | Points |
|---------:|-------:|
| $49.99   | 0      |
| $50.00   | 0      |
| $75.00   | 25     |
| $100.00  | 50     |
| $120.00  | 90     |
| $200.00  | 250    |

**Assumption:** points are earned on whole dollars only, so cents are ignored ($100.99 earns the same 50 points as $100.00). Money is held as `BigDecimal` to avoid floating-point errors.

## Tech stack

- Java 17
- Spring Boot 3.3 (Web, Data JPA)
- H2 in-memory database
- JUnit 5, Mockito, AssertJ, MockMvc for tests
- Maven (via the included Maven Wrapper, so no local Maven install is needed)

## How to run

Prerequisite: JDK 17 or newer.

```bash
git clone <your-repo-url>
cd rewards-api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`.

## How to run the tests

```bash
./mvnw test
```

## Endpoints

| Method | Path                         | Description                                          |
|--------|------------------------------|------------------------------------------------------|
| GET    | `/api/rewards`               | Monthly and total points for every customer          |
| GET    | `/api/rewards/{customerId}`  | Monthly and total points for one customer            |
| GET    | `/api/transactions`          | Every recorded transaction with the points it earned |

### The three-month period

By default, rewards are calculated for **the current month and the two months before it**, from the first day of the month two months ago up to today. The period is computed from the current date in the service layer. It does not depend on what data happens to be in the database, so older transactions are simply excluded.

Both rewards endpoints also accept an optional custom range (ISO dates, both required together):

```
GET /api/rewards/1?startDate=2026-07-01&endDate=2026-09-30
```

Every month in the period is always listed, with `0` points for months with no purchases, so a client can render the response directly without filling gaps.

### Example: GET /api/rewards/1

```bash
curl http://localhost:8080/api/rewards/1
```

```json
{
  "customerId": 1,
  "customerName": "Alice Johnson",
  "periodStart": "2026-07-01",
  "periodEnd": "2026-09-29",
  "monthlyRewards": [
    { "year": 2026, "month": "JULY",      "points": 115 },
    { "year": 2026, "month": "AUGUST",    "points": 50 },
    { "year": 2026, "month": "SEPTEMBER", "points": 250 }
  ],
  "totalPoints": 415
}
```

### Example: GET /api/rewards

```bash
curl http://localhost:8080/api/rewards
```

Returns an array with one object per customer, in the same shape as above.

### Example: GET /api/transactions

```bash
curl http://localhost:8080/api/transactions
```

```json
[
  {
    "transactionId": 2,
    "customerId": 1,
    "customerName": "Alice Johnson",
    "amount": 120.00,
    "transactionDate": "2026-07-05",
    "rewardPoints": 90
  }
]
```

### Errors

Errors use the standard problem-details JSON format.

| Situation                                    | Status |
|----------------------------------------------|--------|
| Customer id does not exist                   | 404    |
| `startDate` is after `endDate`               | 400    |
| Only one of `startDate` / `endDate` provided | 400    |
| Badly formatted date or non-numeric id       | 400    |

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Customer not found with id 99",
  "instance": "/api/rewards/99"
}
```

## Demo data

`DemoDataLoader` seeds the database on startup. Dates are relative to today, so the default period always contains data whenever you run the app. The data set is chosen to show the edge cases:

| Customer      | 2 months ago            | Last month              | This month   | Total |
|---------------|-------------------------|-------------------------|--------------|------:|
| Alice Johnson | $120 (90), $75 (25)     | $100 (50), $50 (0)      | $200 (250)   | 415   |
| Bob Smith     | $49.99 (0)              | $100.01 (50)            | $130.75 (110)| 160   |
| Carol Davis   | $250 (350)              | none                    | $55.50 (5)   | 355   |
| Dan Brown     | none                    | none                    | none         | 0     |

Alice also has a $300 purchase from **three** months ago, which is correctly excluded from her rewards.

You can browse the data at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:rewardsdb`, user `sa`, no password).

## Project structure

```
src/main/java/com/retailer/rewards
├── RewardsApplication.java
├── config/        ClockConfig (injectable clock), DemoDataLoader (seed data)
├── controller/    RewardController, TransactionController
├── dto/           Response records returned by the API
├── exception/     Custom exceptions and GlobalExceptionHandler
├── model/         JPA entities (Customer, Transaction) and RewardPeriod
├── repository/    Spring Data JPA repositories
└── service/       RewardPointsCalculator, RewardService, TransactionService
```

Design notes:

- The points rule lives in its own class, `RewardPointsCalculator`, so it can be tested on its own and changed in one place.
- `RewardService` takes a `java.time.Clock`, so tests can fix "today" and check the three-month window deterministically.
- The API returns response records (DTOs) rather than JPA entities, keeping the API contract separate from the database model.

## Tests

| Test class                           | What it covers                                                                 |
|--------------------------------------|--------------------------------------------------------------------------------|
| `RewardPointsCalculatorTest`         | The points rule at every boundary ($50, $100, cents, large and negative amounts) |
| `RewardServiceTest`                  | Grouping by month, totals, the default window, empty months, year boundaries, invalid ranges, unknown customers |
| `RewardControllerTest`               | HTTP responses, JSON shape, query parameters, and 400/404 errors               |
| `TransactionControllerTest`          | The transactions endpoint                                                      |
| `RewardsApplicationIntegrationTest`  | The full application against the seeded demo data                              |
