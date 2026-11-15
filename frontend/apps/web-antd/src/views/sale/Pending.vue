<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import {
  getPurchaseDetailApi,
  getPurchasePageApi,
  receivePurchaseApi,
  type PurchaseOrder,
  type PurchaseOrderItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'StockPending' });

const loading = ref(false);
const rows = ref<PurchaseOrder[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '' });

const columns = [
  { title: '单号', dataIndex: 'orderNo', width: 170 },
  { title: '下单日期', dataIndex: 'orderDate', width: 110 },
  { title: '订单金额', dataIndex: 'totalAmount', width: 130, align: 'right' as const },
  { title: '发货时间', dataIndex: 'shippedAt', width: 160 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    // 总部已发货（待入库）与门店已收货的订单
    const data = await getPurchasePageApi({ ...query, statuses: 'SHIPPED,RECEIVED' });
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

const detailOpen = ref(false);
const detailLoading = ref(false);
const receiving = ref(false);
const currentOrder = ref<PurchaseOrder>();
const items = ref<PurchaseOrderItem[]>([]);

async function openDetail(row: PurchaseOrder) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    const data = await getPurchaseDetailApi(row.id);
    currentOrder.value = data.order;
    items.value = data.items;
  } finally {
    detailLoading.value = false;
  }
}

function receive() {
  const order = currentOrder.value;
  if (!order) return;
  Modal.confirm({
    title: '确认入库',
    content: `确认订单 ${order.orderNo} 已到货并办理入库？确认后门店库存将增加。`,
    okText: '确认入库',
    cancelText: '取消',
    async onOk() {
      receiving.value = true;
      try {
        // 提交前生成幂等号，防止重复入库
        const requestNo = `IN-${order.id}-${Date.now()}`;
        await receivePurchaseApi(order.id, requestNo);
        message.success('入库成功');
        detailOpen.value = false;
        await load();
      } finally {
        receiving.value = false;
      }
    },
  });
}

onMounted(load);
</script>

<template>
  <Page title="待入库" description="总部已发货的订单在此确认入库，确认后计入门店库存；已收货订单可查看明细">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="单号">
          <a-input v-model:value="query.orderNo" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 700 }"
        :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          total,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        @change="(p: any) => { query.pageNum = p.current; load(); }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'totalAmount'">
            {{ formatAmount(record.totalAmount) }}
          </template>
          <template v-else-if="column.dataIndex === 'shippedAt'">
            {{ formatTime(record.shippedAt) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'RECEIVED' ? 'green' : 'blue'">
              {{ record.status === 'RECEIVED' ? '已收货' : '已发货' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      :title="`入库单 ${currentOrder?.orderNo ?? ''}`"
      width="820px"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions :column="3" bordered size="small" class="mb-3">
          <a-descriptions-item label="单号" :span="2">{{ currentOrder?.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentOrder?.status === 'RECEIVED' ? 'green' : 'blue'">
              {{ currentOrder?.status === 'RECEIVED' ? '已收货' : '已发货' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="门店" :span="2">{{ currentOrder?.storeName || '—' }}</a-descriptions-item>
          <a-descriptions-item label="下单日期">{{ currentOrder?.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="发货时间">{{ formatTime(currentOrder?.shippedAt) }}</a-descriptions-item>
          <a-descriptions-item label="订单金额">
            <b>{{ formatAmount(currentOrder?.totalAmount) }}</b>
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ currentOrder?.remark || '—' }}</a-descriptions-item>
          <a-descriptions-item v-if="currentOrder?.status === 'RECEIVED'" label="收货时间">
            {{ formatTime(currentOrder?.receivedAt) }}
          </a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="items"
          row-key="id"
          size="small"
          :pagination="false"
          :columns="[
            { title: '商品', dataIndex: 'materialName' },
            { title: '规格', dataIndex: 'spec', width: 110 },
            { title: '单位', dataIndex: 'unit', width: 70 },
            { title: '单价', dataIndex: 'price', width: 100, align: 'right' },
            { title: '数量', dataIndex: 'qty', width: 90 },
            { title: '小计', dataIndex: 'amount', width: 110, align: 'right' },
          ]"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'price'">
              {{ formatAmount(record.price) }}
            </template>
            <template v-else-if="column.dataIndex === 'amount'">
              {{ formatAmount(record.amount) }}
            </template>
          </template>
        </a-table>
      </a-spin>
      <template #footer>
        <a-button @click="detailOpen = false">关闭</a-button>
        <a-button
          v-if="currentOrder?.status === 'SHIPPED'"
          type="primary"
          :loading="receiving"
          @click="receive"
        >
          确认入库
        </a-button>
      </template>
    </a-modal>
  </Page>
</template>
