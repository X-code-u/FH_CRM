import type { RouteRecordRaw } from 'vue-router';

const HQ = ['HQ_ADMIN', 'HQ_OPS', 'HQ_FIN'];

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:building-2',
      order: 10,
      title: '组织档案',
      authority: HQ,
    },
    name: 'Org',
    path: '/org',
    children: [
      {
        name: 'OrgLevel',
        path: 'level',
        component: () => import('#/views/org/Level.vue'),
        meta: { icon: 'lucide:layers', title: '加盟商等级', authority: ['HQ_ADMIN'] },
      },
      {
        name: 'OrgFranchisee',
        path: 'franchisee',
        component: () => import('#/views/org/Franchisee.vue'),
        meta: { icon: 'lucide:users', title: '加盟商管理', authority: HQ },
      },
      {
        name: 'OrgStore',
        path: 'store',
        component: () => import('#/views/org/Store.vue'),
        meta: { icon: 'lucide:store', title: '门店管理', authority: HQ },
      },
    ],
  },
  {
    meta: {
      icon: 'lucide:package',
      order: 20,
      title: '商品与定价',
      authority: HQ,
    },
    name: 'Material',
    path: '/material',
    children: [
      {
        name: 'MaterialList',
        path: 'list',
        component: () => import('#/views/material/List.vue'),
        meta: { icon: 'lucide:boxes', title: '商品管理', authority: HQ },
      },
      {
        name: 'MaterialPrice',
        path: 'price',
        component: () => import('#/views/material/Price.vue'),
        meta: { icon: 'lucide:tag', title: '分级供货定价', authority: HQ },
      },
    ],
  },
];

export default routes;
