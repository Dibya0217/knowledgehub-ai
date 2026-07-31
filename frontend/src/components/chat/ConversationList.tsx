import { motion, AnimatePresence } from 'framer-motion'
import { MessageSquare, Plus, Trash2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ConversationSummary } from '@/types'

interface ConversationListProps {
  conversations: ConversationSummary[]
  activeId?: string
  onSelect: (id: string) => void
  onNew: () => void
  onDelete: (id: string) => void
}

export function ConversationList({
  conversations,
  activeId,
  onSelect,
  onNew,
  onDelete,
}: ConversationListProps) {
  return (
    <aside className="w-64 flex-shrink-0 flex flex-col h-full border-r border-white/5 glass">
      <div className="p-3 border-b border-white/5">
        <button
          onClick={onNew}
          className="w-full flex items-center gap-2 px-3 py-2.5 rounded-xl text-sm font-medium gradient-bg text-white shadow-lg shadow-indigo-500/20 hover:shadow-indigo-500/40 transition-shadow"
        >
          <Plus className="w-4 h-4" />
          New Chat
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-1">
        <AnimatePresence>
          {conversations.map((conv) => (
            <motion.div
              key={conv.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              className="group relative"
            >
              <button
                onClick={() => onSelect(conv.id)}
                className={cn(
                  'w-full text-left px-3 py-2.5 rounded-xl text-sm transition-all duration-150 pr-8',
                  activeId === conv.id
                    ? 'bg-indigo-500/15 text-[rgb(var(--text-primary))] border border-indigo-500/20'
                    : 'text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] hover:bg-white/5',
                )}
              >
                <div className="flex items-center gap-2">
                  <MessageSquare className="w-3.5 h-3.5 flex-shrink-0 opacity-60" />
                  <span className="truncate font-medium">{conv.title || 'New conversation'}</span>
                </div>
                <span className="text-xs opacity-40 mt-0.5 block pl-5">
                  {conv.messageCount} messages
                </span>
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete(conv.id)
                }}
                className="absolute right-2 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 w-6 h-6 rounded-lg flex items-center justify-center text-red-400 hover:bg-red-500/10 transition-all"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>

        {conversations.length === 0 && (
          <div className="flex flex-col items-center justify-center flex-1 gap-2 py-8 text-center">
            <MessageSquare className="w-8 h-8 text-[rgb(var(--text-secondary))] opacity-30" />
            <p className="text-xs text-[rgb(var(--text-secondary))] opacity-50">No conversations yet</p>
          </div>
        )}
      </div>
    </aside>
  )
}
