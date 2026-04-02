# Cinema Ticket Booking Service

Java implementation of a cinema ticketing system, built to handle ticket pricing and seat reservations while strictly following theater safety rules.

## Business Rules

- **Ticket Types:**
    - **INFANT:** £0 (Sits on an Adult's lap, no seat allocated).
    - **CHILD:** £15 (Allocated a seat).
    - **ADULT:** £25 (Allocated a seat).
- **Purchase Limits:** Maximum of **25 tickets** per transaction.
- **Safety Policy:** Child and Infant tickets **cannot** be purchased without at least one Adult ticket.
- **Valid Accounts:** Only accounts with an ID greater than zero are processed.

| Ticket Type | Price | Seat |
| :--- | :--- | :--- |
| **INFANT** | £0 | ❌ |
| **CHILD** | £15 | ✅ |
| **ADULT** | £25 | ✅ |

## Tech Stack & Constraints

- **Java 21:** Using modern Features like Records and Switch Expressions.
- **Immutability:** `TicketTypeRequest` is an immutable object to ensure data integrity.
- **Third-Party Services:** Integrates with `TicketPaymentService` and `SeatReservationService`.
- **Validation:** Uses a "Fail-Fast" approach with `InvalidPurchaseException`.

## Getting Started

### Prerequisites
- **JDK 21**
- **Maven 3.9+**

### Run Tests
To run the full suite of JUnit 5 tests:
```bash
mvn clean test
