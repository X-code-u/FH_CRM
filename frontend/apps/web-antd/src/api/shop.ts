import { requestClient } from '#/api/request';

export interface PageResult<T> {
  total: number;
  records: T[];
}

// ---------- 商品 ----------
export interface MaterialItem {
  id: number;
  code: string;
  name: string;
  spec?: string;
  unit: string;
  category?: string;
  baseSalePrice: number | string;
  directPrice: number | string;
  costPrice?: number | string;
  warnMinQty?: number | string;
  /** null / undefined 表示无限库存 */
  stockQty?: number | string | null;
  isActive: boolean;
}

export function getMaterialPageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<MaterialItem>>('/material/page', { params });
}

export function getMaterialOptionsApi() {
  return requestClient.get<MaterialItem[]>('/material/options');
}

export function saveMaterialApi(data: Partial<MaterialItem>) {
  return requestClient.post('/material/save', data);
}

export function changeMaterialStatusApi(id: number, active: boolean) {
  return requestClient.put(`/material/${id}/status`, { active });
}

// ---------- 采购单 ----------
export interface PurchaseOrderItem {
  id: number;
  materialId: number;
  materialCode?: string;
  materialName?: string;
  spec?: string;
  unit?: string;
  qty: number | string;
  price: number | string;
  amount: number | string;
  priceSource?: string;
}

export interface PurchaseOrder {
  id: number;
  orderNo: string;
  storeId: number;
  storeCode?: string;
  storeName?: string;
  storeType?: string;
  orderDate: string;
  totalAmount: number | string;
  status: 'CANCELLED' | 'CONFIRMED' | 'PENDING' | 'RECEIVED' | 'SHIPPED';
  payStatus?: 'PAID' | 'UNPAID';
  receiptStatus?: 'CONFIRMED' | 'UNCONFIRMED';
  remark?: string;
  cancelReason?: string;
  confirmedAt?: string;
  paidAt?: string;
  receiptAt?: string;
  shippedAt?: string;
  receivedAt?: string;
  createdAt?: string;
}

export function createPurchaseApi(data: {
  items: Array<{ materialId: number; qty: number }>;
  remark?: string;
  storeId: number;
}) {
  return requestClient.post<string>('/purchase/orders', data);
}

export function getPurchasePageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<PurchaseOrder>>('/purchase/orders/page', { params });
}

export function getPurchaseDetailApi(id: number) {
  return requestClient.get<{ items: PurchaseOrderItem[]; order: PurchaseOrder }>(
    `/purchase/orders/${id}`,
  );
}

export function approvePurchaseApi(
  id: number,
  items: Array<{ itemId: number; qty: number }>,
) {
  return requestClient.put(`/purchase/orders/${id}/approve`, { items });
}

export function rejectPurchaseApi(id: number, reason: string) {
  return requestClient.put(`/purchase/orders/${id}/reject`, { reason });
}

export function payPurchaseApi(id: number) {
  return requestClient.put(`/purchase/orders/${id}/pay`);
}

export function confirmReceiptApi(id: number) {
  return requestClient.put(`/purchase/orders/${id}/confirm-receipt`);
}

export function shipPurchaseApi(id: number) {
  return requestClient.put(`/purchase/orders/${id}/ship`);
}

export function receivePurchaseApi(id: number, requestNo: string) {
  return requestClient.put(`/purchase/orders/${id}/receive`, { requestNo });
}

// ---------- 门店库存 ----------
export interface StockItem {
  storeId: number;
  storeCode?: string;
  storeName?: string;
  materialId: number;
  materialCode?: string;
  materialName?: string;
  spec?: string;
  unit?: string;
  currentQty: number | string;
  warnMinQty?: number | string;
  updatedAt?: string;
}

export function getStockPageApi(params: Record<string, any>) {
  return requestClient.get<PageResult<StockItem>>('/stock/page', { params });
}
