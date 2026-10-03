import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

const API_PREFIX = '/api';
const DEFAULT_API_TARGET = 'http://localhost:8090';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      proxy: {
        [API_PREFIX]: {
          target: env.VITE_API_TARGET || DEFAULT_API_TARGET,
          changeOrigin: true,
          rewrite: path => path.replace(new RegExp(`^${API_PREFIX}`), ''),
        },
      },
    },
  };
});
