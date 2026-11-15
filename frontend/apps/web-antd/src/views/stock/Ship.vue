<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import { getPurchasePageApi, shipPurchaseApi, type PurchaseOrder } from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'StockShip' });

const loading = ref(false);
const rows = ref<PurchaseOrder[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '' });

const columns = [
  { title: '单号', dataIndex: 'orderNo', width: 170 },
  { title: '门店', dataIndex: 'storeName', width: 190 },
  { title: '下单日期', dataIndex: 'orderDate', width: 110 },
  { title: '订单金额', dataIndex: 'totalAmount', width: 130, align: 'right' as const },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '收款确认时间', dataIndex: 'receiptAt', width: 160 },
  { title: '发货时间', dataIndex: 'shippedAt', width: 160 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    // 审批通过且财务已确认收款的订单：待发货（CONFIRMED）+ 已发货（SHIPPED）
    const data = await getPurchasePageApi({
      ...query,
      statuses: 'CONFIRMED,SHIPPED',
      payStatus: 'PAID',
      receiptStatus: 'CONFIRMED',
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

function ship(row: PurchaseOrder) {
  Modal.confirm({
    title: '确认发货',
    content: `确认对订单 ${row.orderNo}（门店：${row.storeName}）执行发货？发货后门店可在「待入库」确认入库。`,
    okText: '确认发货',
    cancelText: '取消',
    async onOk() {
      await shipPurchaseApi(row.id);
      message.success('已发货');
      await load();
    },
  });
}

onMounted(load);
</script>

<template>
  <Page title="发货" description="审批通过且财务已确认收款的订单在此显示，确认发货后门店端进入待入库">
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
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'SHIPPED' ? 'blue' : 'orange'">
              {{ record.status === 'SHIPPED' ? '已发货' : '待发货' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'receiptAt'">
            {{ formatTime(record.receiptAt) }}
          </template>
          <template v-else-if="column.dataIndex === 'shippedAt'">
            {{ formatTime(record.shippedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.status === 'CONFIRMED'"
              type="link"
              size="small"
              @click="ship(record)"
            >
              发货
            </a-button>
            <span v-else>—</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </Page>
</template>
