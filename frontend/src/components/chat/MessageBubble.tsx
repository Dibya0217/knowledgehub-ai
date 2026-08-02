import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Brain, User, FileText, ChevronDown, ChevronUp } from 'lucide-react'
import { cn } from '@/lib/utils'
import { formatDate } from '@/lib/utils'
import type { MessageDTO } from '@/types'

interface MessageBubbleProps {
  message: MessageDTO
  isStreaming?: boolean
}

export function MessageBubble({ message, isStreaming }: MessageBubbleProps) {
  const isUser = message.role === 'USER'
  const hasCitations = message.citations && message.citations.length > 0
  const [citationsOpen, setCitationsOpen] = useState(false)

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      className={cn('flex gap-3', isUser && 'flex-row-reverse')}
    >
      {/* Avatar */}
      <div
        className={cn(
          'flex-shrink-0 w-8 h-8 rounded-xl flex items-center justify-center text-white shadow-lg',
          isUser ? 'bg-indigo-500 shadow-indigo-500/30' : 'gradient-bg shadow-purple-500/30',
        )}
      >
        {isUser ? <User className="w-4 h-4" /> : <Brain className="w-4 h-4" />}
      </div>

      {/* Bubble */}
      <div className={cn('flex flex-col gap-1 max-w-[75%]', isUser && 'items-end')}>
        <div
          className={cn(
            'rounded-2xl px-4 py-3 text-sm leading-relaxed',
            isUser
              ? 'gradient-bg text-white rounded-tr-sm shadow-lg shadow-indigo-500/20'
              : 'glass rounded-tl-sm text-[rgb(var(--text-primary))]',
          )}
        >
          <p className="whitespace-pre-wrap break-words">
            {message.content}
            {isStreaming && <span className="typing-cursor ml-0.5" />}
          </p>
        </div>

        {/* Citations toggle */}
        {hasCitations && (
          <div className="flex flex-col gap-1 mt-1 w-full">
            <button
              onClick={() => setCitationsOpen((o) => !o)}
              className="flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 transition-colors px-1 self-start"
            >
              <FileText className="w-3.5 h-3.5" />
              {message.citations!.length} source{message.citations!.length !== 1 ? 's' : ''}
              {citationsOpen ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
            </button>

            <AnimatePresence>
              {citationsOpen && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.2 }}
                  className="flex flex-col gap-1 overflow-hidden"
                >
                  {message.citations!.map((cite, i) => (
                    <div
                      key={i}
                      className="flex items-start gap-2 glass rounded-xl px-3 py-2 text-xs text-[rgb(var(--text-secondary))]"
                    >
                      <FileText className="w-3.5 h-3.5 flex-shrink-0 mt-0.5 text-indigo-400" />
                      <div>
                        <span className="font-medium text-[rgb(var(--text-primary))]">{cite.filename}</span>
                        {cite.excerpt && (
                          <p className="mt-0.5 opacity-70 line-clamp-3">{cite.excerpt}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        <span className="text-xs text-[rgb(var(--text-secondary))] opacity-50 px-1">
          {formatDate(message.createdAt)}
        </span>
      </div>
    </motion.div>
  )
}