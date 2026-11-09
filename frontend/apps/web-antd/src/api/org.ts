import { requestClient } from '#/api/request';

// ---------- 等级 ----------
export interface LevelItem {
  id: number;
  levelCode: string;
  levelName: string;
  sortNo: number;
  status: 'DISABLED' | 'ENABLED';
  remark?: string;
}

export function getLevelListApi() {
  return requestClient.get<LevelItem[]>('/franchise-level/list');
}

export function saveLevelApi(data: Partial<LevelItem>) {
  return requestClient.post('/franchise-level/save', data);
}

// ---------- 加盟商 ----------
export interface FranchiseeItem {
  id: number;
  franchiseeCode: string;
  name: string;
  contactName: string;
  contactPhone: string;
  region?: string;
  address?: string;
  levelCode: string;
  levelName?: string;
  levelStatus?: string;
  signDate: string;
  expireDate?: string;
  deposit?: number | string;
  status: 'FROZEN' | 'NORMAL' | 'TERMINATED';
  remark?: string;
}

export interface PageResult<T> {
  total: number;
  records: T[];
}

export function getFranchiseePageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<FranchiseeItem>>('/franchisee/page', { params });
}

export function saveFranchiseeApi(data: Partial<FranchiseeItem>) {
  return requestClient.post('/franchisee/save', data);
}

export function changeFranchiseeLevelApi(id: number, levelCode: string) {
  return requestClient.put(`/franchisee/${id}/level`, { levelCode });
}

export function changeFranchiseeStatusApi(id: number, status: string) {
  return requestClient.put(`/franchisee/${id}/status`, { status });
}

// ---------- 门店 ----------
export interface StoreItem {
  id: number;
  storeCode: string;
  name: string;
  storeType: 'DIRECT' | 'JOIN';
  franchiseeId?: number;
  franchiseeName?: string;
  address: string;
  contactName?: string;
  contactPhone?: string;
  openDate?: string;
  isActive: boolean;
}

export function getStorePageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<StoreItem>>('/store/page', { params });
}

export function saveStoreApi(data: Partial<StoreItem>) {
  return requestClient.post('/store/save', data);
}

export function changeStoreStatusApi(id: number, active: boolean) {
  return requestClient.put(`/store/${id}/status`, { active });
}
