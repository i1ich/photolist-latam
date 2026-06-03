import type { AnalyzeResult } from '../api/photolist'

interface Props {
  result: AnalyzeResult
}

const conditionLabel: Record<string, string> = {
  new: 'nuevo',
  used: 'usado',
  not_specified: ''
}

export default function ResultCard({ result }: Props) {
  const { item, market } = result
  const hasPrices = market && market.priceMax > 0

  return (
    <div style={{ marginTop: '1.5rem', border: '1px solid #ddd', borderRadius: 8, padding: '1rem' }}>
      <h2 style={{ marginBottom: 0 }}>{item.name}</h2>
      <p style={{ color: '#666', marginTop: 4 }}>
        {[item.brand, item.category].filter(Boolean).join(' · ')}
      </p>

      {hasPrices ? (
        <div style={{ background: '#fffbe6', padding: '0.75rem', borderRadius: 6, margin: '0.75rem 0' }}>
          <strong>Rango de precios ({market.site})</strong>
          <p>
            {market.currency} {market.priceMin.toLocaleString()} – {market.priceMax.toLocaleString()}
          </p>
          <p>Mediana: {market.currency} {market.priceMedian.toLocaleString()}</p>
        </div>
      ) : (
        <p style={{ color: '#999' }}>No se encontraron publicaciones en MercadoLibre ({market.site}).</p>
      )}

      {market.topListings && market.topListings.length > 0 && (
        <div>
          <h3>Top publicaciones</h3>
          {market.topListings.map((listing, i) => (
            <a
              key={i}
              href={listing.url}
              target="_blank"
              rel="noopener noreferrer"
              style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem', textDecoration: 'none', color: 'inherit' }}
            >
              {listing.thumbnail && (
                <img src={listing.thumbnail} alt={listing.title} style={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 4 }} />
              )}
              <div>
                <p style={{ margin: 0, fontWeight: 500 }}>{listing.title}</p>
                <p style={{ margin: 0, color: '#0075ca' }}>
                  {listing.currency} {listing.price.toLocaleString()}
                  {conditionLabel[listing.condition] ? ` · ${conditionLabel[listing.condition]}` : ''}
                </p>
              </div>
            </a>
          ))}
        </div>
      )}
    </div>
  )
}
