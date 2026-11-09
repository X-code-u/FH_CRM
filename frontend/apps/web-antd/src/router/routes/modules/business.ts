import type { RouteRecordRaw } from 'vue-router';

const HQ = ['HQ_ADMIN', 'HQ_OPS', 'HQ_FIN'];

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:file-text',
      order: 30,
      title: '供货管理',
      authority: HQ,
    },
    name: 'Sale',
    path: '/sale',
    children: [
      {
        name: 'SaleList',
        path: 'list',
        component: () => import('#/views/sale/List.vue'),
        meta: { icon: 'lucide:list', title: '销货单列表', authority: HQ },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:warehouse',
      order: 40,
      title: '库存管理',
    },
    name: 'Stock',
    path: '/stock',
    children: [
      {
        name: 'StockList',
        path: 'list',
        component: () => import('#/views/stock/List.vue'),
        meta: { icon: 'lucide:boxes', title: '门店库存' },
      },
      {
        name: 'StockFlow',
        path: 'flow',
        component: () => import('#/views/stock/Flow.vue'),
        meta: { icon: 'lucide:arrow-left-right', title: '出入库流水' },
      },
      {
        name: 'StockPending',
        path: 'pending',
        component: () => import('#/views/sale/Pending.vue'),
        meta: {
          icon: 'lucide:inbox',
          title: '待入库单据',
          authority: ['FRANCHISEE_ADMIN', 'STORE_OP'],
        },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:scale',
      order: 50,
      title: '对账报表',
    },
    name: 'Report',
    path: '/report',
    children: [
      {
        name: 'ReportSupply',
        path: 'supply',
        component: () => import('#/views/report/Supply.vue'),
        meta: { icon: 'lucide:calculator', title: '供货对账' },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:settings',
      order: 90,
      title: '系统管理',
      authority: HQ,
    },
    name: 'System',
    path: '/system',
    children: [
      {
        name: 'SystemUser',
        path: 'user',
        component: () => import('#/views/system/User.vue'),
        meta: { icon: 'lucide:user-cog', title: '账号管理', authority: HQ },
      },
      {
        name: 'SystemAudit',
        path: 'audit',
        component: () => import('#/views/system/Audit.vue'),
        meta: { icon: 'lucide:scroll-text', title: '审计日志', authority: ['HQ_ADMIN'] },
      },
    ],
  },
];

export default routes;
