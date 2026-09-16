# Inventory API — Gestion d'un Inventaire de Produits

API Spring Boot 3 / Java 17 pour gérer un inventaire de produits avec suivi des stocks et **alertes de stock bas**.

> 📘 **Procédure de test pas à pas :** voir [GUIDE_DE_TEST.md](GUIDE_DE_TEST.md)
> — lancement depuis le terminal et vérification de chaque fonctionnalité
> exigée par le cahier des charges.

> 🔗 **Lien du dépôt GitHub :** voir [LIEN_GITHUB.md](LIEN_GITHUB.md)

## Prérequis
- Java 17, Maven 3.9+
- PostgreSQL 15, Redis 7 (ou les conteneurs fournis par Docker Compose)

## Démarrage rapide (Docker)
```bash
cd inventory-api
docker-compose up -d
# Swagger UI : http://localhost:8080/api/swagger-ui.html
```

## Démarrage local (sans Docker)
```bash
mvn spring-boot:run
# L'application lit application.yml (PostgreSQL localhost:5432, Redis localhost:6379)
```

## Comment tester l'API
1. **Créer un produit** — `POST /api/products`
   ```json
   {
     "sku": "PHONE001",
     "name": "Smartphone",
     "category": "Electronics",
     "price": 599.99,
     "initialStock": 10,
     "minStockLevel": 5
   }
   ```
   → `201 Created`. (Si `initialStock <= minStockLevel`, une alerte est créée immédiatement.)

2. **Lister / lire / mettre à jour / supprimer**
   - `GET /api/products`
   - `GET /api/products/{id}`
   - `PUT /api/products/{id}` (modifie prix, nom, SKU, seuils… **et la quantité** : renseignez `initialStock` avec la nouvelle quantité, l'écart est tracé comme mouvement `ADJUSTMENT` ; omettez le champ pour ne pas toucher au stock)
   - `DELETE /api/products/{id}`
   - `GET /api/products/low-stock` et `GET /api/products/out-of-stock`

3. **Suivi des stocks** — clés des mouvements
   - `POST /api/products/{id}/stock/in` — entrée de stock
     ```json
     { "quantity": 30, "reason": "Restock", "reference": "PO-001" }
     ```
   - `POST /api/products/{id}/stock/out` — sortie de stock (vérifie la disponibilité)

   Les deux renvoient `200` avec le **produit à jour** (stock courant inclus).
   - `GET /api/products/{id}/stock/movements`

4. **Alertes de stock bas (ex. seuil par défaut : 5 unités)**
   - `GET /api/alerts` — toutes les alertes actives
   - `GET /api/alerts/critical` — ruptures de stock
   - `GET /api/alerts/low-stock` — stocks bas
   - `PUT /api/alerts/{id}/resolve` — résoudre une alerte

> Les alertes sont automatiquement **créées** quand le stock passe sous le seuil (et re-résolues quand il remonte au-dessus). Une seule alerte active par type est maintenue, et une alerte devenue obsolète est résolue lors d'un changement de type (par ex. `LOW_STOCK` → `OUT_OF_STOCK`).

## Intégrité / concurrence
- Les mises à jour de stock utilisent un **verrou pessimiste d'écriture** (`SELECT ... FOR UPDATE`) pour éviter la survente et la perte de mises à jour concurrentes.
- Les endpoints renvoient des **DTO** (`ProductDTO`, `MovementDTO`, `StockAlertDTO`) et jamais les entités JPA.
- Les lectures produit sont mises en cache dans Redis (`products`) ; toute écriture (produit ou mouvement de stock) invalide le cache.

## Tests
```bash
mvn test
```