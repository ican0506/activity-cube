import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

function apiTargetFromEnv(mode) {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  return new URL(apiBaseUrl).origin
}

export default defineConfig(({ mode }) => {
  const apiTarget = apiTargetFromEnv(mode)
  return {
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true
        },
        '/uploads': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    }
  }
})
