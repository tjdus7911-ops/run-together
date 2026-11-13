import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// base './' : 어느 경로에 올려도(GitHub Pages 하위 경로, 정적 호스팅) 그대로 열리게
export default defineConfig({
  base: './',
  plugins: [react()],
  build: { target: 'es2020' },
})
