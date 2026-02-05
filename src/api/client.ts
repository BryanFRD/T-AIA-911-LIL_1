import axios from "axios";
import config from "../config";

export const apiClient = axios.create({
  baseURL: config.api.baseURL,
  timeout: config.api.timeout,
  headers: {
    Accept: "*/*",
    "Content-Type": "application/json",
  },
});

if (config.env.isDevelopment) {
  apiClient.interceptors.request.use(
    (config) => {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => {
      console.error("[API Request Error]", error);
      return Promise.reject(error);
    },
  );

  apiClient.interceptors.response.use(
    (response) => {
      console.log(`[API Response] ${response.status} ${response.config.url}`);
      return response;
    },
    (error) => {
      console.error("[API Response Error]", error.response?.status, error.message);
      return Promise.reject(error);
    },
  );
}
