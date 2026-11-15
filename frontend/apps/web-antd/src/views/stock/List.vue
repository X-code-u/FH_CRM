<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { getStockPageApi, type StockItem } from '#/api/shop';

defineOptions({ name: 'StockList' });

const loading = ref(false);
const rows = ref<StockItem[]>([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialName: '',
  storeName: '',
});

const columns = [
  { title: '门店', dataIndex: 'storeName' },
  { title: '商品编码', dataIndex: 'materialCode', width: 110 },
  { title: '商品名称', dataIndex: 'materialName' },
  { title: '规格', dataIndex: 'spec', width: 110 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '库存数量', dataIndex: 'currentQty', width: 110, align: 'right' as const },
  { title: '预警状态', dataIndex: 'warn', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 160 },
];

async function load() {
  loading.value = true;
  try {
    const data = await getStockPageApi({
      ...query,
      storeName: query.storeName || undefined,
      materialName: query.materialName || undefined,
    });
    rows.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  load();
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—';
}

function isWarning(row: StockItem) {
  const warn = Number(row.warnMinQty ?? 0);
  return warn > 0 && Number(row.currentQty) <= warn;
}

onMounted(load);
</script>

<template>
  <Page title="门店库存" description="门店确认入库后的商品结存实时计入台账">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="门店">
          <a-input v-model:value="query.storeName" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="商品">
          <a-input v-model:value="query.materialName" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :row-key="(r: StockItem) => `${r.storeId}-${r.materialId}`"
        :scroll="{ x: 1100 }"
        :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          total,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        @change="(p: any) => { query.pageNum = p.current; load(); }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'currentQty'">
            {{ record.currentQty }}
          </template>
          <template v-else-if="column.dataIndex === 'warn'">
            <a-tag :color="isWarning(record) ? 'red' : 'green'">
              {{ isWarning(record) ? '低于预警' : '正常' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'updatedAt'">
            {{ formatTime(record.updatedAt) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </Page>
</template>
