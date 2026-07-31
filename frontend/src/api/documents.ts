import api from './axios'
import type { ApiResponse, DocumentDTO } from '@/types'

export const documentsApi = {
  upload: (file: File, onProgress?: (pct: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<ApiResponse<DocumentDTO>>('/documents/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      },
    }).then((r) => r.data)
  },

  list: () =>
    api.get<ApiResponse<DocumentDTO[]>>('/documents').then((r) => r.data),

  getStatus: (id: string) =>
    api.get<ApiResponse<DocumentDTO>>(`/documents/${id}/status`).then((r) => r.data),

  delete: (id: string) =>
    api.delete(`/documents/${id}`).then((r) => r.data),
}
