import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth': { target: 'http://localhost:8085', rewrite: p => p.replace(/^\/api\/auth/, '/auth') },
      '/api/accounts': { target: 'http://localhost:8081', rewrite: p => p.replace(/^\/api\/accounts/, '/accounts') },
      '/api/items': { target: 'http://localhost:8082', rewrite: p => p.replace(/^\/api\/items/, '/items') },
      '/api/orders': { target: 'http://localhost:8083', rewrite: p => p.replace(/^\/api\/orders/, '/orders') },
      '/api/payments': { target: 'http://localhost:8084', rewrite: p => p.replace(/^\/api\/payments/, '/payments') },
    }
  }
})
