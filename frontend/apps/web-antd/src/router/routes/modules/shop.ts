import type { RouteRecordRaw } from 'vue-router';

const ALL = [
  'HQ_ADMIN',
  'HQ_OPS',
  'HQ_FIN',
  'FRANCHISEE_ADMIN',
  'STORE_OP',
];

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:shopping-cart',
      order: 5,
      title: '采购管理',
      authority: ALL,
    },
    name: 'Purchase',
    path: '/purchase',
    children: [
      {
        name: 'PurchaseHQ',
        path: 'hq',
        component: () => import('#/views/shop/Purchase.vue'),
        meta: { icon: 'lucide:building-2', title: '总部采购', authority: ALL },
      },
      {
        name: 'PurchaseSelf',
        path: 'self',
        component: () => import('#/views/shop/SelfPurchase.vue'),
        meta: { icon: 'lucide:truck', title: '自采', authority: ALL },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:store',
      order: 6,
      title: '门店管理',
      authority: ALL,
    },
    name: 'Shop',
    path: '/shop',
    children: [
      {
        name: 'ShopOrder',
        path: 'order',
        component: () => import('#/views/shop/Order.vue'),
        meta: { icon: 'lucide:receipt-text', title: '订单', authority: ALL },
      },
      {
        name: 'ShopStaff',
        path: 'staff',
        component: () => import('#/views/shop/Staff.vue'),
        meta: { icon: 'lucide:users-round', title: '人员管理', authority: ALL },
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
