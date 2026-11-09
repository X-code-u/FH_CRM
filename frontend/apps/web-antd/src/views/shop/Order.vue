<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import {
  approvePurchaseApi,
  getPurchaseDetailApi,
  getPurchasePageApi,
  rejectPurchaseApi,
  type PurchaseOrder,
  type PurchaseOrderItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'ShopOrder' });

const userStore = useUserStore();
const canApprove = computed(() =>
  userStore.userInfo?.roles?.some((r: string) => ['HQ_ADMIN', 'HQ_OPS'].includes(r)),
);

const STATUS_TEXT: Record<string, string> = {
  PENDING: '待审批',
  CONFIRMED: '已通过',
  CANCELLED: '已驳回',
};
const STATUS_COLOR: Record<string, string> = {
  PENDING: 'orange',
  CONFIRMED: 'green',
  CANCELLED: 'red',
};

const loading = ref(false);
const rows = ref<PurchaseOrder[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, status: undefined as string | undefined, orderNo: '' });

const columns = [
  { title: '单号', dataIndex: 'orderNo', width: 160 },
  { title: '门店', dataIndex: 'storeName', width: 170 },
  { title: '类型', dataIndex: 'storeType', width: 80 },
  { title: '下单日期', dataIndex: 'orderDate', width: 110 },
  { title: '金额', dataIndex: 'totalAmount', width: 120, align: 'right' as const },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '备注 / 驳回原因', dataIndex: 'remark' },
  { title: '操作', key: 'action', width: 110, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    const data = await getPurchasePageApi(query);
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

// ---------- 详情 / 审批 ----------
const detailOpen = ref(false);
const detailLoading = ref(false);
const approving = ref(false);
const currentOrder = ref<PurchaseOrder>();
const items = ref<PurchaseOrderItem[]>([]);
const editQty = reactive<Record<number, number>>({});

const editedTotal = computed(() =>
  items.value.reduce((sum, i) => sum + Number(editQty[i.id] ?? 0) * Number(i.price), 0),
);

async function openDetail(row: PurchaseOrder) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    const data = await getPurchaseDetailApi(row.id);
    currentOrder.value = data.order;
    items.value = data.items;
    data.items.forEach((i) => (editQty[i.id] = Number(i.qty)));
  } finally {
    detailLoading.value = false;
  }
}

async function approve() {
  if (!currentOrder.value) return;
  const payload = items.value.map((i) => ({ itemId: i.id, qty: Number(editQty[i.id]) }));
  if (payload.some((p) => !p.qty || p.qty <= 0)) {
    message.warning('审批数量需大于 0');
    return;
  }
  approving.value = true;
  try {
    await approvePurchaseApi(currentOrder.value.id, payload);
    message.success('已审批通过');
    detailOpen.value = false;
    await load();
  } finally {
    approving.value = false;
  }
}

// ---------- 驳回 ----------
const rejectOpen = ref(false);
const rejectReason = ref('');
const rejecting = ref(false);

function openReject() {
  rejectReason.value = '';
  rejectOpen.value = true;
}

async function submitReject() {
  if (!currentOrder.value) return;
  if (!rejectReason.value.trim()) {
    message.warning('请填写驳回原因');
    return;
  }
  rejecting.value = true;
  try {
    await rejectPurchaseApi(currentOrder.value.id, rejectReason.value.trim());
    message.success('已驳回');
    rejectOpen.value = false;
    detailOpen.value = false;
    await load();
  } finally {
    rejecting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="采购订单" description="门店提交的采购单，总部在此审批（通过时可调整数量）">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="单号">
          <a-input v-model:value="query.orderNo" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="全部" class="w-32">
            <a-select-option value="PENDING">待审批</a-select-option>
            <a-select-option value="CONFIRMED">已通过</a-select-option>
            <a-select-option value="CANCELLED">已驳回</a-select-option>
          </a-select>
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
          <template v-if="column.dataIndex === 'storeType'">
            <a-tag :color="record.storeType === 'DIRECT' ? 'blue' : 'purple'">
              {{ record.storeType === 'DIRECT' ? '直营' : '加盟' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'totalAmount'">
            {{ formatAmount(record.totalAmount) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="STATUS_COLOR[record.status]">{{ STATUS_TEXT[record.status] }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            <span v-if="record.status === 'CANCELLED'" class="text-red-500">{{ record.cancelReason }}</span>
            <span v-else class="text-gray-500">{{ record.remark || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">
              {{ canApprove && record.status === 'PENDING' ? '审批' : '查看' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      :title="`采购单 ${currentOrder?.orderNo ?? ''}`"
      width="880px"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions :column="3" bordered size="small" class="mb-3">
          <a-descriptions-item label="门店">{{ currentOrder?.storeName }}</a-descriptions-item>
          <a-descriptions-item label="下单日期">{{ currentOrder?.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag v-if="currentOrder" :color="STATUS_COLOR[currentOrder.status]">
              {{ STATUS_TEXT[currentOrder.status] }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentOrder?.remark || '—' }}</a-descriptions-item>
          <a-descriptions-item label="金额">{{ formatAmount(currentOrder?.totalAmount) }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="currentOrder?.status === 'CANCELLED'"
          type="error"
          class="mb-3"
          :message="`驳回原因：${currentOrder.cancelReason}`"
        />

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
            { title: '数量', key: 'qty', width: 130 },
            { title: '小计', dataIndex: 'amount', width: 110, align: 'right' },
          ]"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'price'">
              {{ formatAmount(record.price) }}
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number
                v-if="canApprove && currentOrder?.status === 'PENDING'"
                v-model:value="editQty[record.id]"
                :min="0.01"
                :precision="2"
                size="small"
                class="w-full"
              />
              <span v-else>{{ record.qty }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'amount'">
              {{
                formatAmount(
                  (canApprove && currentOrder?.status === 'PENDING'
                    ? Number(editQty[record.id] ?? 0) * Number(record.price)
                    : record.amount),
                )
              }}
            </template>
          </template>
        </a-table>

        <div class="mt-3 flex items-center justify-between">
          <span class="text-gray-600">
            合计金额：<b class="text-base">{{ formatAmount(editedTotal) }}</b>
            <span v-if="canApprove && currentOrder?.status === 'PENDING'" class="ml-2 text-xs text-gray-400">
              （调整数量后金额实时重算，单价沿用下单快照）
            </span>
          </span>
          <span v-if="canApprove && currentOrder?.status === 'PENDING'">
            <a-button danger :loading="rejecting" @click="openReject">驳回</a-button>
            <a-button type="primary" class="ml-2" :loading="approving" @click="approve">审批通过</a-button>
          </span>
        </div>
      </a-spin>
    </a-modal>

    <a-modal v-model:open="rejectOpen" title="驳回采购单" :confirm-loading="rejecting" @ok="submitReject">
      <a-form :label-col="{ span: 5 }">
        <a-form-item label="驳回原因" required>
          <a-textarea v-model:value="rejectReason" :rows="3" :maxlength="255" placeholder="请说明驳回原因，门店可见" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
