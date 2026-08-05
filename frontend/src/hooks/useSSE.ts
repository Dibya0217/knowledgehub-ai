import { useState, useCallback, useRef } from 'react'
import { useAuthStore } from '@/store/authStore'

interface UseSSEOptions {
  onToken: (token: string) => void
  onComplete: (conversationId?: string) => void
  onError: (err: string) => void
  onConversationId?: (id: string) => void
}

export function useSSE({ onToken, onComplete, onError, onConversationId }: UseSSEOptions) {
  const [isStreaming, setIsStreaming] = useState(false)
  const abortRef = useRef<AbortController | null>(null)

  const stream = useCallback(
    async (question: string, conversationId?: string) => {
      abortRef.current?.abort()
      const abort = new AbortController()
      abortRef.current = abort

      setIsStreaming(true)

      const token = useAuthStore.getState().accessToken
      const params = new URLSearchParams({ question })
      if (conversationId) params.set('conversationId', conversationId)

      let resolvedConvId: string | undefined = conversationId

      try {
        const baseUrl = import.meta.env.VITE_API_URL ?? ''
        const response = await fetch(`${baseUrl}/api/v1/chat/stream?${params}`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: abort.signal,
        })

        if (!response.ok) throw new Error(`HTTP ${response.status}`)

        const reader = response.body!.getReader()
        const decoder = new TextDecoder()

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          const chunk = decoder.decode(value, { stream: true })
          const lines = chunk.split('\n')

          for (const line of lines) {
            if (line.startsWith('data:')) {
              // Spring SSE writes "data:<value>" (no space) — slice(5) preserves leading space in token
              const data = line.slice(5)
              if (!data) continue

              // Final event carrying conversationId — intercept, don't render
              if (data.startsWith('[CONV:') && data.endsWith(']')) {
                const convId = data.slice(6, -1)
                resolvedConvId = convId
                onConversationId?.(convId)
                continue
              }

              onToken(data)
            }
          }
        }

        onComplete(resolvedConvId)
      } catch (err) {
        if ((err as Error).name === 'AbortError') {
          onComplete(resolvedConvId)
        } else {
          onError((err as Error).message)
        }
      } finally {
        setIsStreaming(false)
      }
    },
    [onToken, onComplete, onError, onConversationId],
  )

  const abort = useCallback(() => {
    abortRef.current?.abort()
    setIsStreaming(false)
  }, [])

  return { stream, abort, isStreaming }
}
