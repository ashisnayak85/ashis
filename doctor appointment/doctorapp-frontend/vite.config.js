import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: true, // fail loudly instead of silently falling back to another
                       // port (e.g. 5173, already used by your other project) -
                       // the backend's CORS allow-list only permits 5174.
  },
})
