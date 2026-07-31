import api from './axios'
import type { ApiResponse, AuthTokens, LoginRequest, RegisterRequest, ForgotPasswordRequest, UserProfile } from '@/types'

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthTokens>>('/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    api.post<ApiResponse<{ message: string }>>('/auth/register', data).then((r) => r.data),

  forgotPassword: (data: ForgotPasswordRequest) =>
    api.post<ApiResponse<{ message: string }>>('/auth/forgot-password', data).then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }).then((r) => r.data),

  me: () =>
    api.get<ApiResponse<UserProfile>>('/users/me').then((r) => r.data),
}
