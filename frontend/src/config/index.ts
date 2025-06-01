/**
 * Centralized configuration for the frontend application
 * All URLs and environment-specific settings are managed here
 */

interface AppConfig {
  api: {
    baseUrl: string;
    endpoints: {
      auth: {
        login: string;
        register: string;
        refresh: string;
        logout: string;
      };
      users: {
        base: string;
        profile: string;
      };
      tasks: {
        base: string;
        byId: (id: string) => string;
      };
      notifications: {
        stream: string;
      };
    };
  };
  app: {
    name: string;
    version: string;
    environment: string;
  };
  features: {
    enableSSE: boolean;
    enableRealTimeUpdates: boolean;
    debugMode: boolean;
  };
}

// Get configuration from environment variables with fallbacks
const getEnvVar = (key: string, defaultValue: string = ''): string => {
  return import.meta.env[key] || defaultValue;
};

// Base configuration
const createConfig = (): AppConfig => {
  const apiBaseUrl = getEnvVar('VITE_API_URL', 'http://localhost:8080/api');
  const sseUrl = getEnvVar('VITE_SSE_URL', 'http://localhost:8080/api/notifications/stream');
  const environment = getEnvVar('VITE_ENVIRONMENT', 'development');

  return {
    api: {
      baseUrl: apiBaseUrl,
      endpoints: {
        auth: {
          login: `${apiBaseUrl}/auth/login`,
          register: `${apiBaseUrl}/auth/register`,
          refresh: `${apiBaseUrl}/auth/refresh`,
          logout: `${apiBaseUrl}/auth/logout`,
        },
        users: {
          base: `${apiBaseUrl}/users`,
          profile: `${apiBaseUrl}/users/profile`,
        },
        tasks: {
          base: `${apiBaseUrl}/tasks`,
          byId: (id: string) => `${apiBaseUrl}/tasks/${id}`,
        },
        notifications: {
          stream: sseUrl,
        },
      },
    },
    app: {
      name: 'Task Management App',
      version: getEnvVar('VITE_APP_VERSION', '1.0.0'),
      environment,
    },
    features: {
      enableSSE: getEnvVar('VITE_ENABLE_SSE', 'true') === 'true',
      enableRealTimeUpdates: getEnvVar('VITE_ENABLE_REAL_TIME', 'true') === 'true',
      debugMode: environment === 'development',
    },
  };
};

// Export the configuration instance
export const config = createConfig();

// Export individual sections for convenience
export const apiConfig = config.api;
export const appConfig = config.app;
export const featureConfig = config.features;

// Utility functions
export const getApiUrl = (path: string = ''): string => {
  return path.startsWith('/') ? `${config.api.baseUrl}${path}` : `${config.api.baseUrl}/${path}`;
};

export const isProduction = (): boolean => {
  return config.app.environment === 'production';
};

export const isDevelopment = (): boolean => {
  return config.app.environment === 'development';
};

// Log configuration in development
if (isDevelopment()) {
  console.log('App Configuration:', config);
}

export default config;
