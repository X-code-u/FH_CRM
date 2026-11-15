import type { RouteRecordRaw } from 'vue-router';

const HQ = ['HQ_ADMIN', 'HQ_OPS', 'HQ_FIN'];
const STORE_SIDE = ['FRANCHISEE_ADMIN', 'STORE_OP'];
const ALL = [...HQ, ...STORE_SIDE];

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:layers',
      order: 5,
      title: '套餐管理',
      authority: STORE_SIDE,
    },
    name: 'Package',
    path: '/package',
    component: () => import('#/views/shop/Package.vue'),
  },
  {
    meta: {
      icon: 'lucide:clipboard-check',
      order: 6,
      title: '核销管理',
      authority: STORE_SIDE,
    },
    name: 'Writeoff',
    path: '/writeoff',
    component: () => import('#/views/shop/Writeoff.vue'),
  },
  {
    meta: {
      icon: 'lucide:shopping-cart',
      order: 10,
      title: '采购管理',
      authority: STORE_SIDE,
    },
    name: 'Purchase',
    path: '/purchase',
    children: [
      {
        name: 'PurchaseCreate',
        path: 'create',
        component: () => import('#/views/shop/Purchase.vue'),
        meta: { icon: 'lucide:file-plus', title: '采购', authority: STORE_SIDE },
      },
      {
        name: 'PurchaseOrders',
        path: 'orders',
        component: () => import('#/views/shop/Order.vue'),
        meta: {
          icon: 'lucide:receipt-text',
          title: '订单管理',
          authority: STORE_SIDE,
        },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:clipboard-check',
      order: 20,
      title: '审批中心',
      authority: HQ,
    },
    name: 'Approval',
    path: '/approval',
    component: () => import('#/views/shop/Order.vue'),
  },
  {
    meta: {
      icon: 'lucide:wallet',
      order: 30,
      title: '财务管理',
      authority: ALL,
    },
    name: 'Finance',
    path: '/finance',
    redirect: '/finance/overview',
    children: [
      {
        name: 'FinanceOverview',
        path: 'overview',
        component: () => import('#/views/finance/Index.vue'),
        meta: { icon: 'lucide:layout-dashboard', title: '财务概览', authority: ALL },
      },
      {
        name: 'FinanceOrders',
        path: 'orders',
        component: () => import('#/views/finance/Order.vue'),
        meta: {
          icon: 'lucide:receipt-text',
          title: '订单管理',
          authority: STORE_SIDE,
        },
      },
      {
        name: 'FinanceHqOrders',
        path: 'hq-orders',
        component: () => import('#/views/finance/HqOrder.vue'),
        meta: {
          icon: 'lucide:receipt-text',
          title: '订单管理',
          authority: ['HQ_ADMIN', 'HQ_FIN'],
        },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:graduation-cap',
      order: 60,
      title: '培训管理',
      authority: ALL,
    },
    name: 'Training',
    path: '/training',
    children: [
      {
        name: 'TrainingTechnique',
        path: 'technique',
        component: () => import('#/views/training/Technique.vue'),
        meta: { icon: 'lucide:hand', title: '手法培训', authority: ALL },
      },
      {
        name: 'TrainingReception',
        path: 'reception',
        component: () => import('#/views/training/Reception.vue'),
        meta: { icon: 'lucide:messages-square', title: '接待培训', authority: ALL },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:book-open',
      order: 70,
      title: '知识库',
      authority: ALL,
    },
    name: 'Knowledge',
    path: '/knowledge',
    children: [
      {
        name: 'KnowledgeTraining',
        path: 'training',
        component: () => import('#/views/knowledge/Training.vue'),
        meta: { icon: 'lucide:notebook-text', title: '培训知识', authority: ALL },
      },
    ],
  },
];

export default routes;
