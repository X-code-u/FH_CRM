<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import {
  changePackageStatusApi,
  createPackageApi,
  getPackageDetailApi,
  getPackageMaterialOptionsApi,
  getPackagePageApi,
  updatePackageApi,
  type PackageItemOption,
  type PackageRow,
} from '#/api/pkg';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'ShopPackage' });

const loading = ref(false);
const rows = ref<PackageRow[]>([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: undefined as string | undefined,
});

const columns = [
  { title: '套餐名称', dataIndex: 'packageName' },
  { title: '时长（分钟）', dataIndex: 'durationMinutes', width: 120 },
  { title: '价格（元）', dataIndex: 'price', width: 120, align: 'right' as const },
  { title: '绑定商品', dataIndex: 'itemCount', width: 100 },
  { title: '门店', dataIndex: 'storeName', width: 200 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 160 },
  { title: '操作', dataIndex: 'action', width: 140 },
];

async function load() {
  loading.value = true;
  try {
    const data = await getPackagePageApi({
      ...query,
      name: query.name || undefined,
      status: query.status || undefined,
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

// ---------- 新建 / 编辑 ----------
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<null | number>(null);
const materialOptions = ref<PackageItemOption[]>([]);
const form = reactive({
  packageName: '',
  durationMinutes: 90,
  price: 0,
  remark: '',
});
const items = ref<{ itemType: 'FIXED' | 'OPTIONAL'; materialId: number; qty: number }[]>([]);

const itemColumns = [
  { title: '消耗类型', dataIndex: 'itemType', width: 120 },
  { title: '商品', dataIndex: 'materialId' },
  { title: '规格', dataIndex: 'spec', width: 110 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '数量', dataIndex: 'qty', width: 120 },
  { title: '', dataIndex: 'action', width: 70 },
];

const availableOptions = computed(() =>
  materialOptions.value.filter(
    (o) => !items.value.some((i) => i.materialId === o.materialId),
  ),
);

function materialOf(id: number) {
  return materialOptions.value.find((o) => o.materialId === id);
}

function addItem() {
  const first = availableOptions.value[0];
  if (!first) {
    message.warning('没有更多可绑定的入库商品');
    return;
  }
  items.value.push({ itemType: 'FIXED', materialId: first.materialId, qty: 1 });
}

function removeItem(index: number) {
  items.value.splice(index, 1);
}

async function openCreate() {
  editingId.value = null;
  form.packageName = '';
  form.durationMinutes = 90;
  form.price = 0;
  form.remark = '';
  items.value = [];
  await loadMaterialOptions();
  modalOpen.value = true;
}

async function openEdit(row: PackageRow) {
  editingId.value = row.id;
  await loadMaterialOptions();
  const detail = await getPackageDetailApi(row.id);
  form.packageName = detail.packageInfo.packageName;
  form.durationMinutes = detail.packageInfo.durationMinutes;
  form.price = Number(detail.packageInfo.price);
  form.remark = detail.packageInfo.remark ?? '';
  items.value = detail.items.map((i) => ({
    itemType: i.itemType === 'OPTIONAL' ? 'OPTIONAL' : 'FIXED',
    materialId: i.materialId,
    qty: Number(i.qty),
  }));
  modalOpen.value = true;
}

async function loadMaterialOptions() {
  materialOptions.value = await getPackageMaterialOptionsApi();
}

async function submit() {
  if (!form.packageName.trim()) {
    message.warning('请填写套餐名称');
    return;
  }
  if (!form.durationMinutes || form.durationMinutes <= 0) {
    message.warning('请填写套餐时长');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      packageName: form.packageName.trim(),
      durationMinutes: form.durationMinutes,
      price: Number(form.price) || 0,
      remark: form.remark || undefined,
      items: items.value.map((i) => ({
        itemType: i.itemType,
        materialId: i.materialId,
        qty: Number(i.qty),
      })),
    };
    if (editingId.value) {
      await updatePackageApi(editingId.value, payload);
      message.success('套餐已更新');
    } else {
      await createPackageApi(payload);
      message.success('套餐已创建');
    }
    modalOpen.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

function toggleStatus(row: PackageRow) {
  const enabled = row.status !== 'ENABLED';
  changePackageStatusApi(row.id, enabled).then(() => {
    message.success(enabled ? '已启用' : '已停用');
    load();
  });
}

onMounted(load);
</script>

<template>
  <Page title="套餐管理" description="创建 90 分钟、45 分钟等套餐，并绑定本店已入库的商品">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="套餐名称">
          <a-input v-model:value="query.name" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            allow-clear
            placeholder="全部"
            style="width: 120px"
            :options="[
              { label: '启用', value: 'ENABLED' },
              { label: '停用', value: 'DISABLED' },
            ]"
            @change="search"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" ghost @click="openCreate">新建套餐</a-button>
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
          <template v-if="column.dataIndex === 'price'">
            {{ formatAmount(record.price) }}
          </template>
          <template v-else-if="column.dataIndex === 'itemCount'">
            {{ record.itemCount ?? 0 }} 种
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'ENABLED' ? 'green' : 'default'">
              {{ record.status === 'ENABLED' ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'updatedAt'">
            {{ formatTime(record.updatedAt) }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="openEdit(record)">编辑</a>
              <a-popconfirm
                :title="`确认${record.status === 'ENABLED' ? '停用' : '启用'}该套餐？`"
                @confirm="toggleStatus(record)"
              >
                <a>{{ record.status === 'ENABLED' ? '停用' : '启用' }}</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑套餐' : '新建套餐'"
      :confirm-loading="saving"
      width="720px"
      ok-text="保存"
      @ok="submit"
    >
      <a-form layout="vertical" class="mt-2">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="套餐名称" required>
              <a-input v-model:value="form.packageName" placeholder="如：90 分钟舒络套餐" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="时长（分钟）" required>
              <a-input-number v-model:value="form.durationMinutes" :min="1" :step="15" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="价格（元）">
              <a-input-number v-model:value="form.price" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-input v-model:value="form.remark" placeholder="选填" />
        </a-form-item>
      </a-form>

      <div class="mb-2 flex items-center justify-between">
        <span class="font-medium">绑定入库商品</span>
        <a-button size="small" type="primary" ghost @click="addItem">添加商品</a-button>
      </div>
      <a-table
        :columns="itemColumns"
        :data-source="items"
        row-key="materialId"
        size="small"
        :pagination="false"
        :locale="{ emptyText: '暂未绑定商品，可添加本店已入库的商品' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'itemType'">
            <a-select
              v-model:value="record.itemType"
              style="width: 100%"
              :options="[
                { label: '固定消耗', value: 'FIXED' },
                { label: '可选', value: 'OPTIONAL' },
              ]"
            />
          </template>
          <template v-else-if="column.dataIndex === 'materialId'">
            <a-select
              v-model:value="record.materialId"
              style="width: 100%"
              :options="
                materialOptions
                  .filter(
                    (o) =>
                      o.materialId === record.materialId ||
                      !items.some((i) => i.materialId === o.materialId),
                  )
                  .map((o) => ({
                    label: `${o.materialName}（结存 ${o.currentQty ?? 0}${o.unit ?? ''}）`,
                    value: o.materialId,
                  }))
              "
              show-search
              option-filter-prop="label"
            />
          </template>
          <template v-else-if="column.dataIndex === 'spec'">
            {{ materialOf(record.materialId)?.spec || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'unit'">
            {{ materialOf(record.materialId)?.unit || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'qty'">
            <a-input-number
              v-model:value="record.qty"
              :min="0.01"
              :max="Number(materialOf(record.materialId)?.currentQty ?? 999999)"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a @click="removeItem(index)">移除</a>
          </template>
        </template>
      </a-table>
    </a-modal>
  </Page>
</template>
