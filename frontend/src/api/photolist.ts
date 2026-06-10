const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''
const IS_MOCK = import.meta.env.VITE_MOCK === 'true'

export interface Listing {
  title: string
  price: number
  currency: string
  condition: string
  url: string
  thumbnail?: string
}

export interface ItemInfo {
  name: string
  brand?: string
  category?: string
  confidence: number
}

export interface MarketInfo {
  site: string
  currency: string
  priceMin: number
  priceMax: number
  priceMedian: number
  topListings: Listing[]
  searchUrl?: string
}

export interface AnalyzeResult {
  item: ItemInfo
  market: MarketInfo
  analyzedAt?: string
  cachedAt?: string | null
}

const MOCK_RESULTS: AnalyzeResult[] = [
  {
    item: { name: 'iPhone 13 128GB', brand: 'Apple', category: 'Celulares', confidence: 0.94 },
    market: {
      site: 'MLU',
      currency: '',
      priceMin: 0,
      priceMax: 0,
      priceMedian: 0,
      topListings: [],
      searchUrl: 'https://listado.mercadolibre.com.uy/iphone-13-128gb',
    },
    analyzedAt: new Date().toISOString(),
    cachedAt: null
  },
  {
    item: { name: 'Samsung Galaxy S22', brand: 'Samsung', category: 'Celulares', confidence: 0.87 },
    market: {
      site: 'MLU',
      currency: '',
      priceMin: 0,
      priceMax: 0,
      priceMedian: 0,
      topListings: [],
      searchUrl: 'https://listado.mercadolibre.com.uy/samsung-galaxy-s22',
    },
    analyzedAt: new Date().toISOString(),
    cachedAt: new Date(Date.now() - 3600_000).toISOString()
  },
  {
    item: { name: 'Zapatillas Nike Air Max 90', brand: 'Nike', category: 'Ropa y Calzado', confidence: 0.78 },
    market: {
      site: 'MLU',
      currency: '',
      priceMin: 0,
      priceMax: 0,
      priceMedian: 0,
      topListings: [],
      searchUrl: 'https://listado.mercadolibre.com.uy/zapatillas-nike-air-max-90',
    },
    analyzedAt: new Date().toISOString(),
    cachedAt: null
  }
]

let mockIndex = 0

async function analyzePhotoMock(_file: File): Promise<AnalyzeResult> {
  await new Promise(r => setTimeout(r, 1800))
  const result = MOCK_RESULTS[mockIndex % MOCK_RESULTS.length]
  mockIndex++
  return result
}

export async function analyzePhoto(file: File, site?: string): Promise<AnalyzeResult> {
  if (IS_MOCK) return analyzePhotoMock(file)
  const contentType = file.type || 'image/jpeg'

  // Step 1: get pre-signed upload URL
  const uploadRes = await fetch(`${API_BASE}/upload-url`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contentType })
  })

  if (!uploadRes.ok) throw new Error('No se pudo obtener la URL de subida')
  const { uploadUrl, imageKey } = await uploadRes.json()

  // Step 2: upload image directly to S3 (Content-Type must match the signed type)
  const s3Res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file
  })

  if (!s3Res.ok) throw new Error('No se pudo subir la imagen')

  // Step 3: analyze
  const analyzeRes = await fetch(`${API_BASE}/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(site ? { imageKey, site } : { imageKey })
  })

  if (analyzeRes.status === 422) throw new Error('No se pudo identificar el artículo en la foto')
  if (!analyzeRes.ok) throw new Error('No se pudo analizar la imagen')
  return analyzeRes.json()
}
