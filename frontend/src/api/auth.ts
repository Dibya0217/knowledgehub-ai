import api from './axios'
import type { ApiResponse, AuthTokens, LoginRequest, RegisterRequest, ForgotPasswordRequest, UserProfile } from '@/types'

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthTokens>>('/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    api.post<ApiResponse<null>>('/auth/register', data).then((r) => r.data),

  forgotPassword: (data: ForgotPasswordRequest) =>
    api.post<ApiResponse<null>>('/auth/forgot-password', data).then((r) => r.data),

  resetPassword: (email: string, otp: string, newPassword: string) =>
    api.post<ApiResponse<null>>('/auth/reset-password', { email, otp, newPassword }).then((r) => r.data),

  sendVerification: (email: string) =>
    api.post<ApiResponse<null>>('/auth/send-verification', { email }).then((r) => r.data),

  verifyEmail: (email: string, otp: string) =>
    api.post<ApiResponse<null>>('/auth/verify-email', { email, otp }).then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }).then((r) => r.data),

  me: () =>
    api.get<ApiResponse<UserProfile>>('/users/me').then((r) => r.data),
}
