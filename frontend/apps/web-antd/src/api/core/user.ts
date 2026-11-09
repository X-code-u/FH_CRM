import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

interface CurrentAccount {
  userId: number;
  username: string;
  roleCode: string;
  dataScope: 'ALL' | 'FRANCHISEE' | 'STORE';
  franchiseeId?: number;
  storeId?: number;
}

/**
 * 获取用户信息：后端 /auth/current 映射为 vben UserInfo
 */
export async function getUserInfoApi(): Promise<UserInfo> {
  const acc = await requestClient.get<CurrentAccount>('/auth/current');
  return {
    userId: String(acc.userId),
    username: acc.username,
    realName: acc.username,
    roles: [acc.roleCode],
    avatar: '',
    desc: acc.dataScope,
    homePath: '/dashboard',
    token: '',
    // 保留数据范围与归属，供页面逻辑使用
    dataScope: acc.dataScope,
    franchiseeId: acc.franchiseeId,
    storeId: acc.storeId,
  } as UserInfo;
}
