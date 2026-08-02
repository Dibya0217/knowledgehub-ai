import { useState, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Brain, Mail, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/Button'
import { authApi } from '@/api/auth'

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') ?? ''
  const navigate = useNavigate()

  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const inputs = useRef<Array<HTMLInputElement | null>>([])

  function handleChange(index: number, value: string) {
    if (!/^\d*$/.test(value)) return
    const next = [...otp]
    next[index] = value.slice(-1)
    setOtp(next)
    if (value && index < 5) {
      inputs.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index: number, e: React.KeyboardEvent) {
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

  async function handleVerify() {
    const code = otp.join('')
    if (code.length !== 6) {
      toast.error('Enter all 6 digits')
      return
    }
    setLoading(true)
    try {
      await authApi.verifyEmail(email, code)
      toast.success('Email verified! You can now sign in.')
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
      await authApi.sendVerification(email)
      toast.success('New verification code sent')
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
        <div className="absolute top-1/3 left-1/3 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 right-1/3 w-80 h-80 bg-purple-500/8 rounded-full blur-3xl" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="relative w-full max-w-md"
      >
        <div className="glass-strong rounded-2xl p-8 shadow-2xl">
          {/* Header */}
          <div className="flex flex-col items-center gap-4 mb-8">
            <motion.div
              animate={{ scale: [1, 1.05, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
              className="w-16 h-16 rounded-2xl gradient-bg flex items-center justify-center shadow-lg shadow-indigo-500/40"
            >
              <Brain className="w-9 h-9 text-white" />
            </motion.div>
            <div className="text-center">
              <h1 className="text-xl font-bold text-[rgb(var(--text-primary))]">Verify your email</h1>
              <div className="flex items-center justify-center gap-1.5 mt-2">
                <Mail className="w-3.5 h-3.5 text-indigo-400" />
                <p className="text-sm text-[rgb(var(--text-secondary))]">
                  Code sent to <span className="text-indigo-400 font-medium">{email}</span>
                </p>
              </div>
            </div>
          </div>

          {/* OTP inputs */}
          <div className="flex gap-3 justify-center mb-6" onPaste={handlePaste}>
            {otp.map((digit, i) => (
              <motion.input
                key={i}
                ref={(el) => { inputs.current[i] = el }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                whileFocus={{ scale: 1.05 }}
                className="w-12 h-14 text-center text-xl font-bold rounded-xl bg-white/5 border border-white/10 text-[rgb(var(--text-primary))] focus:outline-none focus:border-indigo-500/60 focus:ring-2 focus:ring-indigo-500/20 transition-all duration-150"
              />
            ))}
          </div>

          <Button
            onClick={handleVerify}
            loading={loading}
            size="lg"
            className="w-full mb-4"
            disabled={otp.join('').length !== 6}
          >
            Verify Email
          </Button>

          <button
            onClick={handleResend}
            disabled={resending}
            className="w-full flex items-center justify-center gap-2 py-2 text-sm text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${resending ? 'animate-spin' : ''}`} />
            {resending ? 'Sending...' : 'Resend code'}
          </button>

          <p className="text-center text-xs text-[rgb(var(--text-secondary))] opacity-50 mt-4">
            Code expires in 15 minutes ·{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300">Back to login</Link>
          </p>
        </div>
      </motion.div>
    </div>
  )
}
