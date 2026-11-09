import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 后端 /auth/login 返回 { token } */
  export interface LoginResult {
    token: string;
  }
}

/**
 * 登录（Sa-Token 令牌）
 */
export async function loginApi(data: AuthApi.LoginParams) {
  return requestClient.post<AuthApi.LoginResult>('/auth/login', data);
}

/**
 * 刷新accessToken（后端暂不支持，vben 默认关闭 refresh 功能，保留占位）
 */
export async function refreshTokenApi() {
  return baseRequestClient.post('/auth/refresh', undefined);
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return baseRequestClient.post('/auth/logout', undefined);
}

/**
 * 获取用户权限码（角色码 + 数据范围）
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
