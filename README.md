# KasiMarket

An on-demand task marketplace API for South Africa, built with Java and Spring Boot.

KasiMarket connects people who need everyday help (cleaning, laundry, dog walking, babysitting, house watching) with unemployed and under-employed people who are ready to work. Think of it as a ride-hailing app for tasks: the client books, the system sets the price, a verified worker accepts, and the job is tracked from start to payment.

## Why this project

South Africa has very high unemployment, and many people have no capital, no network and no track record to start earning. KasiMarket gives them a safe, regulated way to build a reputation, and uses that reputation to unlock higher-trust work.

## Key features

- **Accounts and roles:** clients, workers and admins, with passwords stored as BCrypt hashes and login tokens (JWT).
- **Vetted workers:** a worker applies and can only take jobs after an admin approves them.
- **Trust ladder:** tasks are tiered. Tier 1 and 2 are earned automatically from paid jobs and ratings. Tier 3 (babysitting, house watching) is never automatic: an admin must confirm references and an enhanced check, and the worker needs a strong track record.
- **System-set pricing:** the price comes from the task's hourly rate, a minimum booking length and an urgency surcharge. The platform fee and the worker's earnings are saved on the job at booking time.
- **First to accept wins:** accepting a job is one atomic database update, so two workers can never take the same job.
- **Job journey with rules:** accepted, on the way, arrived, in progress, completed, paid. Steps cannot be skipped, and every change is logged in a history table.
- **Proof of work:** a worker must upload a real JPEG or PNG photo to finish a job. The file type is checked from the file's contents, not its name.
- **Address privacy:** workers see a job's earnings first and the address only after accepting.
- **Cancelling and dropping:** a client can cancel and a worker can drop a job before the work starts. A dropped job returns to the open list.
- **Expiry:** open jobs nobody accepts expire automatically after a set time.
- **Ratings:** clients rate workers after a paid job, and the ratings feed the trust ladder.
- **Simulated payments:** payment records are real, but no money moves yet.

## Tech stack

Java 25, Spring Boot 4, Spring Security (JWT), Spring Data JPA (Hibernate), PostgreSQL (H2 for quick local runs), Maven, JUnit 5.

## Run it

You need a JDK (the project is set to Java 25) and Git.

```
git clone https://github.com/Phathiswa7/kasimarket-api.git
cd kasimarket-api
.\mvnw.cmd spring-boot:run
```

The app starts on http://localhost:8081 with an in-memory H2 database. The data is wiped on every restart.

### Keep your data with PostgreSQL (optional)

1. Create an empty PostgreSQL database called `kasimarket`.
2. Create the file `src/main/resources/application-postgres.properties` (it is ignored by Git, so your password stays private):

```
spring.datasource.url=jdbc:postgresql://localhost:5432/kasimarket
spring.datasource.username=postgres
spring.datasource.password=YOUR-PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

3. Start with the profile:

```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

### Test accounts (development only)

On startup the app creates these accounts so you can try it straight away. They exist only for development and must be removed before any real launch.

| Role | Email | Password |
|---|---|---|
| Admin | admin@kasimarket.com | Admin12345 |
| Client | sipho@example.com | Password123 |
| Worker (approved) | thandi@example.com | Password123 |
| Worker (approved) | zanele@example.com | Password123 |

## API overview

All addresses start with `/api/v1`. Everything except `auth` needs a token: `Authorization: Bearer <token>`.

| Method and path | Who | What it does |
|---|---|---|
| POST /auth/register, /auth/login | anyone | Create an account, get a token |
| GET /catalog/task-types | any logged-in user | List bookable tasks |
| POST /pricing/quote | client | Price a task before booking |
| POST /jobs | client | Book a job |
| GET /jobs/mine | client | The client's jobs |
| POST /jobs/{id}/cancel | client | Cancel before the work starts |
| POST /jobs/{id}/confirm | client | Confirm the work and pay |
| POST /jobs/{id}/rating | client | Rate the worker after payment |
| GET /jobs/available | worker | Open jobs the worker's tier allows |
| POST /jobs/{id}/accept | worker | Accept a job (first one wins) |
| POST /jobs/{id}/status | worker | Move to en route, arrived or in progress |
| POST /jobs/{id}/complete | worker | Finish with a completion photo |
| POST /jobs/{id}/drop | worker | Give a job back before the work starts |
| GET /jobs/my-work | worker | Jobs the worker accepted |
| GET /jobs/{id}/photo | client, worker, admin | View the completion photo |
| GET /admin/workers | admin | List workers, optionally by status |
| PUT /admin/workers/{id}/decision | admin | Approve, reject or suspend a worker |
| PUT /admin/workers/{id}/tier | admin | Grant tier 3 (or set back to 2) |

## Tests

```
.\mvnw.cmd test
```

Unit tests cover the pricing maths, the job journey rulebook and the tier rules.

## Design decisions

- **Business rules live in small, testable classes** (`PriceCalculator`, `JobStatus.canMoveTo`, `TierEvaluator`) so they can be tested without a database.
- **Prices are fixed at booking.** Changing a task's rate later never changes an old job.
- **Secrets stay out of Git.** The database password lives in a Git-ignored profile file.
- **Safety by design.** Sensitive tasks are gated by admin approval, addresses are hidden until acceptance, and the same error message is used for a wrong email and a wrong password.

## Roadmap

- Real payment provider and automatic payouts
- Real background-check provider
- Live location and push notifications
- Worker and client web or mobile front end
- Deployment on Azure

## Documentation

The project started with a full documentation pack (business plan, requirements, system design, safety and compliance, financial model). The financial figures are assumptions to be tested in a pilot.