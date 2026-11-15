<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import {
  createWriteoffApi,
  getPackageDetailApi,
  getPackagePageApi,
  getWriteoffPageApi,
  type PackageRow,
  type WriteoffRow,
} from '#/api/pkg';

defineOptions({ name: 'ShopWriteoff' });

interface WriteItem {
  itemType: 'FIXED' | 'OPTIONAL';
  materialId: number;
  materialName: string;
  packageQty: number;
  qty: number;
  selected: boolean;
  spec?: string;
  unit?: string;
}

// ---------- 核销操作 ----------
const packages = ref<PackageRow[]>([]);
const currentPackageId = ref<number>();
const beautician = ref('');
const remark = ref('');
const items = ref<WriteItem[]>([]);
const packageLoading = ref(false);
const submitting = ref(false);

const packageColumns = [
  { title: '消耗类型', dataIndex: 'itemType', width: 100 },
  { title: '商品', dataIndex: 'materialName' },
  { title: '规格', dataIndex: 'spec', width: 110 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '套餐数量', dataIndex: 'packageQty', width: 100 },
  { title: '是否选用', dataIndex: 'selected', width: 90 },
  { title: '本次核销数量', dataIndex: 'qty', width: 150 },
];

async function loadPackages() {
  const data = await getPackagePageApi({ pageNum: 1, pageSize: 100, status: 'ENABLED' });
  packages.value = data.records;
}

async function onPackageChange() {
  items.value = [];
  remark.value = '';
  beautician.value = '';
  if (!currentPackageId.value) return;
  packageLoading.value = true;
  try {
    const detail = await getPackageDetailApi(currentPackageId.value);
    items.value = detail.items.map((i) => {
      const fixed = i.itemType !== 'OPTIONAL';
      return {
        itemType: fixed ? 'FIXED' : 'OPTIONAL',
        materialId: i.materialId,
        materialName: i.materialName ?? `商品${i.materialId}`,
        spec: i.spec,
        unit: i.unit,
        packageQty: Number(i.qty ?? 0),
        qty: Number(i.qty ?? 0),
        selected: fixed,
      };
    });
  } finally {
    packageLoading.value = false;
  }
}

/** 实际扣减商品：固定消耗全带，可选按勾选及数量 */
function deductionItems() {
  return items.value
    .filter((i) => i.itemType === 'FIXED' || (i.selected && i.qty > 0))
    .map((i) => ({
      materialId: i.materialId,
      qty: i.itemType === 'FIXED' ? i.packageQty : Number(i.qty),
    }));
}

function confirmWriteoff() {
  if (!currentPackageId.value) {
    message.warning('请先选择套餐');
    return;
  }
  const list = deductionItems();
  const summary = items.value
    .filter((i) => i.itemType === 'FIXED' || (i.selected && i.qty > 0))
    .map(
      (i) =>
        `${i.materialName} × ${i.itemType === 'FIXED' ? i.packageQty : Number(i.qty)}${i.unit ?? ''}`,
    )
    .join('、');
  Modal.confirm({
    title: '确认核销',
    content: list.length
      ? `核销后将自动扣减库存：${summary}`
      : '该套餐未绑定商品，确认仅记录一次核销？',
    okText: '确认核销',
    cancelText: '取消',
    onOk: async () => {
      submitting.value = true;
      try {
        await createWriteoffApi({
          packageId: currentPackageId.value!,
          requestNo: `HX-${currentPackageId.value}-${Date.now()}`,
          beautician: beautician.value || undefined,
          remark: remark.value || undefined,
          items: list,
        });
        message.success('核销成功，库存已扣减');
        currentPackageId.value = undefined;
        items.value = [];
        beautician.value = '';
        remark.value = '';
        loadRecords();
      } finally {
        submitting.value = false;
      }
    },
  });
}

// ---------- 核销记录 ----------
const records = ref<WriteoffRow[]>([]);
const total = ref(0);
const recordLoading = ref(false);
const recordQuery = reactive({ pageNum: 1, pageSize: 10, packageName: '' });

const recordColumns = [
  { title: '核销单号', dataIndex: 'writeoffNo', width: 170 },
  { title: '套餐', dataIndex: 'packageName' },
  { title: '门店', dataIndex: 'storeName', width: 200 },
  { title: '美容师', dataIndex: 'beautician', width: 100 },
  { title: '商品种类', dataIndex: 'itemCount', width: 100 },
  { title: '扣减总量', dataIndex: 'totalQty', width: 100 },
  { title: '备注', dataIndex: 'remark' },
  { title: '核销时间', dataIndex: 'createdAt', width: 160 },
];

async function loadRecords() {
  recordLoading.value = true;
  try {
    const data = await getWriteoffPageApi({
      ...recordQuery,
      packageName: recordQuery.packageName || undefined,
    });
    records.value = data.records;
    total.value = data.total;
  } finally {
    recordLoading.value = false;
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—';
}

onMounted(async () => {
  await loadPackages();
  loadRecords();
});
</script>

<template>
  <Page title="核销管理" description="选择套餐核销，固定消耗商品自动扣减，可选商品可勾选并调整数量">
    <a-card title="套餐核销" :bordered="false" class="mb-4">
      <a-form layout="inline" class="mb-3">
        <a-form-item label="选择套餐" required>
          <a-select
            v-model:value="currentPackageId"
            show-search
            option-filter-prop="label"
            placeholder="请选择启用中的套餐"
            style="width: 280px"
            :options="
              packages.map((p) => ({
                label: `${p.packageName}（${p.durationMinutes}分钟 / ¥${Number(p.price).toFixed(2)}）`,
                value: p.id,
              }))
            "
            @change="onPackageChange"
          />
        </a-form-item>
        <a-form-item label="美容师">
          <a-input v-model:value="beautician" placeholder="填服务美容师姓名" style="width: 160px" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="remark" placeholder="选填，如客户姓名/单号" style="width: 200px" />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            :disabled="!currentPackageId"
            :loading="submitting"
            @click="confirmWriteoff"
          >
            确认核销
          </a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="packageColumns"
        :data-source="items"
        :loading="packageLoading"
        row-key="materialId"
        size="small"
        :pagination="false"
        :locale="{ emptyText: '请选择套餐，系统自动带出套餐商品' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'itemType'">
            <a-tag :color="record.itemType === 'FIXED' ? 'blue' : 'orange'">
              {{ record.itemType === 'FIXED' ? '固定消耗' : '可选' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'spec'">
            {{ record.spec || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'unit'">
            {{ record.unit || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'selected'">
            <a-checkbox
              v-model:checked="record.selected"
              :disabled="record.itemType === 'FIXED'"
            />
          </template>
          <template v-else-if="column.dataIndex === 'qty'">
            <a-input-number
              v-if="record.itemType === 'OPTIONAL'"
              v-model:value="record.qty"
              :min="0.01"
              :disabled="!record.selected"
              style="width: 100%"
            />
            <span v-else>{{ record.packageQty }}（固定）</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="核销记录" :bordered="false">
      <a-form layout="inline" class="mb-3">
        <a-form-item label="套餐名称">
          <a-input
            v-model:value="recordQuery.packageName"
            allow-clear
            placeholder="模糊搜索"
            @press-enter="() => { recordQuery.pageNum = 1; loadRecords(); }"
          />
        </a-form-item>
        <a-form-item>
          <a-button @click="() => { recordQuery.pageNum = 1; loadRecords(); }">查询</a-button>
        </a-form-item>
      </a-form>
      <a-table
        :columns="recordColumns"
        :data-source="records"
        :loading="recordLoading"
        row-key="id"
        size="middle"
        :pagination="{
          current: recordQuery.pageNum,
          pageSize: recordQuery.pageSize,
          total,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        @change="(p: any) => { recordQuery.pageNum = p.current; loadRecords(); }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'totalQty'">
            {{ Number(record.totalQty ?? 0) }}
          </template>
          <template v-else-if="column.dataIndex === 'beautician'">
            {{ record.beautician || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ record.remark || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </Page>
</template>
