# API Client

Client Axios configuré pour les appels API de l'application.

## Configuration

Le client utilise les variables d'environnement définies dans `.env`:

```typescript
import { apiClient } from "@/api/client";

// Le client est déjà configuré avec:
// - baseURL depuis API_BASE_URL
// - timeout depuis API_TIMEOUT  
// - headers par défaut
// - intercepteurs de logging en développement
```

## Utilisation

### GET Request

```typescript
import { apiClient } from "@/api/client";

const fetchData = async () => {
  try {
    const response = await apiClient.get("/endpoint");
    return response.data;
  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
};
```

### POST Request

```typescript
const createData = async (data: MyData) => {
  try {
    const response = await apiClient.post("/endpoint", data);
    return response.data;
  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
};
```

### Upload de fichier (FormData)

```typescript
const uploadFile = async (file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await apiClient.post("/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
};
```

## Intercepteurs

### En développement

Les intercepteurs de logging sont automatiquement activés en mode développement:

- ✅ Log des requêtes sortantes (méthode + URL)
- ✅ Log des réponses (status + URL)
- ✅ Log des erreurs avec détails

### Personnalisation

Ajoutez vos propres intercepteurs selon vos besoins:

```typescript
// Ajouter un token d'authentification
apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Gérer les erreurs globalement
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Rediriger vers login
    }
    return Promise.reject(error);
  }
);
```

## Variables d'environnement

Consultez [docs/ENVIRONMENT.md](../docs/ENVIRONMENT.md) pour la configuration complète.
