# WMS Shipping Mini-API

Schlanke Service-Funktionalität zum Versand von Aufträgen im Kontext eines WMS:
Auftrag erfassen → Sendung erzeugen → Paket(e) labeln (Tracking) → Versand bestätigen → Status einsehen.

## Voraussetzungen

- Java 21+
- Maven 3.9+ (oder das mitgelieferte `./mvnw`, falls vorhanden)
- Docker (für lokale Postgres-Instanz)

## Startanleitung

1. Postgres lokal starten:
   ```bash
   docker compose up -d
   ```
   Startet Postgres 16 auf `localhost:5432` mit DB `shipping`, User/Passwort `shipping`/`shipping`
   (siehe `docker-compose.yml`).

2. Anwendung starten:
   ```bash
   mvn spring-boot:run
   ```
   Die API läuft danach auf `http://localhost:8080`.

   Beim Start legt Hibernate (`ddl-auto: update`) das Schema automatisch an — für dieses MVP
   bewusst ohne Migrationstool (z. B. Flyway), um die Bearbeitungszeit im Rahmen zu halten.

3. Tests ausführen (benötigt laufendes Postgres):
   ```bash
   mvn test
   ```

## Domänenmodell

```
Order (Auftrag)
 ├─ externalOrderNumber   [unique, für Deduplizierung]
 ├─ deliveryAddress       [embedded: street, zipCode, city, country]
 ├─ status                [CREATED]
 ├─ positions: List<OrderPosition>  (1:n)
 └─ shipment: Shipment              (1:1, optional)

OrderPosition (Auftragsposition)
 ├─ sku, quantity, description

Shipment (Sendung)
 ├─ order: Order          [1:1, unique FK -> max. eine Sendung pro Auftrag]
 ├─ status                [CREATED -> PACKED -> SHIPPED]
 ├─ shippedAt             [gesetzt bei Versandbestätigung]
 └─ packages: List<ShipmentPackage> (1:n, im MVP genau 1 Eintrag)

ShipmentPackage (Paket)
 ├─ trackingCode          [unique, systemweit eindeutig]
 └─ carrier
```

**Zentrale Invarianten** (im Service-Layer geprüft, nicht nur per DB-Constraint):
- Dieselbe `externalOrderNumber` darf nicht mehrfach angelegt werden.
- Pro Auftrag maximal eine Sendung (`Shipment.order` ist unique).
- Für das MVP wird pro Sendung genau ein Paket angelegt (keine Kartonierung/Optimierung).
- Tracking-Code ist systemweit eindeutig (DB-Constraint + expliziter Check für saubere Fehlermeldung).
- Versandbestätigung (`PACKED -> SHIPPED`) nur möglich, wenn die Sendung `PACKED` ist **und**
  alle Pakete einen Tracking-Code haben.
- Statusübergänge laufen über dedizierte Endpunkte (`/pack`, `/ship`), nicht über einen freien
  Feld-Update, damit ungültige Sprünge (z. B. direkt `CREATED -> SHIPPED`) ausgeschlossen sind.

## API-Design

Basis-URL: `http://localhost:8080/api`

| Methode | Pfad | Zweck |
|---|---|---|
| `POST` | `/orders` | Auftrag anlegen |
| `GET`  | `/orders/{id}` | Auftragsdetails inkl. Positionen und (falls vorhanden) Sendungs-Kurzinfo |
| `POST` | `/orders/{id}/shipment` | Sendung aus Auftrag erzeugen (legt automatisch 1 Paket an) |
| `GET`  | `/shipments/{id}` | Sendungsdetails inkl. Pakete, Status, Tracking |
| `GET`  | `/shipments?status=&carrier=` | Sendungen suchen/filtern |
| `PATCH`| `/shipments/{id}/packages/{packageId}` | Paket labeln (Tracking-Code + Carrier setzen) |
| `POST` | `/shipments/{id}/pack` | Statusübergang `CREATED -> PACKED` |
| `POST` | `/shipments/{id}/ship` | Versand bestätigen (`PACKED -> SHIPPED`, prüft Invarianten, setzt `shippedAt`) |

### Beispiel-Ablauf (curl)

```bash
# 1. Auftrag anlegen
curl -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d '{
  "externalOrderNumber": "ORD-1001",
  "deliveryAddress": {"street":"Hauptstr. 1","zipCode":"44787","city":"Bochum","country":"DE"},
  "positions": [{"sku":"ART-1","quantity":2,"description":"Testartikel"}]
}'

# 2. Sendung erzeugen (orderId aus Schritt 1)
curl -X POST localhost:8080/api/orders/{orderId}/shipment

# 3. Paket labeln (shipmentId + packageId aus Schritt 2)
curl -X PATCH localhost:8080/api/shipments/{shipmentId}/packages/{packageId} \
  -H "Content-Type: application/json" -d '{"trackingCode":"DHL123456789","carrier":"DHL"}'

# 4. Packen bestätigen
curl -X POST localhost:8080/api/shipments/{shipmentId}/pack

# 5. Versand bestätigen
curl -X POST localhost:8080/api/shipments/{shipmentId}/ship

# 6. Status einsehen
curl localhost:8080/api/shipments/{shipmentId}
```

### Fehlerbehandlung

| HTTP-Status | Bedeutung |
|---|---|
| `400 Bad Request` | Validierungsfehler (z. B. fehlende Pflichtfelder) |
| `404 Not Found` | Order/Shipment/Package existiert nicht |
| `409 Conflict` | Doppelte `externalOrderNumber`, doppelter Tracking-Code, oder zweite Sendung zu einem Auftrag |
| `422 Unprocessable Entity` | Ungültiger Statusübergang bzw. Versand ohne vollständiges Tracking |

## Bewusste Vereinfachungen (MVP-Scope)

- Kein Auth/Security-Layer.
- Kein Flyway/Liquibase — Schema wird von Hibernate generiert (`ddl-auto: update`).
- Keine Pagination bei der Sendungssuche.
- Kartonierung/Paketoptimierung bewusst nicht umgesetzt (laut Vorgabe: 1 Paket je Sendung im MVP).

## Frontend (Angular, optional)

Ein schlankes Angular-Frontend liegt in `frontend/` und deckt den kompletten Ablauf ab:
Auftrag anlegen & suchen → Sendung erzeugen → Paket labeln → Packen/Versand bestätigen → Status einsehen.

### Starten

```bash
cd frontend
npm install
npm start
```

Läuft auf `http://localhost:4200`. Der Dev-Server proxied `/api/*` automatisch an das Backend
auf `http://localhost:8080` (siehe `frontend/proxy.conf.json`) — Backend muss also parallel laufen.

### Seiten

| Route | Inhalt |
|---|---|
| `/orders` | Auftragsübersicht + Formular zum Anlegen neuer Aufträge |
| `/orders/:id` | Auftragsdetails, Positionen, Button "Sendung erzeugen" |
| `/shipments` | Sendungssuche mit Filter nach Status/Carrier |
| `/shipments/:id` | Sendungsdetail mit Status-Pipeline, Paket-Labeling, Packen/Versand-Aktionen |

Für die Auftragsübersicht wurde backend-seitig ein zusätzlicher `GET /api/orders` Endpunkt ergänzt
(im ursprünglichen API-Design nicht explizit gefordert, aber für eine nutzbare Liste notwendig).
