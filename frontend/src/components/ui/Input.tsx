import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  icon?: React.ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, icon, ...props }, ref) => {
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
            className={cn(
              'w-full rounded-xl px-4 py-2.5 text-sm transition-all duration-200',
              'bg-white/5 border border-white/10',
              'text-[rgb(var(--text-primary))] placeholder:text-[rgb(var(--text-secondary))]',
              'focus:outline-none focus:border-indigo-500/60 focus:bg-white/8 focus:ring-2 focus:ring-indigo-500/20',
              'light:bg-black/5 light:border-black/10',
              icon && 'pl-10',
              error && 'border-red-500/60 focus:border-red-500/80 focus:ring-red-500/20',
              className,
            )}
            {...props}
          />
        </div>
        {error && <p className="text-xs text-red-400">{error}</p>}
      </div>
    )
  },
)

Input.displayName = 'Input'
