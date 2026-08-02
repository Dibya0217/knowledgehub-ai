import { useState, useRef, type KeyboardEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Send, Square, Paperclip, X, FileText, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { documentsApi } from '@/api/documents'
import { useQueryClient } from '@tanstack/react-query'

interface ChatInputProps {
  onSend: (message: string) => void
  onAbort?: () => void
  isStreaming: boolean
  disabled?: boolean
}

interface UploadingFile {
  name: string
  progress: number
  done: boolean
}

export function ChatInput({ onSend, onAbort, isStreaming, disabled }: ChatInputProps) {
  const [value, setValue] = useState('')
  const [uploading, setUploading] = useState<UploadingFile | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const qc = useQueryClient()

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

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''

    setUploading({ name: file.name, progress: 0, done: false })

    try {
      await documentsApi.upload(file, (pct) =>
        setUploading((prev) => prev ? { ...prev, progress: pct } : null)
      )
      setUploading((prev) => prev ? { ...prev, done: true } : null)
      qc.invalidateQueries({ queryKey: ['documents'] })
      toast.success(`${file.name} uploaded — processing started`)
      setTimeout(() => setUploading(null), 2000)
    } catch {
      toast.error('Upload failed')
      setUploading(null)
    }
  }

  return (
    <div className="p-4 border-t border-white/5">
      {/* Upload progress bar */}
      <AnimatePresence>
        {uploading && (
          <motion.div
            initial={{ opacity: 0, height: 0, marginBottom: 0 }}
            animate={{ opacity: 1, height: 'auto', marginBottom: 8 }}
            exit={{ opacity: 0, height: 0, marginBottom: 0 }}
            className="glass rounded-xl px-3 py-2 flex items-center gap-3"
          >
            <div className="flex-shrink-0">
              {uploading.done
                ? <FileText className="w-4 h-4 text-green-400" />
                : <Loader2 className="w-4 h-4 text-indigo-400 animate-spin" />
              }
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-medium text-[rgb(var(--text-primary))] truncate">{uploading.name}</p>
              <div className="flex items-center gap-2 mt-1">
                <div className="flex-1 h-1 bg-white/10 rounded-full overflow-hidden">
                  <motion.div
                    className={cn('h-full rounded-full', uploading.done ? 'bg-green-400' : 'gradient-bg')}
                    animate={{ width: `${uploading.progress}%` }}
                    transition={{ duration: 0.2 }}
                  />
                </div>
                <span className="text-xs text-[rgb(var(--text-secondary))] flex-shrink-0">
                  {uploading.done ? 'Done' : `${uploading.progress}%`}
                </span>
              </div>
            </div>
            <button
              onClick={() => setUploading(null)}
              className="text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Input area */}
      <div className="relative glass-strong rounded-2xl overflow-hidden gradient-border">
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.doc,.docx,.txt"
          className="hidden"
          onChange={handleFileChange}
        />

        {/* Paperclip button */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={!!uploading || disabled}
          className={cn(
            'absolute left-3 bottom-3 w-8 h-8 rounded-xl flex items-center justify-center transition-all duration-200',
            uploading || disabled
              ? 'text-[rgb(var(--text-secondary))] opacity-30 cursor-not-allowed'
              : 'text-[rgb(var(--text-secondary))] hover:text-indigo-400 hover:bg-indigo-500/10',
          )}
        >
          <Paperclip className="w-4 h-4" />
        </motion.button>

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
            'w-full bg-transparent pl-12 pr-14 py-3 text-sm resize-none outline-none',
            'text-[rgb(var(--text-primary))] placeholder:text-[rgb(var(--text-secondary))]',
            'max-h-40',
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
        Enter to send · Shift+Enter for new line · 📎 to upload PDF/DOC
      </p>
    </div>
  )
}
