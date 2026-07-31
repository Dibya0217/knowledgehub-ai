import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  MessageSquare,
  FileText,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Brain,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'
import { toast } from 'sonner'

const navItems = [
  { to: '/chat', icon: MessageSquare, label: 'Chat' },
  { to: '/documents', icon: FileText, label: 'Documents' },
  { to: '/settings', icon: Settings, label: 'Settings' },
]

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const { refreshToken, logout, user } = useAuthStore()
  const navigate = useNavigate()

  async function handleLogout() {
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch {
      // ignore
    }
    logout()
    navigate('/login')
    toast.success('Logged out successfully')
  }

  return (
    <motion.aside
      animate={{ width: collapsed ? 72 : 240 }}
      transition={{ duration: 0.25, ease: 'easeInOut' }}
      className="relative flex flex-col h-full glass border-r border-white/5 overflow-hidden"
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/5">
        <div className="flex-shrink-0 w-9 h-9 rounded-xl gradient-bg flex items-center justify-center shadow-lg shadow-indigo-500/30">
          <Brain className="w-5 h-5 text-white" />
        </div>
        <AnimatePresence>
          {!collapsed && (
            <motion.span
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              transition={{ duration: 0.15 }}
              className="font-semibold text-sm gradient-text whitespace-nowrap"
            >
              KnowledgeHub AI
            </motion.span>
          )}
        </AnimatePresence>
      </div>

      {/* Nav */}
      <nav className="flex-1 flex flex-col gap-1 p-3">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group',
                isActive
                  ? 'gradient-bg text-white shadow-lg shadow-indigo-500/20'
                  : 'text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] hover:bg-white/5',
              )
            }
          >
            <Icon className="w-5 h-5 flex-shrink-0" />
            <AnimatePresence>
              {!collapsed && (
                <motion.span
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -8 }}
                  transition={{ duration: 0.15 }}
                  className="whitespace-nowrap"
                >
                  {label}
                </motion.span>
              )}
            </AnimatePresence>
          </NavLink>
        ))}
      </nav>

      {/* User + Logout */}
      <div className="p-3 border-t border-white/5 space-y-1">
        {user && (
          <div className={cn('flex items-center gap-3 px-3 py-2', collapsed && 'justify-center')}>
            <div className="flex-shrink-0 w-8 h-8 rounded-full gradient-bg flex items-center justify-center text-white text-xs font-bold">
              {user.firstName?.[0]?.toUpperCase() ?? 'U'}
            </div>
            <AnimatePresence>
              {!collapsed && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex-1 min-w-0"
                >
                  <p className="text-xs font-medium text-[rgb(var(--text-primary))] truncate">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-xs text-[rgb(var(--text-secondary))] truncate">{user.email}</p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        <button
          onClick={handleLogout}
          className={cn(
            'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-red-400 hover:bg-red-500/10 transition-all duration-200',
            collapsed && 'justify-center',
          )}
        >
          <LogOut className="w-5 h-5 flex-shrink-0" />
          <AnimatePresence>
            {!collapsed && (
              <motion.span
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                Logout
              </motion.span>
            )}
          </AnimatePresence>
        </button>
      </div>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute top-5 -right-3 w-6 h-6 rounded-full glass-strong border border-white/10 flex items-center justify-center text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors z-10"
      >
        {collapsed ? <ChevronRight className="w-3 h-3" /> : <ChevronLeft className="w-3 h-3" />}
      </button>
    </motion.aside>
  )
}
