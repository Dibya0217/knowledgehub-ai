import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { Users, FileText, MessageSquare, BarChart2, CheckCircle, XCircle } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Cell } from 'recharts'
import { adminApi } from '@/api/admin'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatDate } from '@/lib/utils'

export function AdminPage() {
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminApi.getStats(),
    select: (r) => r.data.data,
  })

  const { data: usersPage, isLoading: usersLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => adminApi.getUsers(),
    select: (r) => r.data.data,
  })

  const chartData = stats
    ? [
        { name: 'Users', value: stats.totalUsers, fill: '#6366f1' },
        { name: 'Documents', value: stats.totalDocuments, fill: '#8b5cf6' },
        { name: 'Conversations', value: stats.totalConversations, fill: '#a78bfa' },
        { name: 'Messages', value: stats.totalMessages, fill: '#c4b5fd' },
      ]
    : []

  const statCards = [
    { label: 'Total Users', value: stats?.totalUsers, icon: Users, color: 'text-indigo-400', bg: 'bg-indigo-500/10' },
    { label: 'Documents', value: stats?.totalDocuments, icon: FileText, color: 'text-purple-400', bg: 'bg-purple-500/10' },
    { label: 'Conversations', value: stats?.totalConversations, icon: MessageSquare, color: 'text-violet-400', bg: 'bg-violet-500/10' },
    { label: 'Messages', value: stats?.totalMessages, icon: BarChart2, color: 'text-fuchsia-400', bg: 'bg-fuchsia-500/10' },
  ]

  return (
    <div className="h-full overflow-y-auto px-6 py-6">
      <div className="max-w-5xl mx-auto flex flex-col gap-8">
        {/* Stats grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {statCards.map(({ label, value, icon: Icon, color, bg }, i) => (
            <motion.div
              key={label}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.07 }}
              className="glass rounded-2xl p-5 flex flex-col gap-3"
            >
              <div className={`w-10 h-10 rounded-xl ${bg} flex items-center justify-center`}>
                <Icon className={`w-5 h-5 ${color}`} />
              </div>
              {statsLoading ? (
                <Skeleton className="h-8 w-16 rounded-lg" />
              ) : (
                <p className="text-2xl font-bold text-[rgb(var(--text-primary))]">{value ?? 0}</p>
              )}
              <p className="text-xs text-[rgb(var(--text-secondary))]">{label}</p>
            </motion.div>
          ))}
        </div>

        {/* Bar chart */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="glass rounded-2xl p-6"
        >
          <h2 className="font-semibold text-[rgb(var(--text-primary))] mb-6">Platform Overview</h2>
          {statsLoading ? (
            <Skeleton className="h-52 rounded-xl" />
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis
                  dataKey="name"
                  tick={{ fill: 'rgb(var(--text-secondary))', fontSize: 12 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fill: 'rgb(var(--text-secondary))', fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  contentStyle={{
                    background: 'rgba(15,15,30,0.9)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 12,
                    color: 'rgb(var(--text-primary))',
                    fontSize: 12,
                  }}
                  cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                  {chartData.map((entry, index) => (
                    <Cell key={index} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </motion.div>

        {/* Users table */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="glass rounded-2xl p-6"
        >
          <div className="flex items-center justify-between mb-6">
            <h2 className="font-semibold text-[rgb(var(--text-primary))]">Users</h2>
            {usersPage && (
              <span className="text-xs text-[rgb(var(--text-secondary))]">
                {usersPage.totalElements} total
              </span>
            )}
          </div>

          {usersLoading ? (
            <div className="flex flex-col gap-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-12 rounded-xl" />
              ))}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs text-[rgb(var(--text-secondary))] border-b border-white/5">
                    <th className="text-left py-2 pr-4 font-medium">Name</th>
                    <th className="text-left py-2 pr-4 font-medium">Email</th>
                    <th className="text-left py-2 pr-4 font-medium">Provider</th>
                    <th className="text-left py-2 pr-4 font-medium">Verified</th>
                    <th className="text-left py-2 pr-4 font-medium">Roles</th>
                    <th className="text-left py-2 font-medium">Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {(usersPage?.content ?? []).map((user) => (
                    <tr
                      key={user.id}
                      className="border-b border-white/5 hover:bg-white/2 transition-colors"
                    >
                      <td className="py-3 pr-4 font-medium text-[rgb(var(--text-primary))]">{user.name}</td>
                      <td className="py-3 pr-4 text-[rgb(var(--text-secondary))] max-w-[180px] truncate">{user.email}</td>
                      <td className="py-3 pr-4">
                        <span className="px-2 py-0.5 rounded-md text-xs glass text-[rgb(var(--text-secondary))]">
                          {user.provider}
                        </span>
                      </td>
                      <td className="py-3 pr-4">
                        {user.emailVerified
                          ? <CheckCircle className="w-4 h-4 text-green-400" />
                          : <XCircle className="w-4 h-4 text-red-400" />}
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex flex-wrap gap-1">
                          {user.roles.map((r) => (
                            <span key={r} className="px-1.5 py-0.5 rounded text-xs bg-indigo-500/15 text-indigo-300">
                              {r.replace('ROLE_', '')}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="py-3 text-xs text-[rgb(var(--text-secondary))]">
                        {formatDate(user.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {(usersPage?.content ?? []).length === 0 && (
                <p className="text-center py-8 text-sm text-[rgb(var(--text-secondary))] opacity-50">No users found</p>
              )}
            </div>
          )}
        </motion.div>
      </div>
    </div>
  )
}
