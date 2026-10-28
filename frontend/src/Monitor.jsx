import { useState } from 'react'

const SOURCE_NAMES = { gupy: 'Gupy', careers: 'Empresas', linkedin: 'LinkedIn', indeed: 'Indeed' }

export default function Monitor({ apiBaseUrl }) {
  const [token, setToken] = useState('')
  const [query, setQuery] = useState('')
  const [location, setLocation] = useState('')
  const [searches, setSearches] = useState([])
  const [jobs, setJobs] = useState([])
  const [status, setStatus] = useState(null)
  const [unseenOnly, setUnseenOnly] = useState(true)
  const [source, setSource] = useState('')
  const [offset, setOffset] = useState(0)
  const [loaded, setLoaded] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  async function request(path, options = {}) {
    const response = await fetch(`${apiBaseUrl}/monitor${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(token ? { 'X-Monitor-Token': token } : {}) },
    })
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'Informe o token configurado no servidor para acessar o monitor.'
        : 'Não foi possível atualizar o monitor. Confira se o backend está disponível.')
    }
    const text = await response.text()
    return text ? JSON.parse(text) : null
  }

  async function load(nextOffset = 0, selectedUnseen = unseenOnly, selectedSource = source) {
    const params = new URLSearchParams({ unseenOnly: String(selectedUnseen), source: selectedSource,
      limit: '50', offset: String(nextOffset) })
    const [savedSearches, savedJobs, monitorStatus] = await Promise.all([
      request('/searches'), request(`/jobs?${params}`), request('/status'),
    ])
    setSearches(savedSearches)
    setJobs(savedJobs)
    setStatus(monitorStatus)
    setOffset(nextOffset)
    setLoaded(true)
  }

  async function action(callback) {
    setBusy(true)
    setMessage('')
    try { await callback() } catch (error) { setMessage(error.message) } finally { setBusy(false) }
  }

  function saveSearch(event) {
    event.preventDefault()
    action(async () => {
      await request('/searches', { method: 'POST', body: JSON.stringify({ query, location }) })
      setQuery('')
      await load()
      setMessage('Busca salva. O bot vai coletar as vagas na próxima execução automática.')
    })
  }

  return (
    <section className="monitor-section" aria-labelledby="monitor-title">
      <p className="eyebrow">Monitor automático</p>
      <h2 id="monitor-title">Suas vagas guardadas</h2>
      <p className="page-summary">Salve cargos para acompanhar na Gupy e nas páginas de empresas configuradas.
        Alertas de LinkedIn e Indeed entram pela caixa de e-mail configurada no servidor.</p>
      <form className="monitor-form" onSubmit={saveSearch}>
        <label className="search-field">Cargo ou palavra-chave
          <input className="search-input" value={query} maxLength={100} required
            placeholder="Ex.: estágio TI" onChange={event => setQuery(event.target.value)} />
        </label>
        <label className="search-field">Localidade opcional
          <input className="search-input" value={location} maxLength={200}
            placeholder="Ex.: Brasília" onChange={event => setLocation(event.target.value)} />
        </label>
        <button className="search-button" disabled={busy || !query.trim()}>Monitorar cargo</button>
      </form>
      <details>
        <summary>Acesso ao monitor em servidor remoto</summary>
        <label className="search-field">Token do monitor
          <input className="search-input" type="password" autoComplete="off" value={token}
            onChange={event => setToken(event.target.value)} />
        </label>
      </details>
      <div className="monitor-toolbar">
        <button className="search-button" type="button" disabled={busy} onClick={() => action(() => load())}>
          {busy ? 'Atualizando…' : 'Atualizar histórico'}
        </button>
        <label className="search-toggle">
          <input type="checkbox" checked={unseenOnly} disabled={busy} onChange={event => {
            setUnseenOnly(event.target.checked)
            action(() => load(0, event.target.checked))
          }} />Somente não vistas
        </label>
        <label className="search-field">Fonte
          <select className="search-input" value={source} disabled={busy} onChange={event => {
            setSource(event.target.value)
            action(() => load(0, unseenOnly, event.target.value))
          }}>
            <option value="">Todas</option>
            {Object.entries(SOURCE_NAMES).map(([key, name]) => <option key={key} value={key}>{name}</option>)}
          </select>
        </label>
      </div>
      {message && <p role="status">{message}</p>}
      {status && <p className="search-hint">
        {status.enabled ? 'Coleta automática ativada.' : 'Coleta automática desativada no servidor.'}
        {status.running ? ' Coletando agora.' : ''} {status.careerPages} páginas de empresas configuradas.
        {' '}E-mail: {status.emailStatus}.
      </p>}
      {searches.length > 0 && <ul className="monitor-searches">
        {searches.map(search => <li key={search.id}>
          <div><strong>{search.query}</strong>{search.location ? ` · ${search.location}` : ''}
            <p className="search-hint">{search.lastRunAt
              ? `${new Date(search.lastRunAt).toLocaleString('pt-BR')} · ${search.runStatus}`
              : 'Aguardando a primeira coleta'}</p>
          </div>
          <button type="button" disabled={busy} onClick={() => action(async () => {
            await request(`/searches/${search.id}`, { method: 'PATCH', body: JSON.stringify({ value: !search.enabled }) })
            await load()
          })}>{search.enabled ? 'Pausar' : 'Retomar'}</button>
          <button type="button" disabled={busy} onClick={() => action(async () => {
            await request(`/searches/${search.id}`, { method: 'DELETE' })
            await load()
          })}>Remover busca</button>
        </li>)}
      </ul>}
      {!loaded && <p className="search-hint">Abra o histórico para consultar o que o bot já guardou.</p>}
      {loaded && jobs.length === 0 && <p>Nenhuma vaga neste filtro. As vagas aparecem após a coleta.</p>}
      <div className="jobs-list">
        {jobs.map(job => <article className="job-card" key={job.id}>
          <div className="job-main"><h3>{job.name}</h3>
            {job.company && <p className="job-company">{job.company}</p>}
            <span className="job-source">{SOURCE_NAMES[job.source] || job.source}</span>
            {!job.seen && <span className="job-source">Não vista</span>}
          </div>
          <p>{job.city || 'Local não informado'}</p>
          <p>Encontrada em {new Date(job.firstSeenAt).toLocaleString('pt-BR')}</p>
          {job.publishedDate && <p>Publicada em {job.publishedDate}</p>}
          <a className="job-link" href={job.jobUrl} target="_blank" rel="noopener noreferrer">Ver vaga</a>
          <button type="button" disabled={busy} onClick={() => action(async () => {
            await request(`/jobs/${job.id}/seen`, { method: 'PATCH', body: JSON.stringify({ value: !job.seen }) })
            await load(offset)
          })}>{job.seen ? 'Marcar como não vista' : 'Marcar como vista'}</button>
        </article>)}
      </div>
      {loaded && <div className="monitor-toolbar">
        <button type="button" disabled={busy || offset === 0} onClick={() => action(() => load(Math.max(0, offset - 50)))}>
          Anterior
        </button>
        <span>Página {Math.floor(offset / 50) + 1}</span>
        <button type="button" disabled={busy || jobs.length < 50} onClick={() => action(() => load(offset + 50))}>
          Próxima
        </button>
      </div>}
    </section>
  )
}
