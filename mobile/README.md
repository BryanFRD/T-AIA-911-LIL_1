# Mobile App

Application mobile React Native avec Expo, TypeScript, et TailwindCSS (NativeWind).

## 🚀 Quick Start

```bash
# Installer les dépendances
npm install

# Lancer l'app en développement
npm start

# Lancer sur Android
npm run android

# Lancer sur iOS
npm run ios
```

## 📁 Structure du Projet

```
mobile/
├── src/
│   ├── screens/          # Écrans de l'application
│   ├── components/       # Composants réutilisables
│   ├── navigation/       # Configuration de la navigation
│   ├── hooks/           # Custom React hooks (API calls)
│   ├── types/           # Types TypeScript
│   └── utils/           # Fonctions utilitaires
├── assets/              # Images, fonts, etc.
├── App.tsx             # Point d'entrée de l'app
└── .gitlab-ci.yml      # CI/CD GitLab
```

## 🛠️ Technologies

- **React Native** avec **Expo**
- **TypeScript** pour le typage statique
- **NativeWind** (Tailwind CSS pour React Native)
- **React Navigation** pour la navigation
- **TanStack Query** pour les appels API
- **Jest** + **React Native Testing Library** pour les tests

## 🧪 Tests

```bash
# Lancer les tests
npm test

# Tests en mode watch
npm run test:watch

# Générer le rapport de couverture
npm run test:coverage

# Vérification TypeScript
npm run type-check
```

## 📡 API Calls

L'application utilise TanStack Query pour gérer les appels API. Voir [README.md - Architecture](README.md) pour plus de détails.

Exemple d'utilisation :

```tsx
import { useUsers } from '../hooks/useUsers';

export default function MyScreen() {
  const { data, isLoading, error } = useUsers();

  if (isLoading) return <Text>Loading...</Text>;
  if (error) return <Text>Error: {error.message}</Text>;

  return <Text>{data?.length} users</Text>;
}
```

## 🎨 Styling avec NativeWind

Utilise les classes Tailwind directement avec le prop `className`:

```tsx
<View className="flex-1 bg-white items-center justify-center">
  <Text className="text-2xl font-bold text-blue-600">Hello</Text>
</View>
```

## 🔄 CI/CD

Le projet utilise GitLab CI/CD avec les stages suivants:
- **Test** : Tests unitaires + couverture
- **Quality** : Analyse SonarQube
- **Build** : Build Expo bundle

Voir [SETUP_CI_GITLAB.md](SETUP_CI_GITLAB.md) pour la configuration complète.

## 📦 Scripts Disponibles

| Script | Description |
|--------|-------------|
| `npm start` | Lance le serveur de développement Expo |
| `npm run android` | Lance l'app sur Android |
| `npm run ios` | Lance l'app sur iOS |
| `npm test` | Lance les tests Jest |
| `npm run test:coverage` | Tests avec couverture |
| `npm run type-check` | Vérification TypeScript |
| `npm run lint` | Linter ESLint |

## 📚 Documentation

- [Setup CI/CD GitLab](SETUP_CI_GITLAB.md) - Configuration complète de la CI/CD
- [Expo Documentation](https://docs.expo.dev/)
- [React Native Documentation](https://reactnative.dev/)
- [NativeWind Documentation](https://www.nativewind.dev/)

## 🏗️ Architecture

### Screens (`src/screens/`)
Un fichier par écran. Contient la logique et le layout de chaque page.

### Components (`src/components/`)
Composants UI réutilisables. Garder simples et modulaires.

### Hooks (`src/hooks/`)
Custom React hooks pour les appels API avec TanStack Query.

### Navigation (`src/navigation/`)
Configuration de React Navigation (tabs, stacks, etc.).

### Types (`src/types/`)
Interfaces et types TypeScript partagés.

### Utils (`src/utils/`)
Fonctions utilitaires, constantes, client API.

## 🚢 Déploiement

### Build Expo (Dev)
```bash
npx expo export --platform all
```

### Build Production avec EAS
```bash
# Installer EAS CLI
npm install -g eas-cli

# Login
eas login

# Configurer
eas build:configure

# Build Android
eas build --platform android

# Build iOS
eas build --platform ios
```

## 📄 License

Private
