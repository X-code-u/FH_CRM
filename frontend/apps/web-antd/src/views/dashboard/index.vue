<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { getWriteoffDashboardApi, type WriteoffDashboard } from '#/api/report';

defineOptions({ name: 'DashboardIndex' });

const loading = ref(false);
const days = ref(30);
const data = ref<null | WriteoffDashboard>(null);

const rangeOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
];

const maxPackageCount = computed(() =>
  Math.max(1, ...(data.value?.packages ?? []).map((p) => Number(p.writeoffCount))),
);
const maxProductQty = computed(() =>
  Math.max(1, ...(data.value?.products ?? []).map((p) => Number(p.qty))),
);
const maxBeauticianCount = computed(() =>
  Math.max(1, ...(data.value?.beauticians ?? []).map((b) => Number(b.writeoffCount))),
);

function money(value?: number | string) {
  return `¥${Number(value ?? 0).toFixed(2)}`;
}

function qty(value?: number | string) {
  return Number(value ?? 0);
}

async function load() {
  loading.value = true;
  try {
    data.value = await getWriteoffDashboardApi(days.value);
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page
    title="数据大盘"
    description="套餐核销、产品消耗与美容师业绩实时统计"
  >
    <template #extra>
      <a-radio-group v-model:value="days" button-style="solid" @change="load">
        <a-radio-button v-for="o in rangeOptions" :key="o.value" :value="o.value">
          {{ o.label }}
        </a-radio-button>
      </a-radio-group>
    </template>

    <a-spin :spinning="loading">
      <!-- 汇总指标 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :xs="24" :md="8">
          <a-card :bordered="false" class="kpi-card">
            <div class="kpi-label">套餐核销单数</div>
            <div class="kpi-value">{{ qty(data?.totalWriteoffs) }}</div>
            <div class="kpi-tip">统计区间内已完成核销的套餐次数</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="8">
          <a-card :bordered="false" class="kpi-card">
            <div class="kpi-label">核销业绩</div>
            <div class="kpi-value">{{ money(data?.totalRevenue) }}</div>
            <div class="kpi-tip">按核销时套餐价格累计</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="8">
          <a-card :bordered="false" class="kpi-card">
            <div class="kpi-label">产品扣减总量</div>
            <div class="kpi-value">{{ qty(data?.totalQty) }}</div>
            <div class="kpi-tip">核销自动扣减的入库商品数量合计</div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <!-- 套餐核销 -->
        <a-col :xs="24" :lg="12" class="mb-4">
          <a-card title="套餐核销排行" :bordered="false">
            <a-empty v-if="!data?.packages?.length" description="暂无核销数据" />
            <div v-for="(p, index) in data?.packages ?? []" :key="p.packageName" class="rank-row">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="rank-no" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
                  <span class="font-medium">{{ p.packageName }}</span>
                </div>
                <div class="text-right">
                  <span class="text-base font-semibold">{{ p.writeoffCount }}</span>
                  <span class="ml-1 text-xs text-gray-400">次</span>
                  <span class="ml-3 text-xs text-gray-500">{{ money(p.revenue) }}</span>
                </div>
              </div>
              <div class="bar">
                <div
                  class="bar-inner"
                  :style="{ width: `${(Number(p.writeoffCount) / maxPackageCount) * 100}%` }"
                ></div>
              </div>
            </div>
          </a-card>
        </a-col>

        <!-- 产品核销 -->
        <a-col :xs="24" :lg="12" class="mb-4">
          <a-card title="产品核销排行" :bordered="false">
            <a-empty v-if="!data?.products?.length" description="暂无核销数据" />
            <div
              v-for="(p, index) in data?.products ?? []"
              :key="p.materialId"
              class="product-row"
            >
              <span class="rank-no" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
              <img
                class="product-img"
                :src="p.imageUrl"
                :alt="p.materialName"
                loading="lazy"
              />
              <div class="min-w-0 flex-1">
                <div class="truncate font-medium">{{ p.materialName }}</div>
                <div class="bar mt-1">
                  <div
                    class="bar-inner warning"
                    :style="{ width: `${(Number(p.qty) / maxProductQty) * 100}%` }"
                  ></div>
                </div>
              </div>
              <div class="w-24 text-right">
                <span class="text-base font-semibold">{{ qty(p.qty) }}</span>
                <span class="ml-1 text-xs text-gray-400">{{ p.unit ?? '' }}</span>
              </div>
            </div>
          </a-card>
        </a-col>

        <!-- 美容师排行 -->
        <a-col :span="24">
          <a-card title="美容师排行" :bordered="false">
            <a-empty v-if="!data?.beauticians?.length" description="暂无核销数据" />
            <div
              v-for="(b, index) in data?.beauticians ?? []"
              :key="b.beautician"
              class="beautician-row"
            >
              <span class="rank-no" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
              <a-avatar :size="36" class="bg-emerald-500">
                {{ b.beautician.slice(0, 1) }}
              </a-avatar>
              <div class="min-w-0 flex-1">
                <div class="font-medium">{{ b.beautician }}</div>
                <div class="bar mt-1">
                  <div
                    class="bar-inner success"
                    :style="{ width: `${(Number(b.writeoffCount) / maxBeauticianCount) * 100}%` }"
                  ></div>
                </div>
              </div>
              <div class="ml-4 w-28 text-right">
                <span class="text-base font-semibold">{{ b.writeoffCount }}</span>
                <span class="ml-1 text-xs text-gray-400">单</span>
              </div>
              <div class="w-28 text-right text-sm text-gray-500">{{ money(b.revenue) }}</div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </Page>
</template>

<style scoped>
.kpi-card {
  border-radius: 12px;
  background: linear-gradient(135deg, #f7faf8 0%, #ffffff 100%);
}
.kpi-label {
  font-size: 13px;
  color: #6b7280;
}
.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}
.kpi-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}
.rank-row,
.product-row,
.beautician-row {
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
}
.rank-row:last-child,
.product-row:last-child,
.beautician-row:last-child {
  border-bottom: none;
}
.product-row,
.beautician-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.product-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
}
.rank-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 50%;
}
.rank-1 {
  color: #fff;
  background: #f59e0b;
}
.rank-2 {
  color: #fff;
  background: #94a3b8;
}
.rank-3 {
  color: #fff;
  background: #d97706;
}
.bar {
  height: 6px;
  margin-top: 6px;
  overflow: hidden;
  background: #f3f4f6;
  border-radius: 999px;
}
.bar-inner {
  height: 100%;
  background: #10b981;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.bar-inner.warning {
  background: #f59e0b;
}
.bar-inner.success {
  background: #14b8a6;
}
</style>
