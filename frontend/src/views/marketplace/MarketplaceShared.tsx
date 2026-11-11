'use client'
import {useEffect, useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {marketplaceApi, marketError} from '../../api/marketplace'
import type {ListingDetails} from '../../api/marketplace'
import {globalUniversityApi} from '../../api/university'
import {useAuthStore} from '../../store/authStore'
import {useMarketText} from './marketplaceText'
import s from './Marketplace.module.css'

export function MarketError({error}: {error: unknown}) { return error ? <div className={s.error} role="alert">{marketError(error)}</div> : null }
export function Photo({id, alt}: {id: string; alt: string}) {
  const {text} = useMarketText()
  const user = useAuthStore(state => state.currentUser?.id)
  const [attempt, retry] = useState(0)
  const [state, setState] = useState<{key: string; url?: string; error?: boolean}>({key: ''})
  const key = `${user}:${id}:${attempt}`
  useEffect(() => {
    const controller = new AbortController()
    let objectUrl: string | undefined
    marketplaceApi.photo(id, controller.signal).then(blob => {
      if (controller.signal.aborted) return
      objectUrl = URL.createObjectURL(blob); setState({key, url: objectUrl})
    }).catch(error => { if (!controller.signal.aborted) { console.error('Marketplace photo failed', error); setState({key, error: true}) } })
    return () => { controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [id, key])
  if (state.key !== key || !state.url) return <div className={s.photoFallback}>{state.key === key && state.error ? <><span>{text('photoError')}</span><button type="button" className={s.button} onClick={e => {e.preventDefault(); retry(n => n + 1)}}>{text('retry')}</button></> : text('loading')}</div>
  // Authenticated object URL: image requests never expose bearer tokens in URLs.
  return <img className={s.photo} src={state.url} alt={alt}/>
}
export function Price({details}: {details: ListingDetails}) {
  const {text, language} = useMarketText()
  return <div className={s.price}>{details.free ? text('free') : new Intl.NumberFormat(language, {style: 'currency', currency: details.price.currency, currencyDisplay: 'code'}).format(Number(details.price.amount))}</div>
}
export function SchoolPicker({value, name, onChange}: {value?: number | null; name?: string | null; onChange: (school: {id: number; name: string; countryCode: string; city: string} | null) => void}) {
  const {text, language} = useMarketText()
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  useEffect(() => {const timer = setTimeout(() => setDebounced(query.trim()), 300); return () => clearTimeout(timer)}, [query])
  const search = useQuery({queryKey: ['market-school-search', debounced, language], queryFn: () => globalUniversityApi.search(debounced, undefined, 0, 15, language), enabled: debounced.length >= 2 && !value})
  return <div className={s.field}><span>{text('school')}</span>{value ? <div className={s.row}><span>{name || `#${value}`}</span><button type="button" className={s.button} onClick={() => {onChange(null);setQuery('')}}>{text('clear')}</button></div> : <>
    <input aria-label={text('schoolSearch')} value={query} maxLength={100} onChange={e => setQuery(e.target.value)} placeholder={text('schoolSearch')}/>
    {search.isFetching && <span>{text('loading')}</span>}<MarketError error={search.error}/>
    <div className={s.results}>{debounced === query.trim() && search.data?.map(u => <button type="button" className={s.button} key={u.id} onClick={() => {onChange({...u, name: u.localName || u.name});setQuery('')}}>{u.localName || u.name} · {u.city} · {u.countryCode}</button>)}</div>
  </>}</div>
}
export function CountryOptions({countries}: {countries: string[]}) {
  const {language} = useMarketText()
  const names = new Intl.DisplayNames([language], {type: 'region'})
  return <>{countries.map(code => ({code, name: names.of(code) || code})).sort((a,b) => a.name.localeCompare(b.name, language)).map(c => <option key={c.code} value={c.code}>{c.name}</option>)}</>
}
