import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { User, Save } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import api from '@/api/axios'
import type { ApiResponse, UserProfile } from '@/types'

const schema = z.object({
  firstName: z.string().min(1, 'Required'),
  lastName: z.string().min(1, 'Required'),
})

type FormData = z.infer<typeof schema>

export function SettingsPage() {
  const { setUser } = useAuthStore()

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
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (profile) reset({ firstName: profile.firstName, lastName: profile.lastName })
  }, [profile, reset])

  const updateMutation = useMutation({
    mutationFn: (data: FormData) =>
      api.put<ApiResponse<UserProfile>>('/users/me', data).then((r) => r.data),
    onSuccess: (res) => {
      if (res.success) {
        setUser(res.data)
        toast.success('Profile updated')
      }
    },
    onError: () => toast.error('Update failed'),
  })

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
            <div className="grid grid-cols-2 gap-3">
              <Input
                {...register('firstName')}
                label="First name"
                error={errors.firstName?.message}
              />
              <Input
                {...register('lastName')}
                label="Last name"
                error={errors.lastName?.message}
              />
            </div>

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
              {profile.roles.map((role) => (
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
