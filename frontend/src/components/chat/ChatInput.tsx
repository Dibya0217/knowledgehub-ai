import { useState, useRef, type KeyboardEvent } from 'react'
import { motion } from 'framer-motion'
import { Send, Square } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ChatInputProps {
  onSend: (message: string) => void
  onAbort?: () => void
  isStreaming: boolean
  disabled?: boolean
}

export function ChatInput({ onSend, onAbort, isStreaming, disabled }: ChatInputProps) {
  const [value, setValue] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  function handleSend() {
    const trimmed = value.trim()
    if (!trimmed || isStreaming || disabled) return
    onSend(trimmed)
    setValue('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }

  function handleKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  function handleInput() {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`
  }

  return (
    <div className="p-4 border-t border-white/5">
      <div className="relative glass-strong rounded-2xl overflow-hidden gradient-border">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKey}
          onInput={handleInput}
          placeholder="Ask anything about your documents..."
          rows={1}
          disabled={disabled}
          className={cn(
            'w-full bg-transparent px-4 py-3 pr-14 text-sm resize-none outline-none',
            'text-[rgb(var(--text-primary))] placeholder:text-[rgb(var(--text-secondary))]',
            'max-h-40 scrollbar-thin',
            disabled && 'opacity-50',
          )}
        />

        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={isStreaming ? onAbort : handleSend}
          disabled={!isStreaming && (!value.trim() || disabled)}
          className={cn(
            'absolute right-3 bottom-3 w-8 h-8 rounded-xl flex items-center justify-center transition-all duration-200',
            isStreaming
              ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
              : value.trim() && !disabled
              ? 'gradient-bg text-white shadow-lg shadow-indigo-500/30'
              : 'bg-white/5 text-[rgb(var(--text-secondary))] cursor-not-allowed',
          )}
        >
          {isStreaming ? <Square className="w-3.5 h-3.5" /> : <Send className="w-3.5 h-3.5" />}
        </motion.button>
      </div>
      <p className="text-xs text-center text-[rgb(var(--text-secondary))] opacity-40 mt-2">
        Enter to send · Shift+Enter for new line
      </p>
    </div>
  )
}
