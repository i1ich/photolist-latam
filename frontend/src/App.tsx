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
    <div style={{ maxWidth: 480, margin: '0 auto', padding: '1rem' }}>
      <h1>📷 PhotoList LATAM</h1>
      <p>Saca una foto → ve el precio en MercadoLibre</p>

      <PhotoUploader onPhotoSelected={handlePhotoSelected} disabled={loading} />

      {loading && <p>Analizando foto...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {result && <ResultCard result={result} />}
    </div>
  )
}

export default App
