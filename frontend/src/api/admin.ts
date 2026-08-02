import api from './axios'
import type { ApiResponse, UserProfile } from '@/types'

export interface AdminStats {
  totalUsers: number
  totalDocuments: number
  totalConversations: number
  totalMessages: number
}

export interface PagedUsers {
  content: UserProfile[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const adminApi = {
  getStats: () => api.get<ApiResponse<AdminStats>>('/admin/stats'),
  getUsers: (page = 0, size = 20) =>
    api.get<ApiResponse<PagedUsers>>('/admin/users', { params: { page, size } }),
}
