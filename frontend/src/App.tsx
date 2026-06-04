import { useState } from 'react'
import PhotoUploader from './components/PhotoUploader'
import ResultCard from './components/ResultCard'
import { analyzePhoto } from './api/photolist'
import type { AnalyzeResult } from './api/photolist'

function App() {
  const [result, setResult] = useState<AnalyzeResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handlePhotoSelected = async (file: File) => {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const analyzeResult = await analyzePhoto(file)
      setResult(analyzeResult)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f5f5f5', fontFamily: "'Inter', 'Helvetica Neue', Arial, sans-serif" }}>
      {/* Header */}
      <header style={{
        backgroundColor: '#FFE600',
        padding: '0 24px',
        height: 56,
        display: 'flex',
        alignItems: 'center',
        boxShadow: '0 1px 4px rgba(0,0,0,0.12)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 26 }}>📷</span>
          <span style={{ fontWeight: 700, fontSize: 18, color: '#333', letterSpacing: '-0.3px' }}>
            PhotoList <span style={{ color: '#3483fa' }}>LATAM</span>
          </span>
        </div>
        <div style={{ marginLeft: 'auto', fontSize: 13, color: '#555' }}>
          Foto → precio en MercadoLibre
        </div>
      </header>

      {/* Main content */}
      <main style={{ maxWidth: 600, margin: '0 auto', padding: '32px 16px' }}>
        {result ? (
          <button
            onClick={() => { setResult(null); setError(null) }}
            style={{
              width: '100%',
              padding: '12px',
              marginBottom: 16,
              backgroundColor: '#fff',
              border: '1.5px solid #3483fa',
              borderRadius: 10,
              color: '#3483fa',
              fontWeight: 600,
              fontSize: 14,
              cursor: 'pointer',
              fontFamily: 'inherit',
            }}
          >
            ← Analizar otra foto
          </button>
        ) : (
          <PhotoUploader onPhotoSelected={handlePhotoSelected} disabled={loading} />
        )}

        {loading && (
          <div style={{ marginTop: 24, textAlign: 'center', color: '#666', fontSize: 15 }}>
            <div style={{ fontSize: 32, marginBottom: 8 }}>🔍</div>
            Analizando foto...
          </div>
        )}

        {error && (
          <div style={{
            marginTop: 16,
            padding: '12px 16px',
            backgroundColor: '#fff2f2',
            border: '1px solid #ffcdd2',
            borderRadius: 8,
            color: '#c62828',
            fontSize: 14,
          }}>
            {error}
          </div>
        )}

        {result && <ResultCard result={result} />}
      </main>
    </div>
  )
}

export default App
