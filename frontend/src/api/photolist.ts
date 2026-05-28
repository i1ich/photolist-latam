const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export interface Listing {
  title: string
  price: number
  currency: string
  condition: string
  url: string
  thumbnail?: string
}

export interface PriceRange {
  min: number
  max: number
  median: number
  currency: string
}

export interface AnalyzeResult {
  itemName: string
  category: string
  site: string
  priceRange: PriceRange
  topListings: Listing[]
}

/**
 * Upload photo and analyze: get item name + MercadoLibre prices.
 */
export async function analyzePhoto(file: File, site = 'MLA'): Promise<AnalyzeResult> {
  // Step 1: get pre-signed upload URL
  const uploadRes = await fetch(`${API_BASE}/upload-url`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contentType: file.type })
  })

  if (!uploadRes.ok) throw new Error('Failed to get upload URL')
  const { uploadUrl, imageKey } = await uploadRes.json()

  // Step 2: upload image directly to S3
  const s3Res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file
  })

  if (!s3Res.ok) throw new Error('Failed to upload image')

  // Step 3: analyze
  const analyzeRes = await fetch(`${API_BASE}/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageKey, site })
  })

  if (!analyzeRes.ok) throw new Error('Failed to analyze image')
  return analyzeRes.json()
}
