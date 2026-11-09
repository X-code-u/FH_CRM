<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import { getStorePageApi, type StoreItem } from '#/api/org';
import {
  createPurchaseApi,
  getMaterialOptionsApi,
  type MaterialItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'ShopPurchase' });

const userStore = useUserStore();
const roles = computed<string[]>(() => userStore.userInfo?.roles ?? []);
const isStoreOperator = computed(() => roles.value.includes('STORE_OP'));
const isFranchiseeAdmin = computed(() => roles.value.includes('FRANCHISEE_ADMIN'));

const storeOptions = ref<StoreItem[]>([]);
const storeId = ref<number>();
const materials = ref<MaterialItem[]>([]);
const qtyMap = reactive<Record<number, number | null>>({});
const loading = ref(false);
const submitting = ref(false);

const columns = [
  { title: '编码', dataIndex: 'code', width: 90 },
  { title: '商品名称', dataIndex: 'name', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 120 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '可售库存', dataIndex: 'stockQty', width: 110, align: 'right' as const },
  { title: '采购数量', key: 'qty', width: 140 },
];

const selectedRows = computed(() =>
  materials.value
    .filter((m) => {
      const q = qtyMap[m.id];
      return q !== null && q !== undefined && Number(q) > 0;
    })
    .map((m) => ({ materialId: m.id, qty: Number(qtyMap[m.id]) })),
);

async function load() {
  loading.value = true;
  try {
    materials.value = await getMaterialOptionsApi();
    materials.value.forEach((m) => {
      if (qtyMap[m.id] === undefined) qtyMap[m.id] = null;
    });
    if (isFranchiseeAdmin.value) {
      const data = await getStorePageApi({ pageNum: 1, pageSize: 200 });
      storeOptions.value = data.records.filter((s) => s.isActive);
    }
    const ownStoreId = (userStore.userInfo as any)?.storeId;
    if (isStoreOperator.value && ownStoreId) {
      storeId.value = Number(ownStoreId);
    }
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (!storeId.value) {
    message.warning('请选择门店');
    return;
  }
  if (selectedRows.value.length === 0) {
    message.warning('请至少填写一个商品的采购数量');
    return;
  }
  submitting.value = true;
  try {
    const orderNo = await createPurchaseApi({
      storeId: storeId.value,
      items: selectedRows.value,
    });
    message.success(`已提交采购单 ${orderNo}，等待总部审批`);
    materials.value.forEach((m) => (qtyMap[m.id] = null));
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="采购" description="门店向总部下单，提交后由总部审批（审批时可调整数量）">
    <a-card :bordered="false">
      <a-alert
        type="info"
        show-icon
        class="mb-4"
        message="采购说明"
        description="下单后为待审批状态，总部可修改数量后通过或驳回；结算价按定价规则自动解析，无需手工填写。"
      />

      <a-form layout="inline" class="mb-4">
        <a-form-item label="采购门店">
          <a-select
            v-if="isFranchiseeAdmin"
            v-model:value="storeId"
            class="w-64"
            placeholder="选择名下门店"
            :options="storeOptions.map((s) => ({ value: s.id, label: `${s.name}（${s.storeCode}）` }))"
          />
          <a-input
            v-else
            :value="
              isStoreOperator
                ? (userStore.userInfo as any)?.storeId
                  ? '当前登录门店'
                  : '未绑定门店'
                : '仅门店账号可下单'
            "
            disabled
            class="w-48"
          />
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="materials"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 900 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'stockQty'">
            <span v-if="record.stockQty === null || record.stockQty === undefined" class="text-blue-500">无限</span>
            <span v-else>{{ formatAmount(record.stockQty) }}</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number
              v-model:value="qtyMap[record.id]"
              :min="0"
              :precision="2"
              class="w-full"
              :max="record.stockQty === null || record.stockQty === undefined ? undefined : Number(record.stockQty)"
              placeholder="0"
            />
          </template>
        </template>
      </a-table>

      <div class="mt-4 flex items-center justify-between">
        <span class="text-gray-500">
          已选 <b>{{ selectedRows.length }}</b> 个商品，实际结算金额以总部审批时的定价为准
        </span>
        <a-button type="primary" :loading="submitting" @click="submit">提交采购单</a-button>
      </div>
    </a-card>
  </Page>
</template>
