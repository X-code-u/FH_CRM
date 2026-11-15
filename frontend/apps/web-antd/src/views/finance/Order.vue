<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import {
  getPurchaseDetailApi,
  getPurchasePageApi,
  payPurchaseApi,
  type PurchaseOrder,
  type PurchaseOrderItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'FinanceOrder' });

const loading = ref(false);
const rows = ref<PurchaseOrder[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '' });

const columns = [
  { title: '单号', dataIndex: 'orderNo', width: 170 },
  { title: '下单日期', dataIndex: 'orderDate', width: 120 },
  { title: '审核通过时间', dataIndex: 'confirmedAt', width: 170 },
  { title: '订单金额', dataIndex: 'totalAmount', width: 130, align: 'right' as const },
  { title: '付款状态', dataIndex: 'payStatus', width: 120 },
  { title: '付款时间', dataIndex: 'paidAt', width: 170 },
  { title: '备注', dataIndex: 'remark' },
  { title: '操作', key: 'action', width: 90, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    // 仅展示总部审核通过、进入付款环节的订单
    const data = await getPurchasePageApi({ ...query, status: 'CONFIRMED' });
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

const detailOpen = ref(false);
const detailLoading = ref(false);
const paying = ref(false);
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

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—';
}

function pay() {
  const order = currentOrder.value;
  if (!order) return;
  Modal.confirm({
    title: '确认付款',
    content: `确认支付订单 ${order.orderNo}，金额 ¥${formatAmount(order.totalAmount)}？确认后状态将变为「付款成功」。`,
    okText: '确认付款',
    cancelText: '取消',
    async onOk() {
      paying.value = true;
      try {
        await payPurchaseApi(order.id);
        message.success('付款成功');
        const data = await getPurchaseDetailApi(order.id);
        currentOrder.value = data.order;
        items.value = data.items;
        await load();
      } finally {
        paying.value = false;
      }
    },
  });
}

onMounted(load);
</script>

<template>
  <Page title="订单管理" description="总部审核通过的采购订单会推送至此，门店按订单金额一次性付款">
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
        :scroll="{ x: 1000 }"
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
          <template v-else-if="column.dataIndex === 'confirmedAt'">
            {{ formatTime(record.confirmedAt) }}
          </template>
          <template v-else-if="column.dataIndex === 'payStatus'">
            <a-tag :color="record.payStatus === 'PAID' ? 'green' : 'orange'">
              {{ record.payStatus === 'PAID' ? '付款成功' : '待付款' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'paidAt'">
            {{ formatTime(record.paidAt) }}
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            <span class="text-gray-500">{{ record.remark || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      :title="`订单 ${currentOrder?.orderNo ?? ''}`"
      width="820px"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions :column="3" bordered size="small" class="mb-3">
          <a-descriptions-item label="下单日期">{{ currentOrder?.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="审核通过时间">{{ formatTime(currentOrder?.confirmedAt) }}</a-descriptions-item>
          <a-descriptions-item label="付款时间">{{ formatTime(currentOrder?.paidAt) }}</a-descriptions-item>
          <a-descriptions-item label="付款状态">
            <a-tag :color="currentOrder?.payStatus === 'PAID' ? 'green' : 'orange'">
              {{ currentOrder?.payStatus === 'PAID' ? '付款成功' : '待付款' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentOrder?.remark || '—' }}</a-descriptions-item>
          <a-descriptions-item label="订单金额" :span="3">
            <b>{{ formatAmount(currentOrder?.totalAmount) }}</b>
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
          v-if="currentOrder && currentOrder.payStatus !== 'PAID'"
          type="primary"
          :loading="paying"
          @click="pay"
        >
          付款
        </a-button>
      </template>
    </a-modal>
  </Page>
</template>
