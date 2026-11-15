-- MySQL 8.4 开发库初始化（持久化），脚本可重复执行
CREATE TABLE IF NOT EXISTS sys_role (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code  VARCHAR(32) NOT NULL UNIQUE,
  role_name  VARCHAR(32) NOT NULL,
  data_scope VARCHAR(16) NOT NULL,
  created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(64) NOT NULL UNIQUE,
  password      VARCHAR(128) NOT NULL,
  real_name     VARCHAR(32),
  role_id       BIGINT NOT NULL,
  franchisee_id BIGINT,
  store_id      BIGINT,
  status        VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS franchise_level (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  level_code  VARCHAR(16) NOT NULL UNIQUE,
  level_name  VARCHAR(32) NOT NULL,
  sort_no     INT DEFAULT 0,
  status      VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  remark      VARCHAR(255),
  created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS franchisee (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  franchisee_code VARCHAR(16) NOT NULL UNIQUE,
  name            VARCHAR(64) NOT NULL UNIQUE,
  contact_name    VARCHAR(32) NOT NULL,
  contact_phone   VARCHAR(32) NOT NULL,
  region          VARCHAR(64),
  address         VARCHAR(128),
  level_code      VARCHAR(16) NOT NULL,
  sign_date       DATE NOT NULL,
  expire_date     DATE,
  deposit         DECIMAL(12,2) DEFAULT 0,
  status          VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  contract_url    TEXT,
  remark          VARCHAR(255),
  created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_code    VARCHAR(16) NOT NULL UNIQUE,
  name          VARCHAR(64) NOT NULL,
  store_type    VARCHAR(16) NOT NULL,
  franchisee_id BIGINT,
  address       VARCHAR(128),
  contact_name  VARCHAR(32),
  contact_phone VARCHAR(32),
  open_date     DATE,
  is_active     TINYINT(1) DEFAULT 1,
  created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_store_name UNIQUE (franchisee_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS material (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  code            VARCHAR(16) NOT NULL UNIQUE,
  name            VARCHAR(64) NOT NULL,
  spec            VARCHAR(64),
  unit            VARCHAR(16) NOT NULL,
  category        VARCHAR(32),
  base_sale_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  direct_price    DECIMAL(12,2) NOT NULL DEFAULT 0,
  cost_price      DECIMAL(12,2) DEFAULT 0,
  warn_min_qty    DECIMAL(12,2) DEFAULT 0,
  stock_qty       DECIMAL(12,2),
  image_url       VARCHAR(255),
  is_active       TINYINT(1) DEFAULT 1,
  created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS material_level_price (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  level_code  VARCHAR(16) NOT NULL,
  sale_price  DECIMAL(12,2) NOT NULL,
  created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_price UNIQUE (material_id, level_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sale_order (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no               VARCHAR(32) NOT NULL UNIQUE,
  store_id               BIGINT NOT NULL,
  store_type_snapshot    VARCHAR(16) NOT NULL,
  franchisee_id_snapshot BIGINT,
  order_date             DATE NOT NULL,
  total_amount           DECIMAL(14,2) NOT NULL DEFAULT 0,
  status                 VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  pay_status             VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
  receipt_status         VARCHAR(16) NOT NULL DEFAULT 'UNCONFIRMED',
  voucher_url            TEXT,
  remark                 VARCHAR(255),
  confirmed_at           DATETIME(3),
  paid_at                DATETIME(3),
  receipt_at             DATETIME(3),
  shipped_at             DATETIME(3),
  received_at            DATETIME(3),
  cancel_reason          VARCHAR(255),
  created_by             BIGINT NOT NULL,
  created_at             DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at             DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sale_order_item (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id     BIGINT NOT NULL,
  material_id  BIGINT NOT NULL,
  qty          DECIMAL(12,2) NOT NULL,
  price        DECIMAL(12,2) NOT NULL,
  amount       DECIMAL(14,2) NOT NULL,
  price_source VARCHAR(16) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store_stock (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id    BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  current_qty DECIMAL(12,2) NOT NULL DEFAULT 0,
  updated_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_stock UNIQUE (store_id, material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS stock_flow (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id        BIGINT NOT NULL,
  material_id     BIGINT NOT NULL,
  flow_type       VARCHAR(16) NOT NULL,
  relate_order_id BIGINT,
  request_no      VARCHAR(64),
  qty             DECIMAL(12,2) NOT NULL,
  created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_stock_flow_request UNIQUE (request_no, material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store_package (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id         BIGINT NOT NULL,
  franchisee_id    BIGINT,
  package_name     VARCHAR(64) NOT NULL,
  duration_minutes INT NOT NULL,
  price            DECIMAL(12,2) NOT NULL DEFAULT 0,
  status           VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  remark           VARCHAR(255),
  created_by       BIGINT,
  created_at       DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store_package_item (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  package_id    BIGINT NOT NULL,
  store_id      BIGINT NOT NULL,
  franchisee_id BIGINT,
  material_id   BIGINT NOT NULL,
  qty           DECIMAL(12,2) NOT NULL,
  item_type     VARCHAR(16) NOT NULL DEFAULT 'FIXED',
  created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_package_material UNIQUE (package_id, material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store_writeoff (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  writeoff_no   VARCHAR(32) NOT NULL UNIQUE,
  store_id      BIGINT NOT NULL,
  franchisee_id BIGINT,
  package_id    BIGINT NOT NULL,
  package_name  VARCHAR(64) NOT NULL,
  package_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  beautician    VARCHAR(32),
  request_no    VARCHAR(64) NOT NULL UNIQUE,
  remark        VARCHAR(255),
  created_by    BIGINT,
  created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS store_writeoff_item (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  writeoff_id   BIGINT NOT NULL,
  store_id      BIGINT NOT NULL,
  franchisee_id BIGINT,
  material_id   BIGINT NOT NULL,
  qty           DECIMAL(12,2) NOT NULL,
  item_type     VARCHAR(16) NOT NULL,
  created_at    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT,
  username     VARCHAR(64),
  operate_type VARCHAR(32),
  content      TEXT,
  old_value    TEXT,
  new_value    TEXT,
  ip           VARCHAR(64),
  created_at   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
