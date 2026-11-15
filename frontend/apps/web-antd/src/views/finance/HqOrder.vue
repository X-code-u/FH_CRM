<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import {
  confirmReceiptApi,
  getPurchaseDetailApi,
  getPurchasePageApi,
  type PurchaseOrder,
  type PurchaseOrderItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'FinanceHqOrder' });

const loading = ref(false);
const rows = ref<PurchaseOrder[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '' });

const columns = [
  { title: '单号', dataIndex: 'orderNo', width: 170 },
  { title: '门店', dataIndex: 'storeName', width: 190 },
  { title: '下单日期', dataIndex: 'orderDate', width: 110 },
  { title: '审核通过时间', dataIndex: 'confirmedAt', width: 160 },
  { title: '订单金额', dataIndex: 'totalAmount', width: 130, align: 'right' as const },
  { title: '付款状态', dataIndex: 'payStatus', width: 110 },
  { title: '收款状态', dataIndex: 'receiptStatus', width: 110 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    // 审批中心通过的订单进入总部财务
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

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—';
}

function confirmReceipt(row: PurchaseOrder) {
  Modal.confirm({
    title: '确认收款',
    content: `确认已收到门店「${row.storeName}」订单 ${row.orderNo} 的款项 ¥${formatAmount(row.totalAmount)}？`,
    okText: '确认收款',
    cancelText: '取消',
    async onOk() {
      await confirmReceiptApi(row.id);
      message.success('已确认收款');
      await load();
    },
  });
}

const detailOpen = ref(false);
const detailLoading = ref(false);
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

onMounted(load);
</script>

<template>
  <Page title="订单管理" description="审批中心通过的订单同步至此，门店付款后可确认收款，确认后进入发货环节">
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
        :scroll="{ x: 1200 }"
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
              {{ record.payStatus === 'PAID' ? '已付款' : '待付款' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'receiptStatus'">
            <a-tag :color="record.receiptStatus === 'CONFIRMED' ? 'green' : 'default'">
              {{ record.receiptStatus === 'CONFIRMED' ? '已收款' : '待收款' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
            <a-button
              v-if="record.payStatus === 'PAID' && record.receiptStatus !== 'CONFIRMED'"
              type="link"
              size="small"
              @click="confirmReceipt(record)"
            >
              确认收款
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      :title="`订单 ${currentOrder?.orderNo ?? ''}`"
      width="820px"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions :column="3" bordered size="small" class="mb-3">
          <a-descriptions-item label="门店">{{ currentOrder?.storeName || '—' }}</a-descriptions-item>
          <a-descriptions-item label="下单日期">{{ currentOrder?.orderDate || '—' }}</a-descriptions-item>
          <a-descriptions-item label="审核通过时间">{{ formatTime(currentOrder?.confirmedAt) }}</a-descriptions-item>
          <a-descriptions-item label="付款状态">
            <a-tag :color="currentOrder?.payStatus === 'PAID' ? 'green' : 'orange'">
              {{ currentOrder?.payStatus === 'PAID' ? '已付款' : '待付款' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="付款时间">{{ formatTime(currentOrder?.paidAt) }}</a-descriptions-item>
          <a-descriptions-item label="订单金额">
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
    </a-modal>
  </Page>
</template>
