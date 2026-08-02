import { useState, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { motion } from 'framer-motion'
import { Brain, Lock, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/Button'
import { PasswordInput } from '@/components/ui/PasswordInput'
import { authApi } from '@/api/auth'

const schema = z.object({
  newPassword: z.string().min(8, 'Minimum 8 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type FormData = z.infer<typeof schema>

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') ?? ''
  const navigate = useNavigate()

  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const inputs = useRef<Array<HTMLInputElement | null>>([])

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  function handleOtpChange(index: number, value: string) {
    if (!/^\d*$/.test(value)) return
    const next = [...otp]
    next[index] = value.slice(-1)
    setOtp(next)
    if (value && index < 5) inputs.current[index + 1]?.focus()
  }

  function handleOtpKeyDown(index: number, e: React.KeyboardEvent) {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputs.current[index - 1]?.focus()
    }
  }

  function handlePaste(e: React.ClipboardEvent) {
    e.preventDefault()
    const digits = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    const next = [...otp]
    digits.split('').forEach((d, i) => { next[i] = d })
    setOtp(next)
    inputs.current[Math.min(digits.length, 5)]?.focus()
  }

  async function onSubmit(data: FormData) {
    const code = otp.join('')
    if (code.length !== 6) {
      toast.error('Enter all 6 digits of the reset code')
      return
    }
    setLoading(true)
    try {
      await authApi.resetPassword(email, code, data.newPassword)
      toast.success('Password reset successfully! Please sign in.')
      navigate('/login')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Invalid or expired code'
      toast.error(msg)
      setOtp(['', '', '', '', '', ''])
      inputs.current[0]?.focus()
    } finally {
      setLoading(false)
    }
  }

  async function handleResend() {
    setResending(true)
    try {
      await authApi.forgotPassword({ email })
      toast.success('New reset code sent')
      setOtp(['', '', '', '', '', ''])
      inputs.current[0]?.focus()
    } catch {
      toast.error('Failed to resend code')
    } finally {
      setResending(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background: 'rgb(var(--bg-primary))' }}>
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/3 right-1/4 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 left-1/4 w-72 h-72 bg-purple-500/8 rounded-full blur-3xl" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="relative w-full max-w-md"
      >
        <div className="glass-strong rounded-2xl p-8 shadow-2xl">
          {/* Header */}
          <div className="flex flex-col items-center gap-3 mb-8">
            <div className="w-14 h-14 rounded-2xl gradient-bg flex items-center justify-center shadow-lg shadow-indigo-500/40">
              <Brain className="w-8 h-8 text-white" />
            </div>
            <div className="text-center">
              <h1 className="text-xl font-bold text-[rgb(var(--text-primary))]">Reset password</h1>
              <p className="text-sm text-[rgb(var(--text-secondary))] mt-1">
                Enter the 6-digit code sent to{' '}
                <span className="text-indigo-400 font-medium">{email}</span>
              </p>
            </div>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
            {/* OTP inputs */}
            <div>
              <p className="text-sm font-medium text-[rgb(var(--text-secondary))] mb-2">Reset code</p>
              <div className="flex gap-3 justify-center" onPaste={handlePaste}>
                {otp.map((digit, i) => (
                  <motion.input
                    key={i}
                    ref={(el) => { inputs.current[i] = el }}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleOtpChange(i, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(i, e)}
                    whileFocus={{ scale: 1.05 }}
                    className="w-11 h-13 text-center text-xl font-bold rounded-xl bg-white/5 border border-white/10 text-[rgb(var(--text-primary))] focus:outline-none focus:border-indigo-500/60 focus:ring-2 focus:ring-indigo-500/20 transition-all duration-150"
                  />
                ))}
              </div>
            </div>

            {/* New password */}
            <PasswordInput
              {...register('newPassword')}
              label="New password"
              placeholder="••••••••"
              icon={<Lock className="w-4 h-4" />}
              error={errors.newPassword?.message}
              autoComplete="new-password"
            />

            {/* Confirm password */}
            <PasswordInput
              {...register('confirmPassword')}
              label="Confirm password"
              placeholder="••••••••"
              icon={<Lock className="w-4 h-4" />}
              error={errors.confirmPassword?.message}
              autoComplete="new-password"
            />

            <Button
              type="submit"
              loading={loading}
              size="lg"
              className="w-full"
              disabled={otp.join('').length !== 6}
            >
              Reset Password
            </Button>
          </form>

          <button
            onClick={handleResend}
            disabled={resending}
            className="w-full flex items-center justify-center gap-2 py-2 mt-3 text-sm text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${resending ? 'animate-spin' : ''}`} />
            {resending ? 'Sending...' : 'Resend code'}
          </button>

          <p className="text-center text-xs text-[rgb(var(--text-secondary))] opacity-50 mt-3">
            Code expires in 5 minutes ·{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300">Back to login</Link>
          </p>
        </div>
      </motion.div>
    </div>
  )
}
