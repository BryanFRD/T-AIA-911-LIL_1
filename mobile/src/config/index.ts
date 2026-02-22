import { API_BASE_URL, API_TIMEOUT, NODE_ENV } from "@env";

export const config = {
  api: {
    baseURL: API_BASE_URL,
    timeout: parseInt(API_TIMEOUT, 10) || 30000,
  },
  env: {
    isDevelopment: NODE_ENV === "development",
    isProduction: NODE_ENV === "production",
    nodeEnv: NODE_ENV,
  },
  app: {
    name: "Travel Order Resolver",
    version: "1.0.0",
  },
} as const;

export default config;
