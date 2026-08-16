import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/products': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/orders': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/payments': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/notify': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    }
  }
})
