-- MySQL 8.4 开发库种子数据（INSERT IGNORE，重启不覆盖已有业务数据）
INSERT IGNORE INTO sys_role (id, role_code, role_name, data_scope) VALUES
  (1, 'HQ_ADMIN', '总部超级管理员', 'ALL'),
  (2, 'HQ_OPS', '总部运营', 'ALL'),
  (3, 'HQ_FIN', '总部财务', 'ALL'),
  (4, 'FRANCHISEE_ADMIN', '加盟商管理员', 'FRANCHISEE'),
  (5, 'STORE_OP', '门店操作员', 'STORE');

-- 密码统一为 admin123（BCrypt）
INSERT IGNORE INTO sys_user (id, username, password, real_name, role_id) VALUES
  (1, 'admin', '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '超级管理员', 1);

INSERT IGNORE INTO franchise_level (level_code, level_name, sort_no) VALUES
  ('NORMAL', '普通', 10),
  ('SILVER', '白银', 20),
  ('GOLD', '黄金', 30),
  ('DIAMOND', '钻石', 40);

INSERT IGNORE INTO franchisee (id, franchisee_code, name, contact_name, contact_phone, region, level_code, sign_date, deposit, status) VALUES
  (1, 'F0001', '芳华美颜连锁', '张莉', '13800000001', '浙江省杭州市', 'GOLD', '2025-03-01', 50000.00, 'NORMAL'),
  (2, 'F0002', '悦容健康管理', '王强', '13800000002', '江苏省南京市', 'NORMAL', '2025-06-15', 20000.00, 'NORMAL');

INSERT IGNORE INTO store (id, store_code, name, store_type, franchisee_id, address, contact_name, contact_phone, open_date) VALUES
  (1, 'S0001', '芳华美颜西湖店', 'JOIN', 1, '杭州市西湖区文三路 100 号', '陈敏', '13800000003', '2025-04-01'),
  (2, 'S0002', '方壶FangHu SPA(新嘉中心店)', 'JOIN', 1, '杭州市滨江区江南大道 200 号', '刘洋', '13800000004', '2025-05-10'),
  (3, 'S0003', '方壶直营旗舰店', 'DIRECT', NULL, '上海市静安区南京西路 300 号', '赵倩', '13800000005', '2024-12-01'),
  (4, 'S0004', '方壶FangHu SPA(滨江天街店)', 'JOIN', 1, '杭州市滨江区江汉路 1515 号滨江天街', '王磊', '13800000006', '2025-07-01');

-- 范围测试账号：总部运营 / 加盟商管理员（归属 F0001）/ 门店操作员（归属 S0001）
INSERT IGNORE INTO sys_user (id, username, password, real_name, role_id, franchisee_id, store_id) VALUES
  (2, 'ops01',   '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '运营一号', 2, NULL, NULL),
  (3, 'fran01',  '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '张莉',     4, 1,    NULL),
  (4, 'store01', '$2a$10$GGRfpqAc51jC0.jDmcWF.uTESPrw1fdNLx39ymQ2aa7BnNu6bGBAe', '陈敏',     5, NULL, 1);

-- 演示账号：总部 zongbu / 门店 mendian、mendian2，密码均为 123456
INSERT IGNORE INTO sys_user (id, username, password, real_name, role_id, franchisee_id, store_id) VALUES
  (5, 'zongbu',  '$2a$10$19lgFquoQVxoNfkzpOTWJui1VQEBH33rAf6XnZ798.7.PLaJ8VC5K', '总部测试', 1, NULL, NULL),
  (6, 'mendian', '$2a$10$19lgFquoQVxoNfkzpOTWJui1VQEBH33rAf6XnZ798.7.PLaJ8VC5K', '门店测试', 5, NULL, 2),
  (7, 'mendian2','$2a$10$19lgFquoQVxoNfkzpOTWJui1VQEBH33rAf6XnZ798.7.PLaJ8VC5K', '门店测试2', 5, NULL, 4);

-- 商品种子：stock_qty 为 NULL 表示无限库存
INSERT IGNORE INTO material (id, code, name, spec, unit, category, base_sale_price, direct_price, cost_price, warn_min_qty, stock_qty, image_url) VALUES
  (1, 'M0001', '草本精油·舒络', '30ml/瓶', '瓶', '护理耗材', 128.00, 158.00, 60.00, 10.00, NULL,
   'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?image_size=square&prompt=amber%20glass%20essential%20oil%20bottle%20with%20fresh%20herbs%20spa%20product%20photography%20white%20background'),
  (2, 'M0002', '艾灸贴',       '10片/盒', '盒', '护理耗材',  45.00,  58.00, 18.00, 20.00, 200.00,
   'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?image_size=square&prompt=box%20of%20moxibustion%20patches%20wellness%20product%20packaging%20white%20background'),
  (3, 'M0003', '竹纤维毛巾',   '70×35cm', '条', '门店耗材',  22.00,  28.00,  9.00, 30.00, NULL,
   'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?image_size=square&prompt=folded%20bamboo%20fiber%20spa%20towel%20beige%20white%20background'),
  (4, 'M0004', '草本浴球',     '1个/袋',  '袋', '护理耗材',  36.00,  46.00, 14.00, 10.00,  50.00,
   'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?image_size=square&prompt=herbal%20bath%20bomb%20ball%20in%20linen%20bag%20spa%20product%20white%20background');

INSERT IGNORE INTO material_level_price (material_id, level_code, sale_price) VALUES
  (1, 'GOLD', 118.00),
  (2, 'GOLD', 40.00),
  (3, 'GOLD', 20.00),
  (4, 'GOLD', 32.00);
