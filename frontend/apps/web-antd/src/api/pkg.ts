import type { PageResult } from './shop';

import { requestClient } from '#/api/request';

// ---------- 门店套餐 ----------
export interface PackageRow {
  id: number;
  storeId: number;
  storeName?: string;
  packageName: string;
  durationMinutes: number;
  price: number | string;
  status: 'DISABLED' | 'ENABLED';
  remark?: string;
  itemCount?: number;
  updatedAt?: string;
}

export interface PackageItemOption {
  materialId: number;
  materialCode?: string;
  materialName?: string;
  spec?: string;
  unit?: string;
  qty?: number | string;
  /** FIXED 固定消耗 / OPTIONAL 可选 */
  itemType?: 'FIXED' | 'OPTIONAL';
  currentQty?: number | string;
}

export interface PackageDetail {
  packageInfo: {
    id: number;
    storeId: number;
    packageName: string;
    durationMinutes: number;
    price: number | string;
    status: string;
    remark?: string;
  };
  items: PackageItemOption[];
}

export interface PackagePayload {
  storeId?: number;
  packageName: string;
  durationMinutes: number;
  price: number;
  remark?: string;
  items: { itemType: 'FIXED' | 'OPTIONAL'; materialId: number; qty: number }[];
}

export function getPackagePageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<PackageRow>>('/package/page', { params });
}

export function getPackageDetailApi(id: number) {
  return requestClient.get<PackageDetail>(`/package/${id}`);
}

/** 可绑定商品：本店已入库且有结存的商品 */
export function getPackageMaterialOptionsApi() {
  return requestClient.get<PackageItemOption[]>('/package/material-options');
}

export function createPackageApi(data: PackagePayload) {
  return requestClient.post<number>('/package', data);
}

export function updatePackageApi(id: number, data: PackagePayload) {
  return requestClient.put(`/package/${id}`, data);
}

export function changePackageStatusApi(id: number, enabled: boolean) {
  return requestClient.put(`/package/${id}/status`, { enabled });
}

// ---------- 套餐核销 ----------
export interface WriteoffRow {
  id: number;
  writeoffNo: string;
  packageId: number;
  packageName: string;
  storeId: number;
  storeName?: string;
  beautician?: string;
  packagePrice?: number | string;
  remark?: string;
  itemCount?: number;
  totalQty?: number | string;
  createdAt?: string;
}

export function getWriteoffPageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<WriteoffRow>>('/writeoff/page', { params });
}

export function createWriteoffApi(data: {
  packageId: number;
  requestNo: string;
  beautician?: string;
  remark?: string;
  items: { materialId: number; qty: number }[];
}) {
  return requestClient.post<number>('/writeoff', data);
}
