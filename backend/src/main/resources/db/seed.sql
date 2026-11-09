INSERT INTO sys_role (role_code, role_name, data_scope) VALUES
  ('HQ_ADMIN', '总部超级管理员', 'ALL'),
  ('HQ_OPS', '总部运营', 'ALL'),
  ('HQ_FIN', '总部财务', 'ALL'),
  ('FRANCHISEE_ADMIN', '加盟商管理员', 'FRANCHISEE'),
  ('STORE_OP', '门店操作员', 'STORE');

-- 密码统一为 admin123（BCrypt）
INSERT INTO sys_user (username, password, real_name, role_id) VALUES
  ('admin', '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '超级管理员', 1);

INSERT INTO franchise_level (level_code, level_name, sort_no) VALUES
  ('NORMAL', '普通', 10),
  ('SILVER', '白银', 20),
  ('GOLD', '黄金', 30),
  ('DIAMOND', '钻石', 40);

-- 组织测试数据（id 按插入顺序：加盟商 1/2，门店 1/2/3）
INSERT INTO franchisee (franchisee_code, name, contact_name, contact_phone, region, level_code, sign_date, deposit, status) VALUES
  ('F0001', '芳华美颜连锁', '张莉', '13800000001', '浙江省杭州市', 'GOLD', '2025-03-01', 50000.00, 'NORMAL'),
  ('F0002', '悦容健康管理', '王强', '13800000002', '江苏省南京市', 'NORMAL', '2025-06-15', 20000.00, 'NORMAL');

INSERT INTO store (store_code, name, store_type, franchisee_id, address, contact_name, contact_phone, open_date) VALUES
  ('S0001', '芳华美颜西湖店', 'JOIN', 1, '杭州市西湖区文三路 100 号', '陈敏', '13800000003', '2025-04-01'),
  ('S0002', '芳华美颜滨江店', 'JOIN', 1, '杭州市滨江区江南大道 200 号', '刘洋', '13800000004', '2025-05-10'),
  ('S0003', '方壶直营旗舰店', 'DIRECT', NULL, '上海市静安区南京西路 300 号', '赵倩', '13800000005', '2024-12-01');

-- 范围测试账号：总部运营 / 加盟商管理员（归属 F0001）/ 门店操作员（归属 S0001）
INSERT INTO sys_user (username, password, real_name, role_id, franchisee_id, store_id) VALUES
  ('ops01',   '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '运营一号', 2, NULL, NULL),
  ('fran01',  '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '张莉',     4, 1,    NULL),
  ('store01', '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '陈敏',     5, NULL, 1);

-- 商品种子：stock_qty 为 NULL 表示无限库存
INSERT INTO material (code, name, spec, unit, category, base_sale_price, direct_price, cost_price, warn_min_qty, stock_qty) VALUES
  ('M0001', '草本精油·舒络', '30ml/瓶', '瓶', '护理耗材', 128.00, 158.00, 60.00, 10.00, NULL),
  ('M0002', '艾灸贴',       '10片/盒', '盒', '护理耗材',  45.00,  58.00, 18.00, 20.00, 200.00),
  ('M0003', '竹纤维毛巾',   '70×35cm', '条', '门店耗材',  22.00,  28.00,  9.00, 30.00, NULL),
  ('M0004', '草本浴球',     '1个/袋',  '袋', '护理耗材',  36.00,  46.00, 14.00, 10.00,  50.00);

-- 黄金等级（F0001 使用）专属供货价，其余等级缺失时回退基准价
INSERT INTO material_level_price (material_id, level_code, sale_price)
SELECT id, 'GOLD', 118.00 FROM material WHERE code = 'M0001';
INSERT INTO material_level_price (material_id, level_code, sale_price)
SELECT id, 'GOLD', 40.00 FROM material WHERE code = 'M0002';
INSERT INTO material_level_price (material_id, level_code, sale_price)
SELECT id, 'GOLD', 20.00 FROM material WHERE code = 'M0003';
INSERT INTO material_level_price (material_id, level_code, sale_price)
SELECT id, 'GOLD', 32.00 FROM material WHERE code = 'M0004';
