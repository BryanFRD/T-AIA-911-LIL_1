# Configuration des Variables d'Environnement

## Fichiers de configuration

### `.env`
Contient les variables d'environnement pour votre configuration locale (non versionné).

### `.env.example`
Template des variables d'environnement à copier pour créer votre `.env`.

## Variables disponibles

| Variable | Description | Exemple |
|----------|-------------|---------|
| `API_BASE_URL` | URL de base de l'API | `http://localhost:8080` |
| `API_TIMEOUT` | Timeout des requêtes API (ms) | `30000` |
| `NODE_ENV` | Environnement d'exécution | `development` |

## Utilisation

### 1. Créer votre fichier `.env`

```bash
cp .env.example .env
```

Puis modifiez les valeurs selon votre environnement.

### 2. Utiliser les variables dans le code

```typescript
import { API_BASE_URL, API_TIMEOUT } from "@env";

console.log("API URL:", API_BASE_URL);
```

### 3. Redémarrer le serveur

Après modification du `.env`, redémarrez Metro:

```bash
npm start -- --reset-cache
```

## Environnements multiples

### Développement local
```env
API_BASE_URL=http://localhost:8080
```

### Développement avec appareil physique
```env
API_BASE_URL=http://192.168.1.XXX:8080
```

### Production
```env
API_BASE_URL=https://api.votredomaine.com
```

## Notes importantes

- ⚠️ Ne commitez **jamais** le fichier `.env` (déjà dans `.gitignore`)
- ✅ Commitez toujours `.env.example` avec des valeurs par défaut
- 🔄 Redémarrez Metro après chaque modification du `.env`
- 📝 Les variables sont injectées au build time, pas au runtime
