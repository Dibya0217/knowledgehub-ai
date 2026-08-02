import { useEffect, useState, useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { User, Save, Lock, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PasswordInput } from '@/components/ui/PasswordInput'
import api from '@/api/axios'
import type { ApiResponse, UserProfile } from '@/types'

const profileSchema = z.object({
  name: z.string().min(2, 'Minimum 2 characters'),
})

const passwordSchema = z.object({
  newPassword: z.string().min(8, 'Minimum 8 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type ProfileData = z.infer<typeof profileSchema>
type PasswordData = z.infer<typeof passwordSchema>

export function SettingsPage() {
  const { setUser, user } = useAuthStore()

  // Change-password OTP flow state
  const [pwStep, setPwStep] = useState<'idle' | 'otp'>('idle')
  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [sendingOtp, setSendingOtp] = useState(false)
  const [resetting, setResetting] = useState(false)
  const inputs = useRef<Array<HTMLInputElement | null>>([])

  const { data: profile } = useQuery({
    queryKey: ['me'],
    queryFn: () => authApi.me(),
    select: (r) => r.data,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProfileData>({ resolver: zodResolver(profileSchema) })

  const {
    register: registerPw,
    handleSubmit: handleSubmitPw,
    reset: resetPw,
    formState: { errors: pwErrors },
  } = useForm<PasswordData>({ resolver: zodResolver(passwordSchema) })

  useEffect(() => {
    if (profile) reset({ name: profile.name })
  }, [profile, reset])

  const updateMutation = useMutation({
    mutationFn: (data: ProfileData) =>
      api.put<ApiResponse<UserProfile>>('/users/me', data).then((r) => r.data),
    onSuccess: (res) => {
      if (res.success) {
        setUser(res.data)
        toast.success('Profile updated')
      }
    },
    onError: () => toast.error('Update failed'),
  })

  async function handleSendOtp() {
    const email = profile?.email ?? user?.email
    if (!email) return
    setSendingOtp(true)
    try {
      await authApi.forgotPassword({ email })
      toast.success('Reset code sent to your email')
      setPwStep('otp')
      setOtp(['', '', '', '', '', ''])
    } catch {
      toast.error('Failed to send reset code')
    } finally {
      setSendingOtp(false)
    }
  }

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

  async function onPasswordSubmit(data: PasswordData) {
    const email = profile?.email ?? user?.email
    const code = otp.join('')
    if (code.length !== 6) {
      toast.error('Enter all 6 digits')
      return
    }
    if (!email) return
    setResetting(true)
    try {
      await authApi.resetPassword(email, code, data.newPassword)
      toast.success('Password changed successfully')
      setPwStep('idle')
      resetPw()
      setOtp(['', '', '', '', '', ''])
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Invalid or expired code'
      toast.error(msg)
      setOtp(['', '', '', '', '', ''])
      inputs.current[0]?.focus()
    } finally {
      setResetting(false)
    }
  }

  return (
    <div className="h-full overflow-y-auto px-6 py-6">
      <div className="max-w-xl mx-auto flex flex-col gap-6">
        {/* Profile card */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass rounded-2xl p-6"
        >
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 rounded-xl gradient-bg flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <User className="w-5 h-5 text-white" />
            </div>
            <div>
              <h2 className="font-semibold text-[rgb(var(--text-primary))]">Profile</h2>
              <p className="text-xs text-[rgb(var(--text-secondary))]">Update your personal information</p>
            </div>
          </div>

          <form onSubmit={handleSubmit((d) => updateMutation.mutate(d))} className="flex flex-col gap-4">
            <Input
              {...register('name')}
              label="Full name"
              error={errors.name?.message}
            />
            <Input
              label="Email"
              value={profile?.email ?? ''}
              disabled
              className="opacity-50"
            />
            <Input
              label="Provider"
              value={profile?.provider ?? ''}
              disabled
              className="opacity-50"
            />

            <Button type="submit" loading={updateMutation.isPending} className="w-fit">
              <Save className="w-4 h-4" />
              Save Changes
            </Button>
          </form>
        </motion.div>

        {/* Change password card */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="glass rounded-2xl p-6"
        >
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 rounded-xl gradient-bg flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Lock className="w-5 h-5 text-white" />
            </div>
            <div>
              <h2 className="font-semibold text-[rgb(var(--text-primary))]">Change Password</h2>
              <p className="text-xs text-[rgb(var(--text-secondary))]">We'll send a reset code to your email</p>
            </div>
          </div>

          {pwStep === 'idle' ? (
            <Button onClick={handleSendOtp} loading={sendingOtp} variant="outline" className="w-fit">
              <RefreshCw className="w-4 h-4" />
              Send Reset Code
            </Button>
          ) : (
            <form onSubmit={handleSubmitPw(onPasswordSubmit)} className="flex flex-col gap-5">
              {/* OTP */}
              <div>
                <p className="text-sm font-medium text-[rgb(var(--text-secondary))] mb-2">6-digit code from email</p>
                <div className="flex gap-2" onPaste={handlePaste}>
                  {otp.map((digit, i) => (
                    <input
                      key={i}
                      ref={(el) => { inputs.current[i] = el }}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={digit}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                      className="w-10 h-11 text-center text-lg font-bold rounded-xl bg-white/5 border border-white/10 text-[rgb(var(--text-primary))] focus:outline-none focus:border-indigo-500/60 focus:ring-2 focus:ring-indigo-500/20 transition-all duration-150"
                    />
                  ))}
                </div>
              </div>

              <PasswordInput
                {...registerPw('newPassword')}
                label="New password"
                placeholder="••••••••"
                icon={<Lock className="w-4 h-4" />}
                error={pwErrors.newPassword?.message}
                autoComplete="new-password"
              />
              <PasswordInput
                {...registerPw('confirmPassword')}
                label="Confirm password"
                placeholder="••••••••"
                icon={<Lock className="w-4 h-4" />}
                error={pwErrors.confirmPassword?.message}
                autoComplete="new-password"
              />

              <div className="flex gap-3">
                <Button type="submit" loading={resetting} disabled={otp.join('').length !== 6}>
                  Change Password
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => { setPwStep('idle'); resetPw(); setOtp(['', '', '', '', '', '']) }}
                >
                  Cancel
                </Button>
              </div>

              <button
                type="button"
                onClick={handleSendOtp}
                disabled={sendingOtp}
                className="flex items-center gap-2 text-xs text-[rgb(var(--text-secondary))] hover:text-[rgb(var(--text-primary))] transition-colors w-fit"
              >
                <RefreshCw className={`w-3 h-3 ${sendingOtp ? 'animate-spin' : ''}`} />
                Resend code
              </button>
            </form>
          )}
        </motion.div>

        {/* Roles */}
        {profile?.roles && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="glass rounded-2xl p-6"
          >
            <h2 className="font-semibold text-[rgb(var(--text-primary))] mb-3">Roles</h2>
            <div className="flex flex-wrap gap-2">
              {[...profile.roles].map((role) => (
                <span
                  key={role}
                  className="px-3 py-1 rounded-full text-xs font-medium glass border border-indigo-500/20 text-indigo-300"
                >
                  {role.replace('ROLE_', '')}
                </span>
              ))}
            </div>
          </motion.div>
        )}
      </div>
    </div>
  )
}
