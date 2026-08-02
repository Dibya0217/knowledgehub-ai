export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  correlationId?: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export interface UserProfile {
  id: string
  email: string
  name: string
  provider: string
  emailVerified: boolean
  createdAt: string
  roles: string[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
  confirmPassword: string
}

export type MessageRole = 'USER' | 'ASSISTANT'

export interface CitationDTO {
  documentId: string
  filename: string
  chunkIndex: number
  excerpt: string
}

export interface MessageDTO {
  id: string
  role: MessageRole
  content: string
  createdAt: string
  citations?: CitationDTO[]
}

export interface ConversationSummary {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messageCount: number
}

export interface ChatRequest {
  question: string
  conversationId?: string
}

export interface ChatResponse {
  conversationId: string
  messageId: string
  answer: string
  sources: SourceReference[]
}

export interface SourceReference {
  documentId: string
  filename: string
  chunkIndex: number
  score: number
}

export interface DocumentDTO {
  id: string
  filename: string
  originalName: string
  fileType: string
  fileSize: number
  status: 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED'
  createdAt: string
  updatedAt: string
}

export interface UpdateProfileRequest {
  name: string
}
