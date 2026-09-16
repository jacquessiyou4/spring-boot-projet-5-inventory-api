# Guide de test — API de Gestion d'un Inventaire de Produits

> Projet 5 sur 5. Ce guide décrit, pas à pas, comment lancer l'API depuis un
> terminal et vérifier qu'elle répond bien à chaque fonctionnalité demandée par
> le cahier des charges.

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Java (JDK) | 17 ou supérieur | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker + Compose | v2 | `docker compose version` |
| curl | — | `curl --version` |

## 2. Ports utilisés

| Service | Port hôte |
|---|---|
| API REST | **8080** |
| PostgreSQL | **5440** |
| Redis | **6381** |

> **Important — les 5 projets utilisent tous le port 8080.**
> Testez-les **un seul à la fois** et arrêtez toujours le précédent
> (`docker compose down -v`) avant de démarrer le suivant.

> Le port PostgreSQL hôte est volontairement décalé : le port 5432 est
> fréquemment occupé par une installation locale de PostgreSQL. Le conteneur de
> l'API, lui, joint la base par `postgres:5432` sur le réseau Docker interne —
> ce décalage ne concerne donc que vos propres connexions depuis la machine.

## 3. Lancement

### Méthode A — tout en Docker (au plus proche du rendu)

```bash
cd inventory-api
docker compose up -d --build
```

Le premier build télécharge l'image Maven et les dépendances : comptez
plusieurs minutes. Ensuite :

```bash
docker compose ps                 # les services doivent être "healthy"
docker compose logs -f inventory-api  # suivre le démarrage
```

### Méthode B — base en Docker, application en local (itération rapide)

```bash
cd inventory-api
docker compose up -d postgres redis

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5440/inventory_db \
  SPRING_DATA_REDIS_PORT=6381 \
  mvn spring-boot:run -Dspring-boot.run.fork=false
```

> `-Dspring-boot.run.fork=false` exécute l'application dans le processus Maven :
> un simple **Ctrl+C** l'arrête proprement. Sans cette option, Maven lance une
> JVM fille qui survit à l'arrêt de Maven et garde le port 8080 occupé.
>
> Attention : avec `fork=false`, l'option `-Dspring-boot.run.jvmArguments` est
> ignorée. Les réglages doivent passer par des **variables d'environnement**,
> comme ci-dessus.

### Vérifier que l'API est prête

```bash
curl -s http://localhost:8080/api/actuator/health
# {"status":"UP", ...}
```

## 4. Documentation Swagger (livrable exigé)

Ouvrez dans un navigateur :

    http://localhost:8080/api/swagger-ui.html

Le contrat OpenAPI brut est disponible sur `http://localhost:8080/api/v3/api-docs`.

## 5. Vérification de conformité au cahier des charges

Définissez d'abord l'URL de base :

```bash
B=http://localhost:8080/api
```

### Créer un produit (nom, prix, quantité en stock)

```bash
curl -s -X POST $B/products -H 'Content-Type: application/json' \
  -d '{"sku":"PHONE001","name":"Smartphone","category":"Electronics",
       "price":599.99,"initialStock":10,"minStockLevel":5}'
```
Attendu : **201 Created**. Un SKU déjà utilisé renvoie **400**.

### Afficher la liste des produits

```bash
curl -s $B/products
curl -s $B/products/1
```

### Mettre à jour un produit (prix, quantité, etc.)

```bash
curl -s -X PUT $B/products/1 -H 'Content-Type: application/json' \
  -d '{"sku":"PHONE001","name":"Smartphone Pro","category":"Electronics",
       "price":649.99,"initialStock":25,"minStockLevel":5,"unit":"UNIT"}'
```
`initialStock` fixe la **nouvelle quantité**. L'écart est journalisé comme
mouvement `ADJUSTMENT` :

```bash
curl -s $B/products/1/stock/movements
```

### Mouvements de stock

```bash
curl -s -X POST $B/products/1/stock/in -H 'Content-Type: application/json' \
  -d '{"quantity":30,"reason":"Réassort","reference":"PO-001"}'

curl -s -X POST $B/products/1/stock/out -H 'Content-Type: application/json' \
  -d '{"quantity":50,"reason":"Vente","reference":"CMD-001"}'
```
Les deux renvoient le **produit à jour**. Une sortie supérieure au stock
disponible renvoie **400** avec le détail des quantités.

### Alerte sur stock bas (seuil : 5 unités)

```bash
# Descendre sous le seuil
curl -s -X POST $B/products/1/stock/out -H 'Content-Type: application/json' \
  -d '{"quantity":3,"reason":"Vente"}'

curl -s $B/alerts             # alerte LOW_STOCK / WARNING
curl -s $B/alerts/low-stock
curl -s $B/alerts/critical    # alertes OUT_OF_STOCK / CRITICAL
```

Tomber à zéro remplace l'alerte `LOW_STOCK` par `OUT_OF_STOCK` (l'ancienne est
résolue automatiquement). Un réassort au-dessus du seuil résout les alertes
ouvertes : `curl -s $B/alerts` renvoie alors `[]`.

Résolution manuelle :
```bash
curl -s -X PUT $B/alerts/1/resolve
```

### Produits en stock bas ou en rupture

```bash
curl -s $B/products/low-stock
curl -s $B/products/out-of-stock
```

### Supprimer un produit

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $B/products/1   # 204
```

## 6. Tests automatisés

La suite complète s'exécute sans Docker ni réseau (base H2 en mémoire) :

```bash
mvn test
```

Résultat attendu : **38 tests, 0 échec**.

## 7. Arrêt et nettoyage

```bash
# Méthode A
docker compose down -v

# Méthode B : Ctrl+C sur l'application, puis
docker compose down -v
```

`-v` supprime aussi le volume de données : le projet suivant repart d'une base
vierge. **À faire systématiquement avant de tester un autre projet.**

## 8. En cas de problème

| Symptôme | Cause probable | Solution |
|---|---|---|
| `port is already allocated` | Un autre projet tourne encore | `docker compose down -v` dans le projet précédent |
| `Web server failed to start. Port 8080 was already in use` | Application précédente non arrêtée | `ss -ltnp \| grep :8080` puis arrêter le processus |
| `Connection refused` vers la base | Base pas encore prête | `docker compose ps` — attendre l'état `healthy` |
| L'application ignore vos réglages | `jvmArguments` avec `fork=false` | Utiliser des variables d'environnement |
| Swagger renvoie 404 | URL incomplète | Le contexte est `/api` : `/api/swagger-ui.html` |
