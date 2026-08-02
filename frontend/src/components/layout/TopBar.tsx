import { Sun, Moon } from 'lucide-react'
import { motion } from 'framer-motion'
import { useThemeStore } from '@/store/themeStore'

interface TopBarProps {
  title: string
}

export function TopBar({ title }: TopBarProps) {
  const { isDark, toggle } = useThemeStore()

  return (
    <header className="flex items-center justify-between px-6 py-4 border-b border-white/5 glass">
      <h1 className="text-base font-semibold text-[rgb(var(--text-primary))]">{title}</h1>
      <motion.button
        whileTap={{ scale: 0.9 }}
        onClick={toggle}
        className="w-9 h-9 rounded-xl glass flex items-center justify-center text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors"
      >
        <motion.div
          key={isDark ? 'dark' : 'light'}
          initial={{ rotate: -30, opacity: 0 }}
          animate={{ rotate: 0, opacity: 1 }}
          transition={{ duration: 0.2 }}
        >
          {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </motion.div>
      </motion.button>
    </header>
  )
}
