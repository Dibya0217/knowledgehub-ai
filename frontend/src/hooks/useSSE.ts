import { useState, useCallback, useRef } from 'react'
import { useAuthStore } from '@/store/authStore'

interface UseSSEOptions {
  onToken: (token: string) => void
  onComplete: () => void
  onError: (err: string) => void
}

export function useSSE({ onToken, onComplete, onError }: UseSSEOptions) {
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

      try {
        const response = await fetch(`/api/v1/chat/stream?${params}`, {
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
              const data = line.slice(5).trim()
              if (data) onToken(data)
            }
          }
        }

        onComplete()
      } catch (err) {
        if ((err as Error).name !== 'AbortError') {
          onError((err as Error).message)
        }
      } finally {
        setIsStreaming(false)
      }
    },
    [onToken, onComplete, onError],
  )

  const abort = useCallback(() => {
    abortRef.current?.abort()
    setIsStreaming(false)
  }, [])

  return { stream, abort, isStreaming }
}
