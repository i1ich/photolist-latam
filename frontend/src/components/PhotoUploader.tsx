import { useRef, useState } from 'react'

interface Props {
  onPhotoSelected: (file: File) => void
  disabled?: boolean
}

export default function PhotoUploader({ onPhotoSelected, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) onPhotoSelected(file)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file && file.type.startsWith('image/')) onPhotoSelected(file)
  }

  return (
    <div
      onClick={() => !disabled && inputRef.current?.click()}
      onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      style={{
        backgroundColor: '#fff',
        border: `2px dashed ${dragOver ? '#3483fa' : '#d4d4d4'}`,
        borderRadius: 12,
        padding: '48px 24px',
        textAlign: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
        transition: 'border-color 0.2s, background 0.2s',
        background: dragOver ? '#f0f6ff' : '#fff',
        opacity: disabled ? 0.6 : 1,
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
        onChange={handleChange}
        disabled={disabled}
      />

      <svg width="52" height="44" viewBox="0 0 52 44" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ marginBottom: 12 }}>
        <rect x="1" y="9" width="50" height="34" rx="5" fill="#e8eef8" stroke="#3483fa" strokeWidth="2"/>
        <circle cx="26" cy="26" r="9" fill="#fff" stroke="#3483fa" strokeWidth="2"/>
        <circle cx="26" cy="26" r="5" fill="#3483fa"/>
        <rect x="18" y="1" width="16" height="10" rx="3" fill="#e8eef8" stroke="#3483fa" strokeWidth="2"/>
        <circle cx="42" cy="16" r="2.5" fill="#3483fa"/>
      </svg>

      <div style={{ fontWeight: 600, fontSize: 17, color: '#333', marginBottom: 6 }}>
        Sacá una foto o subí una imagen
      </div>
      <div style={{ fontSize: 13, color: '#999', marginBottom: 24 }}>
        JPG, PNG, HEIC — el AI identifica el artículo y busca el precio
      </div>

      <button
        disabled={disabled}
        style={{
          backgroundColor: '#3483fa',
          color: '#fff',
          border: 'none',
          borderRadius: 8,
          padding: '12px 32px',
          fontSize: 15,
          fontWeight: 600,
          cursor: disabled ? 'not-allowed' : 'pointer',
          fontFamily: 'inherit',
        }}
        onClick={(e) => { e.stopPropagation(); inputRef.current?.click() }}
      >
        Seleccionar foto
      </button>
    </div>
  )
}
