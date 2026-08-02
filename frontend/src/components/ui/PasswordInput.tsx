import { forwardRef, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PasswordInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  icon?: React.ReactNode
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ className, label, error, icon, ...props }, ref) => {
    const [show, setShow] = useState(false)

    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label className="text-sm font-medium text-[rgb(var(--text-secondary))]">{label}</label>
        )}
        <div className="relative">
          {icon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-[rgb(var(--text-secondary))]">
              {icon}
            </div>
          )}
          <input
            ref={ref}
            type={show ? 'text' : 'password'}
            className={cn(
              'w-full rounded-xl px-4 py-2.5 text-sm transition-all duration-200 pr-10',
              'bg-white/5 border border-white/10',
              'text-[rgb(var(--text-primary))] placeholder:text-[rgb(var(--text-secondary))]',
              'focus:outline-none focus:border-indigo-500/60 focus:bg-white/8 focus:ring-2 focus:ring-indigo-500/20',
              icon && 'pl-10',
              error && 'border-red-500/60 focus:border-red-500/80 focus:ring-red-500/20',
              className,
            )}
            {...props}
          />
          <button
            type="button"
            onClick={() => setShow((s) => !s)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors"
          >
            {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        {error && <p className="text-xs text-red-400">{error}</p>}
      </div>
    )
  },
)

PasswordInput.displayName = 'PasswordInput'
