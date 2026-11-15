<script lang="ts" setup>
import { computed } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences, usePreferences } from '@vben/preferences';

const appName = computed(() => preferences.app.name);
const logo = '/logo.png';
const logoDark = '/logo.png';
const { isDark } = usePreferences();
const sloganImage = computed(() =>
  isDark.value ? '/slogan-dark.png' : '/slogan-light.png',
);
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :logo="logo"
    :logo-dark="logoDark"
    :page-description="''"
    :page-title="''"
    :slogan-image="sloganImage"
  >
    <!-- 自定义工具栏 -->
    <!-- <template #toolbar></template> -->
  </AuthPageLayout>
</template>

<style>
.animate-float {
  object-fit: contain;
}
</style>
