# CLAUDE.md — Warehouse Management System

Diese Datei definiert verbindliche Konventionen für dieses Projekt. Sie ist die Referenz für jede Code-Generierung oder -Review durch Claude in diesem Repository.

## Projektüberblick

Lagerverwaltungssystem (Warehouse Management), zweites Portfolio-Projekt nach `KoY877/helpdesk`. Ziel: echte Geschäftslogik (Bestandsführung mit Statusänderungen) statt reinem CRUD zeigen — analog zur Ticket-Status-State-Machine im Helpdesk-Projekt.

**Bewusst kein Copilot / keine automatische Codevervollständigung.** Jede Implementierung muss verstanden und erklärbar sein — Grundprinzip für Live-Coding-Interviews.

## Tech-Stack

| Bereich | Technologie |
|---|---|
| Backend | Spring Boot 4.1.0, Java 21 |
| Frontend | Angular 17+, TypeScript |
| Datenbank | PostgreSQL |
| Auth | Spring Security + JWT (Access + Refresh Token, DB-gestützt, Rotation) |
| Build | Maven |
| Infrastruktur | Docker Compose (postgres, backend, frontend) |

Group/Artifact: `io.github.koy877:warehouse`

## Architektur

Klassisches Monolith-Backend mit Schichtentrennung, wie im Helpdesk-Projekt:

```
controller  -> Request/Response, Validierung via @Valid
service     -> Geschäftslogik, Transaktionsgrenzen
repository  -> Spring Data JPA
entity      -> JPA-Entities, keine Geschäftslogik-Leaks nach außen
dto         -> Request-/Response-Objekte, nie Entities direkt exponieren
exception   -> GlobalExceptionHandler (400/401/403/404/409)
security    -> JWT-Filter, SecurityConfig
```

Frontend: Feature-basierte Modulstruktur, `AuthService` mit JWT-Interceptor (automatischer Refresh bei 401, nicht bei 403), `AuthGuard`, rollenbasiertes Routing — Wiederverwendung der Helpdesk-Konventionen.

## Datenmodell (MVP)

Entities: `User`, `Product`, `Location`, `Stock`, `StockMovement`.

- `Stock` ist eine eigene Entität (Bestand pro Produkt UND Lagerort), nicht nur aus Bewegungen berechnet.
- `StockMovement` erfasst jede Bestandsänderung (INBOUND, OUTBOUND, TRANSFER) mit Zeitstempel, ausführendem User und Referenz.
- IDs: UUID als String, konsistent mit Helpdesk-Projekt.

Vollständiges ERD: siehe Notion-Dokumentation "Projekt Entrepot - MVP Konzept".

## Geschäftsregeln

**TBD — noch nicht final entschieden, nicht ohne Rücksprache implementieren:**
Soll `POST /api/movements` bei Warenausgang eine Bestandsprüfung erzwingen (Ablehnung bei negativem Bestand, 409 Conflict) oder Negativbestand zulassen? Diese Regel muss vor der Service-Implementierung geklärt sein — analog zur dokumentierten Transition-Regel-Tabelle im Helpdesk-Projekt, die nicht ohne explizite Bestätigung geändert wird.

Sobald entschieden, wird die Regel hier als feste Tabelle dokumentiert und gilt als bindend.

## Rollen

- `ADMIN`: volle Verwaltung (Produkte, Lagerorte, User)
- `LAGERIST`: Bestand einsehen, Bewegungen buchen

## Sicherheit (non-negotiable, aus Helpdesk-Review übernommen)

- Kein Mass Assignment: Rollenfeld nie direkt aus Registrierungs-DTO übernehmen
- `@PreAuthorize` nicht auskommentiert lassen
- Keine `ResponseEntity`-Rückgabe aus der Service-Schicht (das ist Controller-Verantwortung)
- `@Valid` + `@NotNull` verpflichtend auf allen eingehenden DTOs
- Secrets nie ins Repo — `.env` bleibt gitignored, `.env.example` ohne echte Werte bleibt getrackt
- Vor jedem Commit: keine `launch.json` oder IDE-spezifischen Dateien mit sensiblen Daten

## Tests

Jede Service-Methode mit Geschäftslogik braucht einen Unit-Test (JUnit 5 + Mockito), wie im Helpdesk-Projekt (61 Tests). Kein Feature gilt als fertig ohne Test.

## Git-Hygiene

- VS Code vollständig schließen vor Branch-Wechsel-Kommandos (`.git/index.lock`-Konflikte vermeiden)
- Commit-Messages auf Englisch, konventionell (`feat:`, `fix:`, `refactor:`)

## Was noch fehlt (Stand dieser Konzeptionsphase)

- Entscheidung Bestandsprüfung (siehe oben)
- Repository-Setup (Monorepo, analog `KoY877/helpdesk`)
- Erste Entity-Implementierung