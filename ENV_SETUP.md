# Guide de Configuration - Variables d'Environnement

## 🚀 Démarrage Rapide

### 1. Créer le fichier `.env`

```bash
cp .env.example .env
```

### 2. Configurer vos valeurs

Éditez `.env` et ajustez l'URL de l'API selon votre setup:

**Émulateur/Simulateur:**
```env
API_BASE_URL=http://localhost:8080
```

**Appareil physique (même réseau WiFi):**
```env
API_BASE_URL=http://192.168.1.X:8080
```
> Remplacez `192.168.1.X` par l'IP de votre machine

### 3. Redémarrer avec cache clear

```bash
npm run start:clear
```

## 📁 Fichiers Créés

```
mobile/
├── .env                    # Variables locales (non versionné)
├── .env.example           # Template (versionné)
├── env.d.ts              # Types TypeScript pour @env
├── babel.config.js       # ✅ Ajout du plugin react-native-dotenv
├── src/
│   ├── config/
│   │   └── index.ts      # Configuration centralisée
│   └── api/
│       ├── client.ts     # ✅ Utilise config depuis .env
│       └── README.md     # Documentation API client
└── docs/
    └── ENVIRONMENT.md    # Guide complet
```

## 🎯 Utilisation dans le Code

### Méthode 1: Via le config centralisé (recommandé)

```typescript
import config from "@/config";

console.log(config.api.baseURL);    // http://localhost:8080
console.log(config.api.timeout);    // 30000
console.log(config.env.isDevelopment); // true
```

### Méthode 2: Import direct

```typescript
import { API_BASE_URL } from "@env";

console.log(API_BASE_URL); // http://localhost:8080
```

## ✅ Améliorations Apportées

### Client Axios (src/api/client.ts)
- ✅ BaseURL depuis `.env`
- ✅ Timeout configurable
- ✅ Intercepteurs de logging en dev
- ✅ Headers par défaut
- ✅ Gestion d'erreurs

### Configuration (src/config/index.ts)
- ✅ Point central pour toute la config
- ✅ Types TypeScript stricts
- ✅ Valeurs par défaut sécurisées
- ✅ Flags d'environnement

### Package.json
- ✅ Script `npm run start:clear` ajouté
- ✅ react-native-dotenv installé

### Babel
- ✅ Plugin react-native-dotenv configuré
- ✅ Module alias `@env` disponible

## 🔧 Commandes Utiles

```bash
# Démarrer normalement
npm start

# Démarrer avec cache clear (après modif .env)
npm run start:clear

# Démarrer en mode tunnel
npm run start:tunnel

# Vérifier les types
npm run type-check
```

## 📝 Notes Importantes

1. **Redémarrer après changement .env**: Les variables sont injectées au build time
2. **Git**: `.env` est dans `.gitignore`, ne sera jamais commité
3. **Production**: Modifiez `.env` pour pointer vers l'API de prod
4. **CI/CD**: Configurez les variables d'environnement dans votre pipeline

## 🐛 Troubleshooting

### Les variables ne sont pas mises à jour
```bash
npm run start:clear
```

### Erreur "Cannot find module '@env'"
1. Vérifiez que `react-native-dotenv` est installé
2. Vérifiez `babel.config.js`
3. Redémarrez avec `--clear`

### TypeScript ne reconnaît pas les types
- Vérifiez que `env.d.ts` existe à la racine
- Redémarrez VS Code

## 📚 Ressources

- [Documentation react-native-dotenv](https://github.com/goatandsheep/react-native-dotenv)
- [Documentation Axios](https://axios-http.com/)
- [Guide complet](./docs/ENVIRONMENT.md)
