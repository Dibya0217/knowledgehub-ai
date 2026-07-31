import api from './axios'
import type { ApiResponse, ChatRequest, ChatResponse, ConversationSummary, MessageDTO } from '@/types'

export const chatApi = {
  send: (data: ChatRequest) =>
    api.post<ApiResponse<ChatResponse>>('/chat', data).then((r) => r.data),

  getConversations: () =>
    api.get<ApiResponse<ConversationSummary[]>>('/chat/conversations').then((r) => r.data),

  getMessages: (conversationId: string) =>
    api.get<ApiResponse<MessageDTO[]>>(`/chat/${conversationId}/messages`).then((r) => r.data),

  deleteConversation: (conversationId: string) =>
    api.delete(`/chat/${conversationId}`).then((r) => r.data),
}
