import type { RouteRecordRaw } from 'vue-router';

const HQ = ['HQ_ADMIN', 'HQ_OPS', 'HQ_FIN'];

const routes: RouteRecordRaw[] = [
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
        meta: {
          icon: 'lucide:boxes',
          title: '门店库存',
          authority: ['HQ_ADMIN', 'HQ_OPS', 'HQ_FIN', 'STORE_OP'],
        },
      },
      {
        name: 'StockFlow',
        path: 'flow',
        component: () => import('#/views/stock/Flow.vue'),
        meta: { icon: 'lucide:arrow-left-right', title: '出入库流水' },
      },
      {
        name: 'StockShip',
        path: 'ship',
        component: () => import('#/views/stock/Ship.vue'),
        meta: {
          icon: 'lucide:truck',
          title: '发货',
          authority: ['HQ_ADMIN', 'HQ_OPS'],
        },
      },
      {
        name: 'StockPending',
        path: 'pending',
        component: () => import('#/views/sale/Pending.vue'),
        meta: {
          icon: 'lucide:inbox',
          title: '待入库',
          authority: ['FRANCHISEE_ADMIN', 'STORE_OP'],
        },
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
