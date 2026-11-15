import { requestClient } from '#/api/request';

export interface PackageRank {
  packageName: string;
  writeoffCount: number;
  revenue: number | string;
}

export interface ProductRank {
  materialId: number;
  materialName: string;
  imageUrl?: string;
  unit?: string;
  qty: number | string;
}

export interface BeauticianRank {
  beautician: string;
  writeoffCount: number;
  revenue: number | string;
}

export interface WriteoffDashboard {
  totalWriteoffs: number;
  totalRevenue: number | string;
  totalQty: number | string;
  packages: PackageRank[];
  products: ProductRank[];
  beauticians: BeauticianRank[];
}

export function getWriteoffDashboardApi(days = 30) {
  return requestClient.get<WriteoffDashboard>('/report/writeoff-dashboard', {
    params: { days },
  });
}
