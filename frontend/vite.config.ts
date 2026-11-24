import { defineConfig, type ProxyOptions } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

const backend = `http://localhost:${Number(process.env.SIH_VITE_PROXY_PORT ?? 8080)}`
const backendProxy: ProxyOptions = {
  target: backend,
  changeOrigin: true,
  configure(proxy) {
    proxy.on('proxyReq', (proxyReq, request) => {
      const { origin, host } = request.headers
      const incomingScheme = 'encrypted' in request.socket && request.socket.encrypted ? 'https' : 'http'
      if (host && origin === `${incomingScheme}://${host}`) {
        proxyReq.setHeader('origin', backend)
      }
    })
  },
}

export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: false,
    manifest: true,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [{ name: 'react-core', test: /node_modules\/(react|react-dom|scheduler)\// }],
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': backendProxy,
      '/actuator': backendProxy,
    },
  },
})
