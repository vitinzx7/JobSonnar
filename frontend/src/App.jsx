import './App.css'
import { useState } from 'react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

const PROVIDER_LABELS = {
  gupy: 'Gupy',
  jooble: 'Jooble',
  adzuna: 'Adzuna',
  serper: 'Serper',
}

function isSafeJobUrl(jobUrl) {
  try {
    const url = new URL(jobUrl)
    return url.protocol === 'https:'
  } catch {
    return false
  }
}

function App() {
  const [searchQuery, setSearchQuery] = useState('')
  const [useLocationFilter, setUseLocationFilter] = useState(false)
  const [searchLocation, setSearchLocation] = useState('')
  const [searchRadiusKm, setSearchRadiusKm] = useState('')
  const [selectedProvider, setSelectedProvider] = useState('')
  const [jobs, setJobs] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [hasSearched, setHasSearched] = useState (false)

  async function handleSearch(event) {
    event.preventDefault()

    const query = searchQuery.trim()
    const location = useLocationFilter ? searchLocation.trim() : ''
    const radiusKm = useLocationFilter ? Number.parseInt(searchRadiusKm, 10) : NaN

    if (!query) {
      return
    }
    setHasSearched(true)
    setErrorMessage('')
    setIsLoading(true)

    try {
      const params = new URLSearchParams({
        query,
      })

      if (selectedProvider) {
        params.set('provider', selectedProvider)
      }

      if (useLocationFilter) {
        if (location) {
          params.set('location', location)
        }

        if (!Number.isNaN(radiusKm)) {
          params.set('radiusKm', String(radiusKm))
        }
      }

      const response = await fetch(`${API_BASE_URL}/jobs?${params.toString()}`)

      if (!response.ok) {
        throw new Error(`Failed to fetch jobs: ${response.status}`)
      }

      const jobResults = await response.json()
      setJobs(jobResults)
    } catch(error) {
      console.error('Job search failed', error)
      setJobs([])
      setErrorMessage('Não foi possível buscar vagas. Tente novamente.')
    }
    finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="app-shell">
      <section className="search-section" aria-labelledby="page-title">
        <div className="brand-block">
          <p className="eyebrow">Job search radar</p>
          <h1 id="page-title">JobSonnar</h1>
          <p className="page-summary">
            Busque vagas por cargo e, se quiser, adicione um filtro de localidade.
          </p>
        </div>

        <form className="search-form" aria-label="Busca de vagas" onSubmit={handleSearch}>
          <div className="search-options">
            <label className="search-toggle">
              <input
                type="checkbox"
                checked={useLocationFilter}
                onChange={(event) => setUseLocationFilter(event.target.checked)}
              />
              <span>Filtrar por localidade</span>
            </label>

            <label className="search-field search-provider" htmlFor="job-provider">
              <span className="search-label">Provedor</span>
              <select
                id="job-provider"
                className="search-input"
                name="provider"
                value={selectedProvider}
                onChange={(event) => setSelectedProvider(event.target.value)}
              >
                <option value="">Todos os provedores</option>
                {Object.entries(PROVIDER_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
          </div>

          <div className="search-controls">
            <label className="search-field" htmlFor="job-query">
              <span className="search-label">Cargo</span>
              <input
                id="job-query"
                className="search-input"
                type="search"
                name="query"
                placeholder="Ex: Java developer"
                autoComplete="off"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
              />
            </label>
            {useLocationFilter && (
              <>
                <label className="search-field" htmlFor="job-location">
                  <span className="search-label">Localidade</span>
                  <input
                    id="job-location"
                    className="search-input"
                    type="text"
                    name="location"
                    placeholder="Ex: Brasília/DF"
                    autoComplete="off"
                    value={searchLocation}
                    onChange={(event) => setSearchLocation(event.target.value)}
                  />
                </label>
                <label className="search-field search-field--compact" htmlFor="job-radius">
                  <span className="search-label">Raio</span>
                  <input
                    id="job-radius"
                    className="search-input"
                    type="number"
                    name="radiusKm"
                    min="1"
                    max="50"
                    step="1"
                    placeholder="Ex: 5"
                    value={searchRadiusKm}
                    onChange={(event) => setSearchRadiusKm(event.target.value)}
                  />
                </label>
              </>
            )}
            <button
              className="search-button"
              type="submit"
              disabled={isLoading}
            >
              {isLoading ? 'Buscando...' : 'Buscar vagas'}
            </button>
          </div>
          <p className="search-hint">
            Fontes ativas: Gupy, Jooble, Adzuna e Serper. O filtro geográfico é opcional.
          </p>
        </form>
      </section>

      <section className="results-section" aria-labelledby="results-title">
        <div className="results-header">
          <div>
            <p className="eyebrow">Results</p>
            <h2 id="results-title">Vagas encontradas</h2>
            <p className="results-context">
              {selectedProvider
                ? `Provedor: ${PROVIDER_LABELS[selectedProvider]}`
                : 'Provedor: todos'}
              {useLocationFilter
                ? ` · ${searchLocation || 'Localidade'}${searchRadiusKm ? ` · ${searchRadiusKm} km` : ''}`
                : ''}
            </p>
          </div>
          <span className="results-count">{jobs.length} vagas</span>
        </div>

        {errorMessage && (
          <div className="error-state" role="alert">
            <p>{errorMessage}</p>
          </div>
        )}

        {!errorMessage && jobs.length === 0 && (
          <div className="empty-state">
            <p>
              {hasSearched
              ? 'Nenhuma vaga encontrada.'
              : 'Nenhuma busca realizada.'}
          </p>
          </div>
        )}

        {!errorMessage && jobs.length > 0 && (
          <div className="jobs-list">
            {jobs.map((job) => (
              <article className="job-card" key={job.jobUrl}>
                <div className="job-main">
                  <h3>{job.name}</h3>
                  {job.company && <p className="job-company">{job.company}</p>}
                  {job.source && <span className="job-source">{PROVIDER_LABELS[job.source] || job.source}</span>}
                </div>
                <p>{job.city || 'Local não informado'}</p>
                <p>{job.publishedDate || 'Data não informada'}</p>
                {isSafeJobUrl(job.jobUrl) ? (
                  <a
                    className="job-link"
                    href={job.jobUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Ver vaga
                  </a>
                ) : (
                  <span className="job-link-unavailable">Link indisponível</span>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

export default App
