<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message, Modal } from 'ant-design-vue';

import {
  changeFranchiseeLevelApi,
  changeFranchiseeStatusApi,
  getFranchiseePageApi,
  getLevelListApi,
  saveFranchiseeApi,
  type FranchiseeItem,
  type LevelItem,
} from '#/api/org';
import { formatAmount } from '#/utils/format';

defineOptions({ name: 'OrgFranchisee' });

const userStore = useUserStore();
const isHqAdmin = computed(() => userStore.userInfo?.roles?.includes('HQ_ADMIN'));
const canEdit = computed(() =>
  userStore.userInfo?.roles?.some((r: string) => ['HQ_ADMIN', 'HQ_OPS'].includes(r)),
);

const loading = ref(false);
const rows = ref<FranchiseeItem[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, name: '', status: undefined as string | undefined });

const columns = [
  { title: '编码', dataIndex: 'franchiseeCode', width: 100 },
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '负责人', dataIndex: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', width: 130 },
  { title: '地区', dataIndex: 'region', width: 140 },
  { title: '合作等级', dataIndex: 'levelName', width: 120 },
  { title: '签约日期', dataIndex: 'signDate', width: 110 },
  { title: '保证金', dataIndex: 'deposit', width: 110, align: 'right' as const },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
];

const STATUS_TEXT: Record<string, string> = { NORMAL: '正常', FROZEN: '冻结', TERMINATED: '终止' };
const STATUS_COLOR: Record<string, string> = { NORMAL: 'green', FROZEN: 'orange', TERMINATED: 'default' };

async function load() {
  loading.value = true;
  try {
    const data = await getFranchiseePageApi(query);
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
const form = reactive<Partial<FranchiseeItem>>({});
const levelOptions = ref<LevelItem[]>([]);

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { name: '', contactName: '', contactPhone: '', region: '', address: '', levelCode: undefined, signDate: undefined, expireDate: undefined, deposit: 0, remark: '' });
  modalOpen.value = true;
}

function openEdit(row: FranchiseeItem) {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { ...row });
  modalOpen.value = true;
}

const rules = {
  name: [
    { required: true, message: '请输入加盟商名称' },
    { min: 2, max: 64, message: '长度 2 至 64 字' },
  ],
  contactName: [{ required: true, message: '请输入负责人' }],
  contactPhone: [
    { required: true, message: '请输入联系电话' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
  ],
  levelCode: [{ required: true, message: '请选择合作等级' }],
  signDate: [{ required: true, message: '请选择签约日期' }],
};

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await saveFranchiseeApi(form);
    message.success('已保存');
    modalOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

// ---------- 等级变更（仅超管） ----------
const levelModalOpen = ref(false);
const levelSaving = ref(false);
const levelTarget = ref<FranchiseeItem>();
const newLevelCode = ref<string>();

function openLevelChange(row: FranchiseeItem) {
  levelTarget.value = row;
  newLevelCode.value = row.levelCode;
  levelModalOpen.value = true;
}

async function submitLevel() {
  if (!levelTarget.value || !newLevelCode.value) return;
  levelSaving.value = true;
  try {
    await changeFranchiseeLevelApi(levelTarget.value.id, newLevelCode.value);
    message.success('等级已变更');
    levelModalOpen.value = false;
    await load();
  } finally {
    levelSaving.value = false;
  }
}

// ---------- 状态流转 ----------
function changeStatus(row: FranchiseeItem, target: string) {
  const tips: Record<string, string> = {
    FROZEN: '冻结后其名下门店不可新建销货单、不可确认入库',
    TERMINATED: '终止不可逆，名下门店将自动停用',
    NORMAL: '恢复正常经营状态',
  };
  Modal.confirm({
    title: `确认将「${row.name}」转为${STATUS_TEXT[target]}？`,
    content: tips[target],
    async onOk() {
      await changeFranchiseeStatusApi(row.id, target);
      message.success('状态已更新');
      await load();
    },
  });
}

onMounted(async () => {
  await load();
  if (canEdit.value) {
    levelOptions.value = await getLevelListApi();
  }
});
</script>

<template>
  <Page title="加盟商管理" description="签约主体档案，编码系统自动生成">
    <a-card :bordered="false">
      <a-form layout="inline" class="mb-4">
        <a-form-item label="名称">
          <a-input v-model:value="query.name" allow-clear placeholder="模糊搜索" @press-enter="search" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="全部" class="w-32">
            <a-select-option value="NORMAL">正常</a-select-option>
            <a-select-option value="FROZEN">冻结</a-select-option>
            <a-select-option value="TERMINATED">终止</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查询</a-button>
          <a-button v-if="canEdit" class="ml-2" @click="openCreate">新增加盟商</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1200 }"
        :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showTotal: (t: number) => `共 ${t} 条` }"
        @change="(p: any) => { query.pageNum = p.current; load(); }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'levelName'">
            {{ record.levelName || record.levelCode }}
            <a-tag v-if="record.levelStatus === 'DISABLED'" color="red" class="ml-1">等级已停用</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'deposit'">
            {{ formatAmount(record.deposit) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="STATUS_COLOR[record.status]">{{ STATUS_TEXT[record.status] }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <template v-if="canEdit">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-button v-if="isHqAdmin" type="link" size="small" @click="openLevelChange(record)">变更等级</a-button>
              <a-button v-if="record.status === 'NORMAL'" type="link" size="small" @click="changeStatus(record, 'FROZEN')">冻结</a-button>
              <a-button v-if="record.status === 'NORMAL'" type="link" size="small" danger @click="changeStatus(record, 'TERMINATED')">终止</a-button>
              <a-button v-if="record.status === 'FROZEN'" type="link" size="small" @click="changeStatus(record, 'NORMAL')">恢复</a-button>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="form.id ? '编辑加盟商' : '新增加盟商'" :confirm-loading="saving" width="640px" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }">
        <a-form-item label="名称" name="name"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="负责人" name="contactName"><a-input v-model:value="form.contactName" /></a-form-item>
        <a-form-item label="联系电话" name="contactPhone"><a-input v-model:value="form.contactPhone" /></a-form-item>
        <a-form-item label="所在地区" name="region"><a-input v-model:value="form.region" placeholder="省市，如 浙江省杭州市" /></a-form-item>
        <a-form-item label="详细地址" name="address"><a-input v-model:value="form.address" :maxlength="128" /></a-form-item>
        <a-form-item v-if="!form.id" label="合作等级" name="levelCode">
          <a-select v-model:value="form.levelCode" placeholder="选择启用状态的等级">
            <a-select-option v-for="l in levelOptions.filter((x) => x.status === 'ENABLED')" :key="l.levelCode" :value="l.levelCode">
              {{ l.levelName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="签约日期" name="signDate">
          <a-date-picker v-model:value="form.signDate" value-format="YYYY-MM-DD" class="w-full" />
        </a-form-item>
        <a-form-item label="到期日期" name="expireDate">
          <a-date-picker v-model:value="form.expireDate" value-format="YYYY-MM-DD" class="w-full" />
        </a-form-item>
        <a-form-item label="保证金" name="deposit">
          <a-input-number v-model:value="form.deposit" :min="0" :precision="2" class="w-full" />
        </a-form-item>
        <a-form-item label="备注" name="remark"><a-textarea v-model:value="form.remark" :maxlength="255" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="levelModalOpen" title="变更合作等级" :confirm-loading="levelSaving" @ok="submitLevel">
      <p class="mb-3 text-gray-500">变更仅影响后续新建单据的定价解析，历史单据不回溯。变更将写入审计日志。</p>
      <a-form :label-col="{ span: 5 }">
        <a-form-item label="当前等级">
          <span>{{ levelTarget?.levelName }}（{{ levelTarget?.levelCode }}）</span>
        </a-form-item>
        <a-form-item label="新等级" required>
          <a-select v-model:value="newLevelCode">
            <a-select-option v-for="l in levelOptions.filter((x) => x.status === 'ENABLED')" :key="l.levelCode" :value="l.levelCode">
              {{ l.levelName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
