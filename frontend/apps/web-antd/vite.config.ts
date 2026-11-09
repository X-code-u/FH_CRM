import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api/v1': {
            changeOrigin: true,
            // 后端 context-path 即 /api/v1，无需 rewrite
            target: 'http://localhost:8080',
          },
        },
      },
    },
  };
});
