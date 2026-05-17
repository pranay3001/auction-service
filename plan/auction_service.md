# Auction Service — Backend Implementation Plan

## Overview

Spring Boot 3.x service backing all 6 tabs of `auction-web`. Single PostgreSQL database
(`auction_db`). Docker Compose spins up Postgres + this service; the frontend reads
`VITE_AUCTION_API_URL` (default `http://localhost:8082/api/v1`).

---

## 1. Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Container | Docker Compose (Postgres + service) |

---

## 2. Docker Compose

```yaml
# backend/docker-compose.yml
services:
  postgres:
    image: postgres:16
    container_name: auction_postgres
    environment:
      POSTGRES_DB: auction_db
      POSTGRES_USER: optimile_user
      POSTGRES_PASSWORD: optimile_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U optimile_user"]
      interval: 5s
      retries: 5

  auction-service:
    image: maven:3.9-eclipse-temurin-17
    container_name: auction_service
    working_dir: /app
    volumes:
      - ./auction-service:/app
      - maven_cache:/root/.m2
    command: mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auction_db

volumes:
  postgres_data:
  maven_cache:
```

**Run:** `docker compose up` from `backend/`

---

## 3. Frontend Wiring

In `auction-web`, the `lib/api-client.ts` reads:

```ts
baseURL: import.meta.env.VITE_AUCTION_API_URL || 'http://localhost:8082/api/v1'
```

All requests attach `X-User-Id` header (seeded admin UUID) for dev. No auth
enforcement in dev profile — all endpoints return data regardless of token.

For local dev without Docker, set in `.env.local`:
```
VITE_AUCTION_API_URL=http://localhost:8082/api/v1
```

---

## 4. Database Schema

### 4.1 Enums (PostgreSQL native)

```sql
CREATE TYPE auction_type        AS ENUM ('SPOT', 'BULK', 'LOT');
CREATE TYPE auction_status      AS ENUM ('DRAFT', 'LIVE', 'COMPLETED', 'AWARDED', 'NO_BIDS', 'CANCELLED');
CREATE TYPE auction_event_type  AS ENUM ('CREATED', 'LAUNCHED', 'EXTENDED', 'COMPLETED',
                                          'AWARDED', 'CANCELLED', 'PLACEMENT_FAILURE',
                                          'OVERRIDE', 'CONTRACT_CREATED');
CREATE TYPE rate_unit           AS ENUM ('PER_TRIP', 'PER_MT', 'PER_KM');
CREATE TYPE allocation_mode     AS ENUM ('SINGLE', 'SPLIT');
CREATE TYPE allocation_rank     AS ENUM ('L1', 'L2', 'L3');
CREATE TYPE contract_status     AS ENUM ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'TERMINATED');
CREATE TYPE contract_type       AS ENUM ('BULK', 'LOT');
CREATE TYPE rfi_status          AS ENUM ('DRAFT', 'PUBLISHED', 'CLOSED');
CREATE TYPE rfq_status          AS ENUM ('DRAFT', 'PUBLISHED', 'EVALUATING', 'AWARDED', 'CANCELLED');
CREATE TYPE vendor_resp_status  AS ENUM ('PENDING', 'RESPONDED', 'DECLINED');
CREATE TYPE booking_status      AS ENUM ('PENDING_AUCTION', 'READY_FOR_DISPATCH');
CREATE TYPE notif_category      AS ENUM ('AUCTIONS', 'AWARDS', 'CONTRACTS', 'SOURCING', 'SYSTEM');
CREATE TYPE internal_role       AS ENUM ('ADMIN', 'OPS', 'PROCUREMENT', 'FINANCE', 'EXECUTIVE');
```

> **Note:** Hibernate sends VARCHAR for `@Enumerated(EnumType.STRING)`. Add implicit
> casts after schema creation:
> `CREATE CAST (varchar AS auction_status) WITH INOUT AS IMPLICIT;` — repeat for each type.

---

### 4.2 Tables

#### `internal_users`
```sql
id            UUID PK default gen_random_uuid()
name          VARCHAR(255) NOT NULL
email         VARCHAR(255) UNIQUE NOT NULL
password_hash VARCHAR(255) NOT NULL
mobile        VARCHAR(20)
role          internal_role NOT NULL default 'OPS'
status        VARCHAR(20) NOT NULL default 'ACTIVE'
created_at    TIMESTAMPTZ NOT NULL default now()
```
Used for: `createdById` on auctions/RFI/RFQ, `X-User-Id` header lookup.

#### `vendors`
```sql
id         UUID PK
name       VARCHAR(255) NOT NULL
email      VARCHAR(255)
score      NUMERIC(4,1)
created_at TIMESTAMPTZ NOT NULL default now()
```
Reference copy — sourced from vendor-service. Used for invited vendor lookup.

#### `bookings`
```sql
id           UUID PK
lane         VARCHAR(255) NOT NULL
vehicle_type VARCHAR(100)
commodity    VARCHAR(255)
quantity     NUMERIC(12,2)
uom          VARCHAR(50)
loading_date DATE
status       booking_status NOT NULL default 'PENDING_AUCTION'
created_at   TIMESTAMPTZ NOT NULL default now()
```

#### `auctions`
```sql
id                         UUID PK
title                      VARCHAR(500) NOT NULL
type                       auction_type NOT NULL
status                     auction_status NOT NULL default 'DRAFT'
created_by_id              UUID REFERENCES internal_users(id)
created_by_role            VARCHAR(50)
created_at                 TIMESTAMPTZ NOT NULL default now()
updated_at                 TIMESTAMPTZ NOT NULL default now()
start_at                   TIMESTAMPTZ
completed_at               TIMESTAMPTZ
contract_start_date        DATE
contract_end_date          DATE
min_bid_decrement          NUMERIC(12,2) default 0
extension_trigger_minutes  INT default 5
extension_duration_minutes INT default 10
max_extensions             INT default 3
bidding_window_minutes     INT default 60
booking_id                 UUID REFERENCES bookings(id) ON DELETE SET NULL
region                     VARCHAR(255)
award_deadline             TIMESTAMPTZ
```

#### `auction_lanes`
```sql
id                UUID PK
auction_id        UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE
lane              VARCHAR(500) NOT NULL
region            VARCHAR(255)
vehicle_type      VARCHAR(100)
capacity_mt       NUMERIC(12,2)
rate_unit         rate_unit NOT NULL default 'PER_TRIP'
ceiling_rate      NUMERIC(12,2)
estimated_trips   INT
base_price_source VARCHAR(20) default 'MANUAL'
allocation_mode   allocation_mode NOT NULL default 'SINGLE'
l1_allocation_pct NUMERIC(5,2)
l2_allocation_pct NUMERIC(5,2)
l3_allocation_pct NUMERIC(5,2)
timer_ends_at     TIMESTAMPTZ
extension_count   INT NOT NULL default 0
bid_count         INT NOT NULL default 0
rejection_reason  TEXT
created_at        TIMESTAMPTZ NOT NULL default now()
```

#### `auction_invited_vendors`
```sql
auction_id UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE
vendor_id  UUID NOT NULL
PRIMARY KEY (auction_id, vendor_id)
```

#### `auction_lane_eligible_vendors`
```sql
lane_id   UUID NOT NULL REFERENCES auction_lanes(id) ON DELETE CASCADE
vendor_id UUID NOT NULL
PRIMARY KEY (lane_id, vendor_id)
```

#### `auction_bids`
```sql
id         UUID PK
auction_id UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE
lane_id    UUID NOT NULL REFERENCES auction_lanes(id) ON DELETE CASCADE
vendor_id  UUID NOT NULL
vendor_name VARCHAR(255)
amount     NUMERIC(12,2) NOT NULL
bid_rank   INT
is_current BOOLEAN NOT NULL default true
placed_at  TIMESTAMPTZ NOT NULL default now()
```

#### `auction_events`
```sql
id              UUID PK
auction_id      UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE
event_type      auction_event_type NOT NULL
message         TEXT
actor           VARCHAR(255)
event_timestamp TIMESTAMPTZ NOT NULL default now()
```

#### `award_decisions`
```sql
id                 UUID PK
lane_id            UUID NOT NULL REFERENCES auction_lanes(id) ON DELETE CASCADE
auction_id         UUID NOT NULL REFERENCES auctions(id) ON DELETE CASCADE
vendor_id          UUID NOT NULL
vendor_name        VARCHAR(255)
allocation_rank    allocation_rank NOT NULL
awarded_bid_rank   allocation_rank
awarded_amount     NUMERIC(12,2) NOT NULL
override_reason    TEXT
allocation_percent NUMERIC(5,2)
decided_at         TIMESTAMPTZ NOT NULL default now()
```

#### `contracts`
```sql
id                        UUID PK
source_auction_id         UUID REFERENCES auctions(id) ON DELETE SET NULL
contract_type             contract_type
vendor_id                 UUID NOT NULL
vendor_name               VARCHAR(255)
lane                      VARCHAR(500)
region                    VARCHAR(255)
vehicle_type              VARCHAR(100)
contracted_rate           NUMERIC(12,2) NOT NULL
rate_unit                 rate_unit NOT NULL default 'PER_TRIP'
volume_allocation_percent NUMERIC(5,2)
allocation_rank           allocation_rank
start_date                DATE
end_date                  DATE
estimated_trips           INT
status                    contract_status NOT NULL default 'ACTIVE'
l1_override_reason        TEXT
rate_synced_to_tms        BOOLEAN NOT NULL default false
rate_deviation_open       BOOLEAN NOT NULL default false
created_at                TIMESTAMPTZ NOT NULL default now()
```

#### `placement_failures`
```sql
id                  UUID PK
contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE
failed_vendor       VARCHAR(255)
replacement_vendor  VARCHAR(255)
original_rate       NUMERIC(12,2)
replacement_rate    NUMERIC(12,2)
differential        NUMERIC(12,2)
debit_note_triggered BOOLEAN default false
recorded_at         TIMESTAMPTZ NOT NULL default now()
```

#### `rfis`
```sql
id                 UUID PK
title              VARCHAR(500) NOT NULL
description        TEXT
deadline           TIMESTAMPTZ
status             rfi_status NOT NULL default 'DRAFT'
message_to_vendor  TEXT
template_file_name VARCHAR(500)
created_by_id      UUID REFERENCES internal_users(id)
created_at         TIMESTAMPTZ NOT NULL default now()
updated_at         TIMESTAMPTZ NOT NULL default now()
```

#### `rfi_target_emails`
```sql
rfi_id UUID NOT NULL REFERENCES rfis(id) ON DELETE CASCADE
email  VARCHAR(255) NOT NULL
PRIMARY KEY (rfi_id, email)
```

#### `rfi_vendor_tracking`
```sql
id                  UUID PK
rfi_id              UUID NOT NULL REFERENCES rfis(id) ON DELETE CASCADE
vendor_id_or_email  VARCHAR(255) NOT NULL
name                VARCHAR(255)
status              vendor_resp_status NOT NULL default 'PENDING'
```

#### `rfqs`
```sql
id                 UUID PK
title              VARCHAR(500) NOT NULL
deadline           TIMESTAMPTZ
status             rfq_status NOT NULL default 'DRAFT'
message_to_vendor  TEXT
template_file_name VARCHAR(500)
created_by_id      UUID REFERENCES internal_users(id)
created_at         TIMESTAMPTZ NOT NULL default now()
updated_at         TIMESTAMPTZ NOT NULL default now()
```

#### `rfq_target_emails`
```sql
rfq_id UUID NOT NULL REFERENCES rfqs(id) ON DELETE CASCADE
email  VARCHAR(255) NOT NULL
PRIMARY KEY (rfq_id, email)
```

#### `rfq_vendor_tracking`
```sql
id                  UUID PK
rfq_id              UUID NOT NULL REFERENCES rfqs(id) ON DELETE CASCADE
vendor_id_or_email  VARCHAR(255) NOT NULL
name                VARCHAR(255)
status              vendor_resp_status NOT NULL default 'PENDING'
```

#### `rfq_responses`
```sql
id           UUID PK
rfq_id       UUID REFERENCES rfqs(id) ON DELETE SET NULL
vendor_name  VARCHAR(255)
file_name    VARCHAR(500)
uploaded_by  VARCHAR(255)
uploaded_at  TIMESTAMPTZ NOT NULL default now()
```

#### `rfq_response_rows`
```sql
id           UUID PK
response_id  UUID NOT NULL REFERENCES rfq_responses(id) ON DELETE CASCADE
lane         VARCHAR(500) NOT NULL
vehicle_type VARCHAR(100)
price        NUMERIC(12,2) NOT NULL
```

#### `notifications`
```sql
id         UUID PK
user_id    UUID NOT NULL
category   notif_category NOT NULL
title      VARCHAR(500) NOT NULL
message    TEXT
is_read    BOOLEAN NOT NULL default false
deep_link  VARCHAR(1000)
created_at TIMESTAMPTZ NOT NULL default now()
```

---

## 5. API Endpoints

Base path: `/api/v1`  
Dev auth: all requests accepted; `X-User-Id` header used to identify actor.

---

### Tab 1 — Dashboard (`GET /api/v1/dashboard`)

```
GET /api/v1/dashboard
```

**Response:**
```json
{
  "liveAuctions":       { "value": 2, "insight": "2 auctions currently accepting bids" },
  "pendingAwards":      { "value": 3, "insight": "3 auctions awaiting award confirmation" },
  "expiringContracts":  { "value": 4, "insight": "4 contracts expire within 30 days" },
  "priorityAuctions":   [ ...Auction[] — first 4 by updatedAt desc, status LIVE or COMPLETED ],
  "expiringContracts":  [ ...Contract[] — status EXPIRING_SOON ]
}
```

---

### Tab 2 — Client Hub (RFI & RFQ)

#### RFI

```
GET    /api/v1/rfis
       Query: ?status=PUBLISHED&search=text
       Response: RfiDto[]
       Fields: id, title, status, createdBy, createdAt, deadline,
               targetEmails[], vendorTracking[{vendorIdOrEmail, status}]

POST   /api/v1/rfis
       Body: { title, description, deadline, targetEmails[], messageToVendor, templateFileName }
       Headers: X-User-Id
       Action: create RFI with status=PUBLISHED, insert rfi_target_emails and
               rfi_vendor_tracking rows (status=PENDING) for each email
       Response: RfiDto

GET    /api/v1/rfis/:id
       Response: RfiDto (same as list item)

PATCH  /api/v1/rfis/:id/status
       Body: { status: "CLOSED" | "PUBLISHED" | "DRAFT" }
       Response: RfiDto
```

#### RFQ

```
GET    /api/v1/rfqs
       Query: ?status=PUBLISHED&search=text
       Response: RfqDto[]
       Fields: id, title, status, createdBy, createdAt, deadline,
               targetEmails[], vendorTracking[{vendorIdOrEmail, status}]

POST   /api/v1/rfqs
       Body: { title, deadline, targetEmails[], messageToVendor, templateFileName }
       Headers: X-User-Id
       Action: same pattern as RFI
       Response: RfqDto

GET    /api/v1/rfqs/:id
       Response: RfqDto

PATCH  /api/v1/rfqs/:id/status
       Body: { status }
       Response: RfqDto
```

---

### Tab 3 — RFQ Responses

```
GET    /api/v1/rfq-responses
       Query: ?search=text&from=YYYY-MM-DD&to=YYYY-MM-DD
       Response: flat array of response rows with vendor info
       Fields per row: id, lane, vehicleType, price, vendorName, uploadedAt, rfqId

POST   /api/v1/rfqs/:rfqId/responses
       Body: { fileName, vendorName?, rows: [{lane, vehicleType, price}] }
       Action: create rfq_responses + rfq_response_rows; mark rfq_vendor_tracking
               row for this vendor as RESPONDED if match found
       Response: { id, rfqId, fileName, vendorName, uploadedAt, rows[] }

GET    /api/v1/rfqs/:rfqId/responses
       Response: list of responses for one RFQ
```

---

### Tab 4 — Auctions

#### List & Create

```
GET    /api/v1/auctions
       Query: ?status=LIVE&type=SPOT&search=text
       Response: AuctionDto[]
       Fields: id, title, type, status, createdById, createdByRole, createdAt,
               startAt, completedAt, contractStartDate, contractEndDate,
               minBidDecrement, extensionTriggerMinutes, extensionDurationMinutes,
               maxExtensions, biddingWindowMinutes, bookingId, region, awardDeadline,
               invitedVendorIds[], lanes[] (summary: id, lane, vehicleType, bidCount)

POST   /api/v1/auctions
       Headers: X-User-Id
       Body:
       {
         title, type,
         bookingId?,           // SPOT only
         region?,              // LOT only
         minBidDecrement, extensionTriggerMinutes, extensionDurationMinutes,
         maxExtensions, biddingWindowMinutes,
         contractStartDate?, contractEndDate?,
         invitedVendorIds[],
         launchNow: boolean,
         lanes: [{
           lane, vehicleType, capacityMt, rateUnit, ceilingRate,
           estimatedTrips?,
           allocationMode,
           l1AllocationPct?, l2AllocationPct?, l3AllocationPct?,
           eligibleVendorIds[]
         }]
       }
       Action:
         - Insert auction (status = DRAFT or LIVE if launchNow)
         - Insert auction_lanes
         - Insert auction_invited_vendors
         - Insert auction_events: CREATED (+ LAUNCHED if launchNow)
         - If launchNow: set startAt=now, set timerEndsAt = now + biddingWindowMinutes
       Response: AuctionDto (full)
```

#### Detail

```
GET    /api/v1/auctions/:id
       Response: AuctionDto (full)
       Includes: lanes[]{...allLaneFields, ranking[{rank,vendorId,vendorName,amount,timestamp}],
                          awardDecision[]}, invitedVendorIds[], auditTrail[]
```

#### Lifecycle Actions

```
POST   /api/v1/auctions/:id/launch
       Headers: X-User-Id
       Precondition: status == DRAFT
       Action: status → LIVE, startAt = now,
               timerEndsAt = now + biddingWindowMinutes (per lane),
               insert auction_event LAUNCHED
       Response: AuctionDto

POST   /api/v1/auctions/:id/cancel
       Headers: X-User-Id
       Body: { reason? }
       Precondition: status in [DRAFT, LIVE, COMPLETED]
       Action: status → CANCELLED, insert auction_event CANCELLED
       Response: AuctionDto

POST   /api/v1/auctions/:id/complete
       Action: status → COMPLETED, completedAt = now, insert COMPLETED event
       Response: AuctionDto
```

#### Bids

```
GET    /api/v1/auctions/:auctionId/lanes/:laneId/bids
       Response: AuctionBidDto[] sorted by amount ASC (lowest = R1)
       Fields: id, vendorId, vendorName, amount, bidRank, placedAt

POST   /api/v1/auctions/:auctionId/lanes/:laneId/bids
       Body: { vendorId, vendorName, amount }
       Precondition: auction status == LIVE
       Action:
         - Validate amount ≤ ceilingRate
         - Validate amount decrement ≥ minBidDecrement vs current best bid
         - Insert bid (is_current = true), mark previous bids is_current = false
         - Recompute bid_rank for all is_current bids on this lane (rank by amount ASC)
         - Increment lane.bid_count
         - Check extension: if now > timerEndsAt - extensionTriggerMinutes AND
           extensionCount < maxExtensions → extend timer, increment extensionCount,
           insert EXTENDED event
       Response: AuctionBidDto
```

#### Award

```
POST   /api/v1/auctions/:auctionId/award
       Body: [{
         laneId, vendorId, vendorName,
         allocationRank,    // "L1" | "L2" | "L3"
         awardedBidRank,    // which bid rank was chosen
         awardedAmount,
         overrideReason?,
         allocationPercent
       }]
       Precondition: status in [LIVE, COMPLETED]
       Action:
         - Delete existing award_decisions for this auction
         - Insert new award_decisions
         - status → AWARDED
         - Insert AWARDED event (+ OVERRIDE event if any overrideReason)
       Response: AwardDecision[]

POST   /api/v1/auctions/:auctionId/finalize
       Precondition: status == AWARDED
       Action:
         - For each award_decision: create contract row
         - Insert CONTRACT_CREATED event per contract
         - Update booking status → READY_FOR_DISPATCH if bookingId present
       Response: ContractDto[]

POST   /api/v1/auctions/:auctionId/reject
       Body: { reason? }
       Action: status → NO_BIDS, insert event
       Response: AuctionDto
```

---

### Tab 5 — Contracts

```
GET    /api/v1/contracts
       Query: ?status=ACTIVE&search=text&vendorId=uuid
       Response: ContractDto[]
       Fields: id, sourceAuctionId, contractType, vendorId, vendorName,
               lane, region, vehicleType, contractedRate, rateUnit,
               volumeAllocationPercent, allocationRank, startDate, endDate,
               estimatedTrips, status, l1OverrideReason, rateSyncedToTms,
               rateDeviationOpen, placementFailures[]

GET    /api/v1/contracts/:id
       Response: ContractDto (same)

POST   /api/v1/contracts/:id/terminate
       Action: status → TERMINATED
       Response: ContractDto
```

---

### Tab 6 — Notifications

```
GET    /api/v1/notifications
       Headers: X-User-Id
       Query: ?isRead=false&category=AWARDS
       Response: NotificationDto[]
       Fields: id, category, title, message, isRead, deepLink, createdAt
       Order: createdAt DESC

PATCH  /api/v1/notifications/:id/read
       Headers: X-User-Id
       Action: is_read = true
       Response: 204 No Content

PATCH  /api/v1/notifications/read-all
       Headers: X-User-Id
       Action: is_read = true WHERE user_id = X-User-Id AND is_read = false
       Response: 204 No Content
```

---

### Supporting Lookups

```
GET    /api/v1/bookings
       Query: ?status=PENDING_AUCTION
       Response: BookingDto[] (id, lane, vehicleType, commodity, quantity, uom,
                               loadingDate, status)

GET    /api/v1/bookings/lookup
       Response: BookingDto[] with status=PENDING_AUCTION only (for create form dropdown)

GET    /api/v1/bookings/:id
       Response: BookingDto

GET    /api/v1/vendors
       Response: VendorDto[] (id, name, score)

GET    /api/v1/vendors/lookup
       Query: ?search=text
       Response: VendorDto[] (for invite vendors autocomplete)

GET    /api/v1/search
       Query: ?q=text
       Response: { auctions: [...], contracts: [...] }
```

---

## 6. Key Business Rules

### Bid Ranking
- All bids on a lane are ranked by amount ASC — lowest bid = Rank 1.
- Only `is_current = true` bids participate in ranking (one per vendor per lane).
- When a vendor re-bids, old bid is set `is_current = false`.

### Timer Extension
- If a bid arrives within `extensionTriggerMinutes` of `timerEndsAt` AND
  `extensionCount < maxExtensions`: add `extensionDurationMinutes` to `timerEndsAt`,
  increment `extensionCount`, write EXTENDED event.

### Award Flow
1. `POST /auctions/:id/award` — records decisions, status → AWARDED.
2. `POST /auctions/:id/finalize` — converts decisions to contracts, status stays AWARDED.
3. `POST /auctions/:id/reject` — no contracts created, status → NO_BIDS.

### SPOT vs BULK/LOT Award
- **SPOT**: single award decision (100% L1). One contract created on finalize.
- **BULK/LOT**: multiple lanes, each can have L1/L2/L3 split decisions.
  Each non-L1 selection requires `overrideReason`. One contract row per award decision.

### Contract Status Auto-Expiry
- `EXPIRING_SOON` = `endDate` within 30 days of today.
- Run a scheduled job (daily cron, `@Scheduled`) to update statuses.

### Notification Auto-Creation
Write a notification to `notifications` table whenever:
- Auction transitions to LIVE → category `AUCTIONS`
- Auction reaches COMPLETED → category `AWARDS`
- Award finalized → category `AWARDS`
- Contract status becomes EXPIRING_SOON → category `CONTRACTS`
- RFI/RFQ published → category `SOURCING`

---

## 7. Seed Data (Flyway V2)

Insert via `V2__seed_data.sql`:

```sql
-- 2 internal users
INSERT INTO internal_users (id, name, email, password_hash, role) VALUES
  ('71234567-...001', 'Arjun Mehta',  'ops@optimile.com', '$2a$...', 'OPS'),
  ('71234567-...002', 'Naina Kapoor', 'procurement@optimile.com', '$2a$...', 'PROCUREMENT');

-- 4 vendors
INSERT INTO vendors (id, name, score) VALUES
  ('f47ac10b-...-0001', 'SwiftHaul Logistics', 92),
  ('f47ac10b-...-0002', 'BlueAxle Freight',    88),
  ('f47ac10b-...-0003', 'RoadBridge Carriers', 84),
  ('f47ac10b-...-0004', 'MetroFleet Movers',   79);

-- 2 bookings (status PENDING_AUCTION)
-- 4 auctions across all types/statuses
-- Bids for the COMPLETED auctions (3 bids per lane ranking)
-- 4 contracts (2 ACTIVE, 2 EXPIRING_SOON)
-- 1 RFI (PUBLISHED), 1 RFQ (PUBLISHED)
-- 8 notifications (mix of categories, 3 unread)
```

---

## 8. Application Properties (dev profile)

```properties
# application-dev.properties
server.port=8082

spring.datasource.url=jdbc:postgresql://postgres:5432/auction_db
spring.datasource.username=optimile_user
spring.datasource.password=optimile_password

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# No auth enforcement in dev
spring.security.enabled=false

# CORS — allow auction-web on any port
cors.allowed-origins=http://localhost:5173,http://localhost:5174,http://localhost:3000

jwt.secret=optimile-secret-key-auction-service
jwt.expiration=86400000

logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
```

---

## 9. Project Structure

```
auction-service/
├── src/main/java/com/optimile/auction/
│   ├── AuctionServiceApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── SecurityConfig.java          # permit all in dev
│   ├── controller/
│   │   ├── BaseController.java          # extractUserId(request)
│   │   ├── DashboardController.java
│   │   ├── AuctionController.java
│   │   ├── BookingController.java
│   │   ├── VendorController.java
│   │   ├── ContractController.java
│   │   ├── RfiController.java
│   │   ├── RfqController.java
│   │   ├── RfqResponseController.java
│   │   ├── NotificationController.java
│   │   └── SearchController.java
│   ├── service/
│   │   ├── AuctionService.java
│   │   ├── BidService.java
│   │   ├── AwardService.java
│   │   ├── ContractService.java
│   │   ├── RfiService.java
│   │   ├── RfqService.java
│   │   ├── NotificationService.java
│   │   └── DashboardService.java
│   ├── entity/                          # JPA entities, one per table
│   ├── repository/                      # Spring Data JPA repos
│   ├── dto/                             # Request/Response DTOs
│   └── exception/
│       ├── ResourceNotFoundException.java
│       ├── BusinessException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── db/migration/
│       ├── V1__create_schema.sql
│       └── V2__seed_data.sql
└── pom.xml
```

---

## 10. Implementation Order

1. **Schema + Docker Compose** — V1 migration, docker-compose.yml, confirm `docker compose up` works
2. **Seed data** — V2 migration with realistic data matching frontend mock shapes
3. **Bookings + Vendors** — simple read-only GET endpoints, wire frontend dropdown
4. **Auctions CRUD** — GET list, GET detail, POST create, confirm frontend lists load
5. **Auction lifecycle** — launch, cancel, complete
6. **Bids** — place bid with ranking + timer extension logic
7. **Award flow** — award decisions, finalize → contracts
8. **Contracts** — GET list + detail, terminate
9. **Client Hub** — RFI/RFQ create + list + detail
10. **RFQ Responses** — upload + flat view
11. **Notifications** — auto-created on key transitions, mark read endpoints
12. **Dashboard** — aggregate KPIs from existing data
13. **Search** — simple ILIKE across titles

---

## 11. Testing

### 11.1 Backend — Unit Tests (JUnit 5 + Mockito)

Target: **~80% line + branch coverage** on all service classes.

**Stack:** JUnit 5, Mockito (), AssertJ.  
**Location:** 

| Class under test | Key scenarios to cover |
|---|---|
|  | list with status/type/search filters; create with launchNow=true vs false; launch validates DRAFT; cancel validates allowed statuses; complete sets completedAt |
|  | place bid — vendor first bid vs re-bid; ceiling rate rejection; min decrement rejection; bid rank recomputation (3 vendors); timer extension triggers at boundary; timer extension does NOT trigger when extensionCount == maxExtensions |
|  | award SPOT (single decision); award BULK/LOT (multi-lane, L1/L2/L3); finalize creates correct Contract count; SPOT finalize creates BULK contract; booking status updated on finalize; reject sets NO_BIDS |
|  | list with status/search/vendorId filters; terminate changes status only |
|  | ACTIVE → EXPIRING_SOON when endDate ≤ today+30; EXPIRING_SOON → EXPIRED when endDate < today; no change when endDate > today+30 |
|  | create inserts correct number of RfiTargetEmail + RfiVendorTracking rows; all tracking rows start PENDING |
|  | create inserts tracking rows; uploadResponse marks matching vendor RESPONDED; getAllResponses date range filter |
|  | each notify* method saves notification with correct category; getNotifications filters by isRead and category; markAllRead calls bulk query |
|  | KPI values match repo return counts; priority list capped at 4; expiring list reflects EXPIRING_SOON contracts |

### 11.2 Backend — Controller/Integration Tests (@WebMvcTest)

Target: all API endpoints return correct HTTP status + JSON shape.

**Stack:** , ,  for services.  
**Location:** 

| Controller | Endpoints to test |
|---|---|
|  | GET /auctions (200 + list); POST /auctions (201); GET /auctions/{id} (200 + 404); POST launch/cancel/complete (200 + precondition 400); GET/POST bids; POST award/finalize/reject |
|  | GET list; GET /{id} 200 + 404; POST terminate |
|  | GET list; POST create (201); GET /{id}; PATCH status |
|  | same pattern |
|  | GET /rfq-responses; POST /rfqs/{id}/responses; GET /rfqs/{id}/responses |
|  | GET (200); PATCH /{id}/read (204); PATCH /read-all (204) |
|  | GET / → 200 with all KPI fields present |
|  | 404 returns structured error body; 400 returns validation errors |

### 11.3 Frontend — Unit Tests (Vitest + React Testing Library)

Target: **~70% coverage** on service mappers + critical page render paths.

**Stack:** Vitest, React Testing Library,  (Mock Service Worker) for API mocking.  
**Location:** 

| Area | What to test |
|---|---|
|  |  — null  falls back correctly;  — null  returns [];  — nested  type field mapped from  |
|  |  — all fallback defaults applied |
|  |  +  — empty  returns [] |
|  component | renders correct colour class for each status (DRAFT/LIVE/COMPLETED/AWARDED/NO_BIDS/CANCELLED/ACTIVE/EXPIRING_SOON/EXPIRED/TERMINATED) |
|  component | renders value and insight text |
|  component | shows expired state when date is past; shows countdown when active |
|  | mocked  → 3 KPI values render; loading skeleton shows before resolve; error state shows on rejection |
|  | mocked  → rows render; tab filter changes query param |
|  | mocked  → title renders; Launch button calls ; disabled while saving |
|  | mocked  → rows render; Terminate button calls  |
|  | mocked  → unread count badge shown; Mark Read calls service |
|  | mocked  +  → both tabs render row counts |

### 11.4 Frontend — Playwright E2E Tests

Target: **5 critical user journeys** verified end-to-end against the running backend.

**Stack:** Playwright, .  
**Location:**   
**Prerequisite:** Backend running on , frontend on .

| Journey | Steps |
|---|---|
| **Full auction lifecycle** | Create SPOT auction (launchNow=false) → verify DRAFT in list → Launch → verify LIVE → Place 3 bids from different vendors → Complete → Award L1 → Finalize → verify Contract appears in Contracts tab |
| **Dashboard KPIs** | Load dashboard → verify Live Auctions count matches seeded LIVE auction → verify Expiring Contracts count matches seeded EXPIRING_SOON contracts |
| **RFI publish + sourcing** | Navigate to Client Hub → Create new RFI with 2 emails → verify appears in list with PUBLISHED status → verify vendor tracking shows 2 PENDING entries |
| **Notifications flow** | Launch an auction → navigate to Notifications tab → verify new AUCTIONS notification appears → click Mark Read → verify unread badge decrements → Mark All Read → verify badge clears |
| **Contract terminate** | Navigate to Contracts → open an ACTIVE contract → click Terminate → verify status changes to TERMINATED in list |

### 11.5 Coverage Targets Summary

| Layer | Tool | Target |
|---|---|---|
| Backend services | JUnit 5 + Mockito | 80% line + branch |
| Backend controllers | @WebMvcTest MockMvc | All endpoints 200/400/404 |
| Frontend mappers + components | Vitest + RTL | 70% line |
| Critical user journeys | Playwright | 5 flows green |

---

## 12. Vendor Portal Integration

auction-service serves **two portals**: `auction-web` (internal ops team) and `vendor-web` (external vendors). Vendor-facing endpoints live here because the data — auctions, bids, contracts — is owned by auction-service. vendor-service has no proxy; vendor-web calls auction-service directly at `VITE_AUCTION_API_URL=http://localhost:8082/api/v1`.

**Identity:** All vendor-facing requests carry `X-Vendor-Id: <UUID>` header (the logged-in vendor's UUID). The same UUID exists in the `vendors` table (reference copy seeded from vendor-service).

---

### 12.1 Sourcing Tab — Auction List

**Screen:** `vendor-web > Sourcing` — tabs: ALL / UPCOMING / LIVE / ENDED / CANCELLED

```
GET /api/v1/auctions?invitedVendorId={X-Vendor-Id}&status={status}&type={type}
Headers: X-Vendor-Id

New filter: invitedVendorId (UUID) — returns only auctions where vendor UUID is
  present in auction_invited_vendors for that auction_id.
Existing filters (status, type, search) still apply.

Response: AuctionDto[] (same shape as internal list)
  Each item includes: id, title, type, status, startAt, biddingWindowMinutes,
  invitedVendorIds[], lanes[] (summary: id, lane, vehicleType, bidCount)

Status mapping for vendor-web tabs:
  UPCOMING tab → status=DRAFT (not yet launched, vendor can preview)
  LIVE tab     → status=LIVE
  ENDED tab    → status=COMPLETED,AWARDED,NO_BIDS
  CANCELLED    → status=CANCELLED
  ALL tab      → no status filter
```

**Code changes required:**
- `AuctionRepository`: add `findByInvitedVendorIdAndOptionalStatus()` JPQL with `JOIN a.invitedVendors v WHERE v.id = :vendorId`
- `AuctionService.listAuctions()`: branch on `invitedVendorId != null`
- `AuctionController.listAuctions()`: add `@RequestParam(required=false) UUID invitedVendorId`

---

### 12.2 Sourcing Tab — Auction Detail & Bidding

**Screen:** `vendor-web > Sourcing > Auction Detail` — shows lane table, vendor's current bid, rank, bid input

```
GET /api/v1/auctions/{id}
Headers: X-Vendor-Id
Response: AuctionDto (full — existing endpoint, no change)
  Vendor uses: lanes[].ranking[] to find their own bid (match by vendorId)
  Vendor uses: lanes[].timerEndsAt for countdown display
  Vendor uses: lanes[].ceilingRate as bid upper bound

POST /api/v1/auctions/{auctionId}/lanes/{laneId}/bids
Headers: X-Vendor-Id
Body: { vendorId, vendorName, amount }   ← vendorId from X-Vendor-Id
Screen: Bid input row per lane; "Submit Bids" floating action bar (LOT: all lanes; SPOT: any lane)
Preconditions: auction status=LIVE; amount ≤ ceilingRate; decrement ≥ minBidDecrement
Response: AuctionBidDto (existing — no change)

GET /api/v1/auctions/{auctionId}/lanes/{laneId}/bids
Screen: Lane ranking table in Auction Detail
Response: AuctionBidDto[] sorted by amount ASC (existing — no change)
```

No code changes required for the bid and lane endpoints — they already work for vendor use.

---

### 12.3 Contracts Tab

**Screen:** `vendor-web > Contracts` — list of contracts awarded to this vendor; detail view shows rate card and allocation

```
GET /api/v1/contracts?vendorId={X-Vendor-Id}&status={status}&search={text}
Headers: X-Vendor-Id
Screen: vendor-web > Contracts (list) — tabs: ALL / ACTIVE / EXPIRED
Existing endpoint — vendorId filter already implemented. No change required.

Response: ContractDto[]
  Vendor-relevant fields: id, lane, vehicleType, contractedRate, rateUnit,
  allocationRank, volumeAllocationPercent, startDate, endDate, status,
  sourceAuctionId (links back to the auction that created this contract)

GET /api/v1/contracts/{id}
Headers: X-Vendor-Id
Screen: vendor-web > Contracts > Contract Detail
Existing endpoint — no change required.
Response: ContractDto (full, with placementFailures[])
```

No code changes required — both endpoints already exist and support `vendorId` filter.

---

### 12.4 Vendor Portal — Summary of Endpoint Changes

| Endpoint | Change | Screen |
|----------|--------|--------|
| `GET /api/v1/auctions?invitedVendorId=` | **NEW filter** — add to repo/service/controller | vendor-web > Sourcing (list) |
| `GET /api/v1/auctions/{id}` | None — existing endpoint | vendor-web > Sourcing (detail + bidding) |
| `POST /api/v1/auctions/{auctionId}/lanes/{laneId}/bids` | None — existing endpoint | vendor-web > Sourcing (place bid) |
| `GET /api/v1/auctions/{auctionId}/lanes/{laneId}/bids` | None — existing endpoint | vendor-web > Sourcing (lane rankings) |
| `GET /api/v1/contracts?vendorId=` | None — already implemented | vendor-web > Contracts (list) |
| `GET /api/v1/contracts/{id}` | None — existing endpoint | vendor-web > Contracts (detail) |
| `GET /api/v1/auctions?invitedVendorId=` | Same endpoint — vendor-web calls this on Dashboard load for live/upcoming counts | vendor-web > Dashboard (auction KPIs) |

---

### 12.5 Vendor Notifications — auction-service pushes to vendor-service

When an auction goes LIVE or a contract is created (finalize), auction-service calls vendor-service's internal endpoint to push notifications to all invited vendors.

**New component: VendorServiceClient.java**
```java
// auction-service/src/main/java/com/optimile/auction/client/VendorServiceClient.java

@Component
public class VendorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${vendor.service.url}")   // http://optimile_vendor_service:8083
    private String vendorServiceUrl;

    @Value("${vendor.service.key}")   // shared secret
    private String vendorServiceKey;

    public void pushNotification(List<UUID> vendorIds, String category,
                                  String title, String message, String deepLink) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Key", vendorServiceKey);
        Map<String, Object> body = Map.of(
            "vendorIds", vendorIds,
            "category", category,
            "title", title,
            "message", message,
            "deepLink", deepLink
        );
        try {
            restTemplate.exchange(
                vendorServiceUrl + "/api/v1/internal/notifications",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Void.class
            );
        } catch (Exception e) {
            // non-blocking — log and continue; auction operation must not fail
            log.warn("Failed to push notification to vendor-service: {}", e.getMessage());
        }
    }
}
```

**Where it is called in NotificationService:**
```java
// When auction goes LIVE → notifyAuctionLive(Auction auction)
vendorServiceClient.pushNotification(
    auction.getInvitedVendors().stream().map(Vendor::getId).toList(),
    "AUCTIONS",
    "Auction " + auction.getTitle() + " is now LIVE",
    "Bidding is open. Submit your bids before the timer ends.",
    "/vendor/sourcing/auctions/" + auction.getId()
);

// When auction finalized → notifyAuctionAwarded(Auction auction)
// Only notify vendors who won (have an award decision)
vendorServiceClient.pushNotification(winningVendorIds, "AUCTIONS", ...);
```

**New env vars in docker-compose.yml:**
```yaml
auction-service:
  environment:
    - VENDOR_SERVICE_URL=http://optimile_vendor_service:8083
    - VENDOR_SERVICE_KEY=optimile-internal-service-secret

vendor-service:
  environment:
    - VENDOR_SERVICE_KEY=optimile-internal-service-secret
```

**Failure handling:** wrapped in try-catch — vendor notification failure must never block auction lifecycle operations.
