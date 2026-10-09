import { useEffect, useState } from 'react'
import { getHealth, type HealthResponse } from './api/healthApi'

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .catch((err: Error) => setError(err.message))
  }, [])

  return (
    <main>
      <h1>EduTrack</h1>

      <h2>Backend Status</h2>

      {health && (
        <p>
          {health.application}: {health.status}
        </p>
      )}

      {error && (
        <p>
          Error: {error}
        </p>
      )}

      {!health && !error && <p>Checking backend...</p>}
    </main>
  )
}

export default App