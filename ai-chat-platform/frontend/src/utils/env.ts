const readEnv = (key: string): string => {
  const value = import.meta.env[key];

  if (!value) {
    throw new Error(`Missing required environment variable: ${key}`);
  }

  return value;
};

export const env = {
  apiBaseUrl: readEnv('VITE_API_BASE_URL'),
  wsUrl: readEnv('VITE_WS_URL'),
  supabaseUrl: readEnv('VITE_SUPABASE_URL'),
  supabaseAnonKey: readEnv('VITE_SUPABASE_ANON_KEY'),
};
