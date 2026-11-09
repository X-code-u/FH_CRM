/** 金额展示统一两位小数，页面内不做四舍五入 */
export function formatAmount(v: null | number | string | undefined): string {
  if (v === null || v === undefined || v === '') return '0.00';
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(2) : '0.00';
}

/** 幂等号：入库确认等提交前生成，按钮点击即生成一次 */
export function newRequestNo(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
