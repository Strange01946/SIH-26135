import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            const origin = req.headers['origin'];
            if (origin && typeof origin === 'string') {
              proxyReq.setHeader('origin', origin.replace(/\/+$/, ''));
            }
          });
        },
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            const origin = req.headers['origin'];
            if (origin && typeof origin === 'string') {
              proxyReq.setHeader('origin', origin.replace(/\/+$/, ''));
            }
          });
        },
      },
    },
  },
})
