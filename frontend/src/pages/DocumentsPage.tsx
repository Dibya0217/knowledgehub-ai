import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { Upload, FileText, Trash2, CheckCircle, Clock, AlertCircle, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { documentsApi } from '@/api/documents'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { cn } from '@/lib/utils'
import { formatDate } from '@/lib/utils'
import type { DocumentDTO } from '@/types'

const statusConfig = {
  PENDING: { icon: Clock, color: 'text-yellow-400', label: 'Pending' },
  PROCESSING: { icon: Loader2, color: 'text-blue-400', label: 'Processing' },
  READY: { icon: CheckCircle, color: 'text-green-400', label: 'Ready' },
  FAILED: { icon: AlertCircle, color: 'text-red-400', label: 'Failed' },
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function DocumentsPage() {
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [dragOver, setDragOver] = useState(false)
  const qc = useQueryClient()

  const { data: docs, isLoading } = useQuery({
    queryKey: ['documents'],
    queryFn: () => documentsApi.list(),
    select: (r) => r.data.content,
    refetchInterval: (query) => {
      const docs = query.state.data?.data?.content
      return docs?.some((d: DocumentDTO) => d.status === 'PROCESSING' || d.status === 'PENDING')
        ? 3000
        : false
    },
  })

  const deleteMutation = useMutation({
    mutationFn: documentsApi.delete,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['documents'] })
      toast.success('Document deleted')
    },
    onError: () => toast.error('Delete failed'),
  })

  async function handleUpload(file: File) {
    if (!file) return
    setUploading(true)
    setUploadProgress(0)
    try {
      await documentsApi.upload(file, setUploadProgress)
      qc.invalidateQueries({ queryKey: ['documents'] })
      toast.success(`${file.name} uploaded successfully`)
    } catch {
      toast.error('Upload failed')
    } finally {
      setUploading(false)
      setUploadProgress(0)
    }
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (file) handleUpload(file)
  }

  return (
    <div className="h-full overflow-y-auto px-6 py-6">
      <div className="max-w-4xl mx-auto flex flex-col gap-6">
        {/* Upload zone */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          className={cn(
            'relative glass rounded-2xl p-10 border-2 border-dashed transition-all duration-200 text-center cursor-pointer group',
            dragOver ? 'border-indigo-500/60 bg-indigo-500/5' : 'border-white/10 hover:border-indigo-500/30 hover:bg-white/2',
          )}
          onClick={() => document.getElementById('file-input')?.click()}
        >
          <input
            id="file-input"
            type="file"
            accept=".pdf,.doc,.docx,.txt"
            className="hidden"
            onChange={(e) => e.target.files?.[0] && handleUpload(e.target.files[0])}
          />

          <motion.div
            animate={dragOver ? { scale: 1.1 } : { scale: 1 }}
            className="flex flex-col items-center gap-3"
          >
            <div className={cn(
              'w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-200',
              dragOver ? 'gradient-bg shadow-lg shadow-indigo-500/40' : 'glass group-hover:gradient-bg',
            )}>
              <Upload className="w-7 h-7 text-white" />
            </div>
            <div>
              <p className="font-medium text-[rgb(var(--text-primary))]">
                {dragOver ? 'Drop to upload' : 'Drag & drop or click to upload'}
              </p>
              <p className="text-sm text-[rgb(var(--text-secondary))] mt-1">PDF, DOC, DOCX, TXT</p>
            </div>
          </motion.div>

          {uploading && (
            <div className="absolute inset-0 rounded-2xl flex flex-col items-center justify-center bg-black/50 backdrop-blur-sm gap-3">
              <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
              <div className="w-48">
                <div className="flex justify-between text-xs text-[rgb(var(--text-secondary))] mb-1">
                  <span>Uploading...</span>
                  <span>{uploadProgress}%</span>
                </div>
                <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                  <motion.div
                    className="h-full gradient-bg rounded-full"
                    animate={{ width: `${uploadProgress}%` }}
                    transition={{ duration: 0.2 }}
                  />
                </div>
              </div>
            </div>
          )}
        </motion.div>

        {/* Document list */}
        <div className="flex flex-col gap-3">
          {isLoading
            ? Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-20 rounded-xl" />
              ))
            : (
              <AnimatePresence>
                {(docs ?? []).map((doc, i) => {
                  const { icon: StatusIcon, color, label } = statusConfig[doc.status]
                  return (
                    <motion.div
                      key={doc.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ delay: i * 0.05 }}
                      className="glass rounded-xl p-4 flex items-center gap-4 group"
                    >
                      <div className="w-10 h-10 rounded-xl glass flex items-center justify-center flex-shrink-0">
                        <FileText className="w-5 h-5 text-indigo-400" />
                      </div>

                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm text-[rgb(var(--text-primary))] truncate">{doc.originalName}</p>
                        <p className="text-xs text-[rgb(var(--text-secondary))] mt-0.5">
                          {formatBytes(doc.fileSize)} · {formatDate(doc.createdAt)}
                        </p>
                      </div>

                      <div className={cn('flex items-center gap-1.5 text-xs font-medium', color)}>
                        <StatusIcon className={cn('w-3.5 h-3.5', doc.status === 'PROCESSING' && 'animate-spin')} />
                        {label}
                      </div>

                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => deleteMutation.mutate(doc.id)}
                        className="opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </Button>
                    </motion.div>
                  )
                })}
              </AnimatePresence>
            )}

          {!isLoading && (docs ?? []).length === 0 && (
            <div className="text-center py-12 text-[rgb(var(--text-secondary))]">
              <FileText className="w-10 h-10 mx-auto mb-3 opacity-20" />
              <p className="text-sm opacity-50">No documents yet. Upload one to get started.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
