import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      resolve: {
        // pnpm 的 peer 变体可能安装多份 vue-router，生产构建会导致 provide/inject 失效
        dedupe: ['pinia', 'vue', 'vue-router'],
      },
      server: {
        // 允许 Cloudflare 快速隧道域名访问（仅用于本地演示）
        allowedHosts: ['.trycloudflare.com'],
        proxy: {
          '/api/v1': {
            changeOrigin: true,
            // 后端 context-path 即 /api/v1，无需 rewrite
            target: 'http://localhost:8080',
          },
        },
      },
      preview: {
        allowedHosts: ['.trycloudflare.com'],
        proxy: {
          '/api/v1': {
            changeOrigin: true,
            target: 'http://localhost:8080',
          },
        },
      },
    },
  };
});
