import { useState, useEffect, useRef, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { Brain, Sparkles } from 'lucide-react'
import { toast } from 'sonner'
import { chatApi } from '@/api/chat'
import { useSSE } from '@/hooks/useSSE'
import { ConversationList } from '@/components/chat/ConversationList'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { ChatInput } from '@/components/chat/ChatInput'
import { Skeleton } from '@/components/ui/Skeleton'
import type { MessageDTO } from '@/types'

export function ChatPage() {
  const [activeConversationId, setActiveConversationId] = useState<string | undefined>()
  const [streamingContent, setStreamingContent] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [localMessages, setLocalMessages] = useState<MessageDTO[]>([])
  const bottomRef = useRef<HTMLDivElement>(null)
  const qc = useQueryClient()

  const { data: conversationsData } = useQuery({
    queryKey: ['conversations'],
    queryFn: () => chatApi.getConversations(),
    select: (r) => r.data.content,
  })

  const { data: messagesData, isLoading: messagesLoading } = useQuery({
    queryKey: ['messages', activeConversationId],
    queryFn: () => chatApi.getMessages(activeConversationId!),
    enabled: !!activeConversationId,
    select: (r) => r.data,
  })

  useEffect(() => {
    if (messagesData) setLocalMessages(messagesData)
  }, [messagesData])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [localMessages, streamingContent])

  const deleteMutation = useMutation({
    mutationFn: chatApi.deleteConversation,
    onSuccess: (_, id) => {
      if (activeConversationId === id) {
        setActiveConversationId(undefined)
        setLocalMessages([])
      }
      qc.invalidateQueries({ queryKey: ['conversations'] })
      toast.success('Conversation deleted')
    },
  })

  const onToken = useCallback((token: string) => {
    setStreamingContent((prev) => prev + token)
  }, [])

  const onConversationId = useCallback((id: string) => {
    setActiveConversationId(id)
    setIsStreaming(false)
    setStreamingContent('')
    qc.invalidateQueries({ queryKey: ['conversations'] })
    qc.invalidateQueries({ queryKey: ['messages', id] })
  }, [qc])

  const onComplete = useCallback(() => {
    setIsStreaming(false)
    setStreamingContent('')
  }, [])

  const onError = useCallback((err: string) => {
    setIsStreaming(false)
    setStreamingContent('')
    toast.error(`Stream error: ${err}`)
  }, [])

  const { stream, abort } = useSSE({ onToken, onComplete, onError, onConversationId })

  const handleAbort = useCallback(() => {
    abort()
    if (streamingContent.trim()) {
      setLocalMessages((prev) => [
        ...prev,
        {
          id: `aborted-${Date.now()}`,
          role: 'ASSISTANT' as const,
          content: streamingContent,
          createdAt: new Date().toISOString(),
        },
      ])
    }
    setIsStreaming(false)
    setStreamingContent('')
  }, [abort, streamingContent])

  async function handleSend(question: string) {
    const userMsg: MessageDTO = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: question,
      createdAt: new Date().toISOString(),
    }
    setLocalMessages((prev) => [...prev, userMsg])
    setStreamingContent('')
    setIsStreaming(true)

    await stream(question, activeConversationId)
  }

  function handleSelectConversation(id: string) {
    setActiveConversationId(id)
    setStreamingContent('')
    setIsStreaming(false)
  }

  function handleNewChat() {
    setActiveConversationId(undefined)
    setLocalMessages([])
    setStreamingContent('')
    setIsStreaming(false)
  }

  const conversations = conversationsData ?? []
  const showWelcome = !activeConversationId && localMessages.length === 0

  return (
    <div className="flex h-full">
      <ConversationList
        conversations={conversations}
        activeId={activeConversationId}
        onSelect={handleSelectConversation}
        onNew={handleNewChat}
        onDelete={(id) => deleteMutation.mutate(id)}
      />

      {/* Chat area */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <AnimatePresence mode="wait">
            {showWelcome ? (
              <motion.div
                key="welcome"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="h-full flex flex-col items-center justify-center gap-6 text-center"
              >
                <motion.div
                  animate={{ scale: [1, 1.05, 1] }}
                  transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                  className="w-20 h-20 rounded-3xl gradient-bg flex items-center justify-center shadow-2xl shadow-indigo-500/40"
                >
                  <Brain className="w-10 h-10 text-white" />
                </motion.div>
                <div>
                  <h2 className="text-2xl font-bold gradient-text mb-2">How can I help?</h2>
                  <p className="text-sm text-[rgb(var(--text-secondary))] max-w-sm">
                    Ask me anything about your uploaded documents. I'll search through them and give you accurate answers with sources.
                  </p>
                </div>
                <div className="flex items-center gap-2 text-xs text-[rgb(var(--text-secondary))] glass rounded-full px-4 py-2">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                  Powered by RAG · Citations included
                </div>
              </motion.div>
            ) : (
              <motion.div key="messages" className="flex flex-col gap-4 max-w-3xl mx-auto">
                {messagesLoading
                  ? Array.from({ length: 3 }).map((_, i) => (
                      <div key={i} className="flex gap-3">
                        <Skeleton className="w-8 h-8 rounded-xl flex-shrink-0" />
                        <div className="flex-1 flex flex-col gap-2">
                          <Skeleton className="h-10 w-3/4 rounded-2xl" />
                        </div>
                      </div>
                    ))
                  : localMessages.map((msg) => (
                      <MessageBubble key={msg.id} message={msg} />
                    ))}

                {/* Streaming bubble */}
                {isStreaming && streamingContent && (
                  <MessageBubble
                    message={{
                      id: 'streaming',
                      role: 'ASSISTANT',
                      content: streamingContent,
                      createdAt: new Date().toISOString(),
                    }}
                    isStreaming
                  />
                )}

                {/* Thinking indicator */}
                {isStreaming && !streamingContent && (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="flex items-center gap-3"
                  >
                    <div className="w-8 h-8 rounded-xl gradient-bg flex items-center justify-center">
                      <Brain className="w-4 h-4 text-white" />
                    </div>
                    <div className="glass rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-1.5">
                      {[0, 1, 2].map((i) => (
                        <motion.span
                          key={i}
                          animate={{ opacity: [0.3, 1, 0.3] }}
                          transition={{ duration: 1, repeat: Infinity, delay: i * 0.2 }}
                          className="w-1.5 h-1.5 rounded-full bg-indigo-400"
                        />
                      ))}
                    </div>
                  </motion.div>
                )}

                <div ref={bottomRef} />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <ChatInput
          onSend={handleSend}
          onAbort={handleAbort}
          isStreaming={isStreaming}
        />
      </div>
    </div>
  )
}
