import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

  // Vite proxy for /api → Java backend (adjust port when backend is running)
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  // Vitest configuration
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
