import { createRoot } from 'react-dom/client'
import { Toaster } from 'sonner'
import './index.css'
import App from './App'
import { initTheme } from './store/themeStore'

initTheme()

createRoot(document.getElementById('root')!).render(
  <>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        style: {
          background: 'rgb(28 28 45)',
          border: '1px solid rgb(255 255 255 / 0.08)',
          color: 'white',
        },
      }}
    />
  </>,
)
