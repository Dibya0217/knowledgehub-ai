import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface ThemeState {
  isDark: boolean
  toggle: () => void
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      isDark: true,
      toggle: () => {
        const next = !get().isDark
        set({ isDark: next })
        document.documentElement.classList.toggle('light', !next)
      },
    }),
    { name: 'knowledgehub-theme' },
  ),
)

export function initTheme() {
  const stored = localStorage.getItem('knowledgehub-theme')
  if (stored) {
    const { state } = JSON.parse(stored)
    if (!state.isDark) document.documentElement.classList.add('light')
  }
}
