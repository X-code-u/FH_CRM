<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message, Modal } from 'ant-design-vue';

import {
  changeMaterialStatusApi,
  getMaterialPageApi,
  saveMaterialApi,
  type MaterialItem,
} from '#/api/shop';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'MaterialList' });

const userStore = useUserStore();
const canEdit = computed(() =>
  userStore.userInfo?.roles?.some((r: string) => ['HQ_ADMIN', 'HQ_OPS'].includes(r)),
);

const loading = ref(false);
const rows = ref<MaterialItem[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, name: '', isActive: undefined as boolean | undefined });

const columns = [
  { title: '编码', dataIndex: 'code', width: 90 },
  { title: '商品名称', dataIndex: 'name', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 110 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '分类', dataIndex: 'category', width: 110 },
  { title: '基准售价', dataIndex: 'baseSalePrice', width: 110, align: 'right' as const },
  { title: '直营价', dataIndex: 'directPrice', width: 110, align: 'right' as const },
  { title: '总部库存', dataIndex: 'stockQty', width: 110, align: 'right' as const },
  { title: '状态', dataIndex: 'isActive', width: 80 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    const data = await getMaterialPageApi(query);
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

// ---------- 新增 / 编辑 ----------
const modalOpen = ref(false);
const saving = ref(false);
const formRef = ref();
const form = reactive<Partial<MaterialItem>>({});

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, {
    name: '',
    spec: '',
    unit: '',
    category: '',
    baseSalePrice: undefined,
    directPrice: 0,
    costPrice: 0,
    warnMinQty: 0,
    stockQty: null,
  });
  modalOpen.value = true;
}

function openEdit(row: MaterialItem) {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, {
    id: row.id,
    name: row.name,
    spec: row.spec,
    unit: row.unit,
    category: row.category,
    baseSalePrice: row.baseSalePrice,
    directPrice: row.directPrice,
    costPrice: row.costPrice,
    warnMinQty: row.warnMinQty,
    stockQty: row.stockQty ?? null,
  });
  modalOpen.value = true;
}

const rules = {
  name: [{ required: true, message: '请输入商品名称' }, { max: 64, message: '不超过 64 字' }],
  unit: [{ required: true, message: '请输入计量单位' }],
  baseSalePrice: [{ required: true, message: '请输入基准售价' }],
};

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await saveMaterialApi({
      ...form,
      // 清空输入框 = 无限库存
      stockQty: form.stockQty === undefined || form.stockQty === null ? null : form.stockQty,
    });
    message.success('已保存');
    modalOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

function toggleStatus(row: MaterialItem) {
  Modal.confirm({
    title: `确认${row.isActive ? '停用' : '启用'}「${row.name}」？`,
    content: row.isActive ? '停用后门店不可再采购该商品' : '启用后门店可正常采购',
    async onOk() {
      await changeMaterialStatusApi(row.id, !row.isActive);
      message.success('状态已更新');
      await load();
    },
  });
}

onMounted(load);
</script>

<template>
  <Page title="商品主数据" description="总部维护商品与可售库存，库存留空表示无限">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="商品名称">
          <a-input v-model:value="query.name" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.isActive" allow-clear placeholder="全部" class="w-32">
            <a-select-option :value="true">启用</a-select-option>
            <a-select-option :value="false">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
          <a-button v-if="canEdit" class="ml-2" @click="openCreate">新增商品</a-button>
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
          <template v-if="['baseSalePrice', 'directPrice'].includes(column.dataIndex)">
            {{ formatAmount(record[column.dataIndex]) }}
          </template>
          <template v-else-if="column.dataIndex === 'stockQty'">
            <span v-if="record.stockQty === null || record.stockQty === undefined" class="text-blue-500">无限</span>
            <span v-else>{{ formatAmount(record.stockQty) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'isActive'">
            <a-tag :color="record.isActive ? 'green' : 'default'">{{ record.isActive ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action' && canEdit">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" :danger="record.isActive" @click="toggleStatus(record)">
              {{ record.isActive ? '停用' : '启用' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="form.id ? '编辑商品' : '新增商品'"
      :confirm-loading="saving"
      width="620px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }">
        <a-form-item label="商品名称" name="name"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="规格" name="spec"><a-input v-model:value="form.spec" placeholder="如 30ml/瓶" /></a-form-item>
        <a-form-item label="计量单位" name="unit"><a-input v-model:value="form.unit" placeholder="瓶 / 盒 / 条" /></a-form-item>
        <a-form-item label="分类" name="category"><a-input v-model:value="form.category" /></a-form-item>
        <a-form-item label="基准售价" name="baseSalePrice">
          <a-input-number v-model:value="form.baseSalePrice" :min="0" :precision="2" class="w-full" />
        </a-form-item>
        <a-form-item label="直营价" name="directPrice">
          <a-input-number v-model:value="form.directPrice" :min="0" :precision="2" class="w-full" />
        </a-form-item>
        <a-form-item label="成本价" name="costPrice">
          <a-input-number v-model:value="form.costPrice" :min="0" :precision="2" class="w-full" />
        </a-form-item>
        <a-form-item label="总部库存" name="stockQty">
          <a-input-number
            v-model:value="form.stockQty"
            :min="0"
            :precision="2"
            class="w-full"
            placeholder="留空表示无限库存"
          />
          <div class="text-xs text-gray-400">留空 = 无限库存；填写数值后审批通过时自动扣减</div>
        </a-form-item>
        <a-form-item label="库存预警值" name="warnMinQty">
          <a-input-number v-model:value="form.warnMinQty" :min="0" :precision="2" class="w-full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
