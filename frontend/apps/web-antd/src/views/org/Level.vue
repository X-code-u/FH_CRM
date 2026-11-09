<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { getLevelListApi, saveLevelApi, type LevelItem } from '#/api/org';

defineOptions({ name: 'OrgLevel' });

const loading = ref(false);
const rows = ref<LevelItem[]>([]);

const columns = [
  { title: '等级编码', dataIndex: 'levelCode', width: 140 },
  { title: '等级名称', dataIndex: 'levelName', width: 140 },
  { title: '排序号', dataIndex: 'sortNo', width: 90 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark' },
  { title: '操作', key: 'action', width: 120 },
];

async function load() {
  loading.value = true;
  try {
    rows.value = await getLevelListApi();
  } finally {
    loading.value = false;
  }
}

// ---------- 新增 / 编辑弹窗 ----------
const modalOpen = ref(false);
const saving = ref(false);
const formRef = ref();
const form = reactive<Partial<LevelItem>>({});

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { levelCode: '', levelName: '', sortNo: 0, status: 'ENABLED', remark: '' });
  modalOpen.value = true;
}

function openEdit(row: LevelItem) {
  Object.keys(form).forEach((k) => delete (form as any)[k]);
  Object.assign(form, { ...row });
  modalOpen.value = true;
}

const rules = {
  levelCode: [
    { required: true, message: '请输入等级编码' },
    { pattern: /^[A-Z_]{1,16}$/, message: '仅允许大写字母与下划线，不超过 16 位' },
  ],
  levelName: [
    { required: true, message: '请输入等级名称' },
    { max: 32, message: '不超过 32 字' },
  ],
};

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await saveLevelApi(form);
    message.success('已保存');
    modalOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="加盟商等级" description="分级供货定价的等级维度，编码创建后不可修改">
    <a-card :bordered="false">
      <div class="mb-4">
        <a-button type="primary" @click="openCreate">新增等级</a-button>
      </div>
      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'ENABLED' ? 'green' : 'default'">
              {{ record.status === 'ENABLED' ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="form.id ? '编辑等级' : '新增等级'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }">
        <a-form-item label="等级编码" name="levelCode">
          <a-input v-model:value="form.levelCode" :disabled="!!form.id" placeholder="如 GOLD" />
        </a-form-item>
        <a-form-item label="等级名称" name="levelName">
          <a-input v-model:value="form.levelName" placeholder="如 黄金" />
        </a-form-item>
        <a-form-item label="排序号" name="sortNo">
          <a-input-number v-model:value="form.sortNo" :min="0" class="w-full" />
        </a-form-item>
        <a-form-item v-if="form.id" label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio value="ENABLED">启用</a-radio>
            <a-radio value="DISABLED">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
