<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message, Modal } from 'ant-design-vue';

import {
  changeStoreStatusApi,
  getFranchiseePageApi,
  getStoreAccountApi,
  getStorePageApi,
  saveStoreAccountApi,
  saveStoreApi,
  type FranchiseeItem,
  type StoreItem,
} from '#/api/org';

defineOptions({ name: 'OrgStore' });

const userStore = useUserStore();
const canEdit = computed(() =>
  userStore.userInfo?.roles?.some((r: string) => ['HQ_ADMIN', 'HQ_OPS'].includes(r)),
);

const loading = ref(false);
const rows = ref<StoreItem[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, name: '', storeType: undefined as string | undefined });

const columns = [
  { title: '编码', dataIndex: 'storeCode', width: 100 },
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '类型', dataIndex: 'storeType', width: 90 },
  { title: '所属加盟商', dataIndex: 'franchiseeName', width: 160 },
  { title: '详细地址', dataIndex: 'address' },
  { title: '负责人', dataIndex: 'contactName', width: 100 },
  { title: '开业日期', dataIndex: 'openDate', width: 110 },
  { title: '状态', dataIndex: 'isActive', width: 90 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' as const },
];

async function load() {
  loading.value = true;
  try {
    const data = await getStorePageApi(query);
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
const form = reactive<Partial<StoreItem>>({});
const franchiseeOptions = ref<FranchiseeItem[]>([]);

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { name: '', storeType: 'JOIN', franchiseeId: undefined, address: '', contactName: '', contactPhone: '', openDate: undefined });
  modalOpen.value = true;
}

function openEdit(row: StoreItem) {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { ...row });
  modalOpen.value = true;
}

const rules = {
  name: [{ required: true, message: '请输入门店名称' }, { max: 64, message: '不超过 64 字' }],
  storeType: [{ required: true, message: '请选择门店类型' }],
  franchiseeId: [{ required: true, message: '加盟门店必须选择所属加盟商' }],
  address: [{ required: true, message: '请输入详细地址' }, { max: 128, message: '不超过 128 字' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }],
};

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = { ...form };
    if (payload.storeType === 'DIRECT') payload.franchiseeId = undefined;
    await saveStoreApi(payload);
    message.success('已保存');
    modalOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

function toggleStatus(row: StoreItem) {
  Modal.confirm({
    title: `确认${row.isActive ? '停用' : '启用'}「${row.name}」？`,
    content: row.isActive ? '停用后门店不可下单' : '启用后恢复正常业务',
    async onOk() {
      await changeStoreStatusApi(row.id, !row.isActive);
      message.success('状态已更新');
      await load();
    },
  });
}

// ---------- 门店登录账号 ----------
const accountOpen = ref(false);
const accountSaving = ref(false);
const accountLoading = ref(false);
const accountStoreName = ref('');
const accountRow = ref<StoreItem | null>(null);
const hasAccount = ref(false);
const accountFormRef = ref();
const accountForm = reactive({ username: '', password: '', realName: '' });

async function openAccount(row: StoreItem) {
  accountRow.value = row;
  accountStoreName.value = row.name;
  accountForm.username = '';
  accountForm.password = '';
  accountForm.realName = '';
  hasAccount.value = false;
  accountOpen.value = true;
  accountLoading.value = true;
  try {
    const data = await getStoreAccountApi(row.id);
    if (data) {
      hasAccount.value = true;
      accountForm.username = data.username;
      accountForm.realName = data.realName || '';
    }
  } finally {
    accountLoading.value = false;
  }
}

const accountRules = {
  username: [
    { required: true, message: '请输入登录账号' },
    { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '3-32 位字母、数字或下划线' },
  ],
  password: [
    {
      validator: (_rule: any, value: string) => {
        if (!hasAccount.value && !value) return Promise.reject(new Error('新建账号必须设置密码'));
        if (value && value.length < 6) return Promise.reject(new Error('密码不少于 6 位'));
        return Promise.resolve();
      },
    },
  ],
};

async function submitAccount() {
  await accountFormRef.value?.validate();
  if (!accountRow.value) return;
  accountSaving.value = true;
  try {
    await saveStoreAccountApi(accountRow.value.id, {
      username: accountForm.username,
      password: accountForm.password || undefined,
      realName: accountForm.realName || undefined,
    });
    message.success('账号已保存');
    accountOpen.value = false;
  } finally {
    accountSaving.value = false;
  }
}

onMounted(async () => {
  await load();
  if (canEdit.value) {
    const data = await getFranchiseePageApi({ pageNum: 1, pageSize: 500, status: 'NORMAL' });
    franchiseeOptions.value = data.records;
  }
});
</script>

<template>
  <Page title="门店管理" description="最小业务单元，编码系统自动生成，类型创建后不可修改">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="名称">
          <a-input v-model:value="query.name" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.storeType" allow-clear placeholder="全部" class="w-32">
            <a-select-option value="JOIN">加盟</a-select-option>
            <a-select-option value="DIRECT">直营</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
          <a-button v-if="canEdit" class="ml-2" @click="openCreate">新增门店</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1100 }"
        :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showTotal: (t: number) => `共 ${t} 条` }"
        @change="(p: any) => { query.pageNum = p.current; load(); }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'storeType'">
            <a-tag :color="record.storeType === 'DIRECT' ? 'blue' : 'purple'">
              {{ record.storeType === 'DIRECT' ? '直营' : '加盟' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'franchiseeName'">
            {{ record.franchiseeName || '—' }}
          </template>
          <template v-else-if="column.dataIndex === 'isActive'">
            <a-tag :color="record.isActive ? 'green' : 'default'">{{ record.isActive ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action' && canEdit">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="openAccount(record)">账号</a-button>
            <a-button type="link" size="small" :danger="record.isActive" @click="toggleStatus(record)">
              {{ record.isActive ? '停用' : '启用' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="form.id ? '编辑门店' : '新增门店'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }">
        <a-form-item label="门店类型" name="storeType">
          <a-radio-group v-model:value="form.storeType" :disabled="!!form.id">
            <a-radio value="JOIN">加盟</a-radio>
            <a-radio value="DIRECT">直营</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="form.storeType === 'JOIN'" label="所属加盟商" name="franchiseeId">
          <a-select
            v-model:value="form.franchiseeId"
            :disabled="!!form.id"
            show-search
            :filter-option="(input: string, o: any) => o.label.includes(input)"
            placeholder="仅可选择正常状态的加盟商"
            :options="franchiseeOptions.map((f) => ({ value: f.id, label: `${f.name}（${f.franchiseeCode}）` }))"
          />
        </a-form-item>
        <a-form-item label="门店名称" name="name"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="详细地址" name="address"><a-input v-model:value="form.address" :maxlength="128" /></a-form-item>
        <a-form-item label="负责人" name="contactName"><a-input v-model:value="form.contactName" /></a-form-item>
        <a-form-item label="联系电话" name="contactPhone"><a-input v-model:value="form.contactPhone" /></a-form-item>
        <a-form-item label="开业日期" name="openDate">
          <a-date-picker v-model:value="form.openDate" value-format="YYYY-MM-DD" class="w-full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="accountOpen"
      :title="`门店账号 · ${accountStoreName}`"
      :confirm-loading="accountSaving"
      @ok="submitAccount"
    >
      <a-spin :spinning="accountLoading">
        <a-form ref="accountFormRef" :model="accountForm" :rules="accountRules" :label-col="{ span: 5 }">
          <a-form-item label="登录账号" name="username">
            <a-input v-model:value="accountForm.username" :maxlength="32" placeholder="3-32 位字母、数字或下划线" />
          </a-form-item>
          <a-form-item label="姓名" name="realName">
            <a-input v-model:value="accountForm.realName" :maxlength="32" placeholder="选填" />
          </a-form-item>
          <a-form-item label="登录密码" name="password">
            <a-input-password
              v-model:value="accountForm.password"
              :placeholder="hasAccount ? '留空则不修改密码' : '不少于 6 位'"
            />
          </a-form-item>
          <div class="text-xs text-gray-500">
            账号固定为门店店长，登录后仅能操作本门店数据。
          </div>
        </a-form>
      </a-spin>
    </a-modal>
  </Page>
</template>
