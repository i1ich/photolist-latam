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
      site: 'MLA',
      currency: 'ARS',
      priceMin: 450000,
      priceMax: 620000,
      priceMedian: 530000,
      topListings: [
        { title: 'Apple iPhone 13 128 GB Negro', price: 529999, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA1234', thumbnail: 'https://http2.mlstatic.com/D_NQ_NP_placeholder.jpg' },
        { title: 'iPhone 13 128gb Liberado', price: 489000, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA5678' },
        { title: 'iPhone 13 Como Nuevo 128gb', price: 615000, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA9012' },
      ]
    },
    analyzedAt: new Date().toISOString(),
    cachedAt: null
  },
  {
    item: { name: 'Samsung Galaxy S22', brand: 'Samsung', category: 'Celulares', confidence: 0.87 },
    market: {
      site: 'MLA',
      currency: 'ARS',
      priceMin: 280000,
      priceMax: 390000,
      priceMedian: 330000,
      topListings: [
        { title: 'Samsung Galaxy S22 128gb', price: 329999, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA2222' },
        { title: 'Samsung S22 Liberado', price: 289000, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA3333' },
      ]
    },
    analyzedAt: new Date().toISOString(),
    cachedAt: new Date(Date.now() - 3600_000).toISOString()
  },
  {
    item: { name: 'Zapatillas Nike Air Max 90', brand: 'Nike', category: 'Ropa y Calzado', confidence: 0.78 },
    market: {
      site: 'MLA',
      currency: 'ARS',
      priceMin: 85000,
      priceMax: 140000,
      priceMedian: 108000,
      topListings: [
        { title: 'Nike Air Max 90 Talle 42', price: 110000, currency: 'ARS', condition: 'new', url: 'https://www.mercadolibre.com.ar/p/MLA4444' },
        { title: 'Air Max 90 Original', price: 89000, currency: 'ARS', condition: 'used', url: 'https://www.mercadolibre.com.ar/p/MLA5555' },
      ]
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

export async function analyzePhoto(file: File, site = 'MLA'): Promise<AnalyzeResult> {
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
    body: JSON.stringify({ imageKey, site })
  })

  if (analyzeRes.status === 422) throw new Error('No se pudo identificar el artículo en la foto')
  if (!analyzeRes.ok) throw new Error('No se pudo analizar la imagen')
  return analyzeRes.json()
}
