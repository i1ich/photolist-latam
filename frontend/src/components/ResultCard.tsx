import type { AnalyzeResult } from '../api/photolist'

interface Props {
  result: AnalyzeResult
}

const conditionStyle: Record<string, { text: string; color: string; bg: string }> = {
  new:           { text: 'Nuevo', color: '#1a7f4b', bg: '#e6f7ef' },
  used:          { text: 'Usado', color: '#666',    bg: '#f0f0f0' },
  not_specified: { text: '',      color: '',         bg: '' },
}

function fmt(n: number) {
  return n.toLocaleString('es-AR')
}

export default function ResultCard({ result }: Props) {
  const { item, market } = result
  const hasPrices = market && market.priceMax > 0
  const cond = (c: string) => conditionStyle[c] ?? conditionStyle.not_specified

  return (
    <div style={{ marginTop: 16 }}>

      {/* Item identity card */}
      <div style={{
        backgroundColor: '#fff',
        borderRadius: 12,
        padding: '20px 24px',
        marginBottom: 12,
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
      }}>
        <div style={{ fontSize: 20, fontWeight: 700, color: '#222', marginBottom: 4 }}>
          {item.name}
        </div>
        <div style={{ fontSize: 13, color: '#888', display: 'flex', alignItems: 'center', gap: 8 }}>
          {[item.brand, item.category].filter(Boolean).join(' · ')}
          {item.confidence && (
            <span style={{
              backgroundColor: '#e8f5e9', color: '#2e7d32',
              borderRadius: 4, padding: '1px 7px', fontSize: 11, fontWeight: 600,
            }}>
              {Math.round(item.confidence * 100)}% confianza
            </span>
          )}
        </div>
      </div>

      {/* Price range card or deep-link CTA */}
      {hasPrices ? (
        <div style={{
          backgroundColor: '#fff',
          borderRadius: 12,
          padding: '20px 24px',
          marginBottom: 12,
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        }}>
          <div style={{ fontSize: 11, color: '#aaa', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: 12 }}>
            Precio mediano en {market.site}
          </div>

          <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginBottom: 4 }}>
            <span style={{ fontSize: 14, color: '#555', fontWeight: 500 }}>{market.currency}</span>
            <span style={{ fontSize: 36, fontWeight: 700, color: '#222', lineHeight: 1 }}>
              {fmt(market.priceMedian)}
            </span>
          </div>

          <div style={{ display: 'flex', gap: 24, marginTop: 16, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
            <div>
              <div style={{ fontSize: 11, color: '#bbb', marginBottom: 2 }}>Mínimo</div>
              <div style={{ fontSize: 14, fontWeight: 600, color: '#555' }}>{market.currency} {fmt(market.priceMin)}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: '#bbb', marginBottom: 2 }}>Máximo</div>
              <div style={{ fontSize: 14, fontWeight: 600, color: '#555' }}>{market.currency} {fmt(market.priceMax)}</div>
            </div>
          </div>
        </div>
      ) : market.searchUrl ? (
        <a
          href={market.searchUrl}
          target="_blank"
          rel="noopener noreferrer"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: '#fff200',
            borderRadius: 12,
            padding: '18px 24px',
            marginBottom: 12,
            textDecoration: 'none',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            transition: 'box-shadow 0.15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.boxShadow = '0 4px 14px rgba(0,0,0,0.15)')}
          onMouseLeave={e => (e.currentTarget.style.boxShadow = '0 1px 4px rgba(0,0,0,0.08)')}
        >
          <div>
            <div style={{ fontSize: 15, fontWeight: 700, color: '#222', marginBottom: 2 }}>
              Ver precios en MercadoLibre
            </div>
            <div style={{ fontSize: 12, color: '#555' }}>
              Búsqueda en {market.site} · abre en nueva pestaña
            </div>
          </div>
          <span style={{ fontSize: 24, color: '#222', flexShrink: 0 }}>›</span>
        </a>
      ) : (
        <div style={{
          backgroundColor: '#fff', borderRadius: 12, padding: '20px 24px',
          marginBottom: 12, color: '#999', fontSize: 14, boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        }}>
          No se encontraron publicaciones en MercadoLibre ({market.site}).
        </div>
      )}

      {/* Listings */}
      {market.topListings && market.topListings.length > 0 && (
        <div>
          <div style={{ fontSize: 11, fontWeight: 600, color: '#aaa', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: 8 }}>
            Publicaciones similares
          </div>
          {market.topListings.map((listing, i) => {
            const c = cond(listing.condition)
            return (
              <a
                key={i}
                href={listing.url}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: 'flex', gap: 14, marginBottom: 8,
                  padding: '14px 16px', backgroundColor: '#fff',
                  borderRadius: 10, textDecoration: 'none', color: 'inherit',
                  boxShadow: '0 1px 4px rgba(0,0,0,0.07)', alignItems: 'center',
                  transition: 'box-shadow 0.15s',
                }}
                onMouseEnter={e => (e.currentTarget.style.boxShadow = '0 4px 14px rgba(0,0,0,0.12)')}
                onMouseLeave={e => (e.currentTarget.style.boxShadow = '0 1px 4px rgba(0,0,0,0.07)')}
              >
                <img
                  src={listing.thumbnail || ''}
                  alt={listing.title}
                  style={{ width: 64, height: 64, objectFit: 'contain', borderRadius: 6, flexShrink: 0, background: '#f5f5f5' }}
                  onError={e => {
                    const el = e.currentTarget
                    el.style.display = 'none'
                    const placeholder = el.nextElementSibling as HTMLElement
                    if (placeholder) placeholder.style.display = 'flex'
                  }}
                />
                <div style={{ width: 64, height: 64, borderRadius: 6, backgroundColor: '#f5f5f5', flexShrink: 0, alignItems: 'center', justifyContent: 'center', fontSize: 22, display: 'none' }}>
                  📦
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, color: '#555', marginBottom: 5, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {listing.title}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 18, fontWeight: 700, color: '#222' }}>
                      {listing.currency} {fmt(listing.price)}
                    </span>
                    {c.text && (
                      <span style={{ fontSize: 11, fontWeight: 600, color: c.color, backgroundColor: c.bg, borderRadius: 4, padding: '2px 7px' }}>
                        {c.text}
                      </span>
                    )}
                  </div>
                </div>
                <span style={{ color: '#ccc', fontSize: 20, flexShrink: 0 }}>›</span>
              </a>
            )
          })}
        </div>
      )}
    </div>
  )
}
