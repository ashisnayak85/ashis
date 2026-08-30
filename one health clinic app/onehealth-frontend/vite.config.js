import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    strictPort: true, // fail loudly instead of silently falling back to another
                       // port - the backend's CORS allow-list only permits 5175.
  },
})
