-- H2 (PostgreSQL 兼容模式) 开发库初始化，与 db/schema.sql 保持同步
CREATE TABLE IF NOT EXISTS sys_role (
  id         bigserial PRIMARY KEY,
  role_code  varchar(32) NOT NULL UNIQUE,
  role_name  varchar(32) NOT NULL,
  data_scope varchar(16) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_user (
  id            bigserial PRIMARY KEY,
  username      varchar(64) NOT NULL UNIQUE,
  password      varchar(128) NOT NULL,
  real_name     varchar(32),
  role_id       bigint NOT NULL,
  franchisee_id bigint,
  store_id      bigint,
  status        varchar(16) NOT NULL DEFAULT 'ENABLED',
  created_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at    TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS franchise_level (
  id          bigserial PRIMARY KEY,
  level_code  varchar(16) NOT NULL UNIQUE,
  level_name  varchar(32) NOT NULL,
  sort_no     int DEFAULT 0,
  status      varchar(16) NOT NULL DEFAULT 'ENABLED',
  remark      varchar(255),
  created_at  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS franchisee (
  id              bigserial PRIMARY KEY,
  franchisee_code varchar(16) NOT NULL UNIQUE,
  name            varchar(64) NOT NULL UNIQUE,
  contact_name    varchar(32) NOT NULL,
  contact_phone   varchar(32) NOT NULL,
  region          varchar(64),
  address         varchar(128),
  level_code      varchar(16) NOT NULL,
  sign_date       date NOT NULL,
  expire_date     date,
  deposit         numeric(12,2) DEFAULT 0,
  status          varchar(16) NOT NULL DEFAULT 'NORMAL',
  contract_url    text,
  remark          varchar(255),
  created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS store (
  id            bigserial PRIMARY KEY,
  store_code    varchar(16) NOT NULL UNIQUE,
  name          varchar(64) NOT NULL,
  store_type    varchar(16) NOT NULL,
  franchisee_id bigint,
  address       varchar(128),
  contact_name  varchar(32),
  contact_phone varchar(32),
  open_date     date,
  is_active     boolean DEFAULT true,
  created_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uk_store_name UNIQUE (franchisee_id, name)
);

CREATE TABLE IF NOT EXISTS material (
  id              bigserial PRIMARY KEY,
  code            varchar(16) NOT NULL UNIQUE,
  name            varchar(64) NOT NULL,
  spec            varchar(64),
  unit            varchar(16) NOT NULL,
  category        varchar(32),
  base_sale_price numeric(12,2) NOT NULL DEFAULT 0,
  direct_price    numeric(12,2) NOT NULL DEFAULT 0,
  cost_price      numeric(12,2) DEFAULT 0,
  warn_min_qty    numeric(12,2) DEFAULT 0,
  stock_qty       numeric(12,2),
  is_active       boolean DEFAULT true,
  created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS material_level_price (
  id          bigserial PRIMARY KEY,
  material_id bigint NOT NULL,
  level_code  varchar(16) NOT NULL,
  sale_price  numeric(12,2) NOT NULL,
  created_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uk_price UNIQUE (material_id, level_code)
);

CREATE TABLE IF NOT EXISTS sale_order (
  id                     bigserial PRIMARY KEY,
  order_no               varchar(32) NOT NULL UNIQUE,
  store_id               bigint NOT NULL,
  store_type_snapshot    varchar(16) NOT NULL,
  franchisee_id_snapshot bigint,
  order_date             date NOT NULL,
  total_amount           numeric(14,2) NOT NULL DEFAULT 0,
  status                 varchar(16) NOT NULL DEFAULT 'PENDING',
  voucher_url            text,
  remark                 varchar(255),
  confirmed_at           TIMESTAMP WITH TIME ZONE,
  cancel_reason          varchar(255),
  created_by             bigint NOT NULL,
  created_at             TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at             TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sale_order_item (
  id           bigserial PRIMARY KEY,
  order_id     bigint NOT NULL,
  material_id  bigint NOT NULL,
  qty          numeric(12,2) NOT NULL,
  price        numeric(12,2) NOT NULL,
  amount       numeric(14,2) NOT NULL,
  price_source varchar(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS store_stock (
  id          bigserial PRIMARY KEY,
  store_id    bigint NOT NULL,
  material_id bigint NOT NULL,
  current_qty numeric(12,2) NOT NULL DEFAULT 0,
  updated_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uk_stock UNIQUE (store_id, material_id)
);

CREATE TABLE IF NOT EXISTS stock_flow (
  id              bigserial PRIMARY KEY,
  store_id        bigint NOT NULL,
  material_id     bigint NOT NULL,
  flow_type       varchar(16) NOT NULL,
  relate_order_id bigint,
  request_no      varchar(64),
  qty             numeric(12,2) NOT NULL,
  created_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_log (
  id           bigserial PRIMARY KEY,
  user_id      bigint,
  username     varchar(64),
  operate_type varchar(32),
  content      text,
  old_value    text,
  new_value    text,
  ip           varchar(64),
  created_at   TIMESTAMP WITH TIME ZONE DEFAULT now()
);
