import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Home } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center" style={{ background: 'rgb(var(--bg-primary))' }}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center"
      >
        <p className="text-8xl font-bold gradient-text">404</p>
        <p className="text-xl font-semibold text-[rgb(var(--text-primary))] mt-4">Page not found</p>
        <p className="text-sm text-[rgb(var(--text-secondary))] mt-2">The page you're looking for doesn't exist.</p>
        <Link to="/chat" className="mt-6 inline-block">
          <Button>
            <Home className="w-4 h-4" />
            Go Home
          </Button>
        </Link>
      </motion.div>
    </div>
  )
}
