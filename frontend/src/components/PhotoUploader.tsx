import { useRef } from 'react'

interface Props {
  onPhotoSelected: (file: File) => void
  disabled?: boolean
}

export default function PhotoUploader({ onPhotoSelected, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) onPhotoSelected(file)
  }

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
        onChange={handleChange}
        disabled={disabled}
      />
      <button
        onClick={() => inputRef.current?.click()}
        disabled={disabled}
        style={{
          width: '100%',
          padding: '1rem',
          fontSize: '1.2rem',
          backgroundColor: '#FFE600',
          border: 'none',
          borderRadius: 8,
          cursor: disabled ? 'not-allowed' : 'pointer'
        }}
      >
        📷 Tomar / Subir foto
      </button>
    </div>
  )
}
