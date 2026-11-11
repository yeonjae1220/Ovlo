'use client'
import Link from 'next/link'
import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {marketplaceApi} from '../../api/marketplace'
import type {MarketFilters} from '../../api/marketplace'
import {useAuthStore} from '../../store/authStore'
import {categories, tags, useMarketText} from './marketplaceText'
import {Photo, Price, MarketError, SchoolPicker, CountryOptions} from './MarketplaceShared'
import s from './Marketplace.module.css'

const initial: MarketFilters = {availableOnly: true, mine: false, page: 0}
export default function MarketplaceListPage() {
  const {text, label, language} = useMarketText()
  const user = useAuthStore(state => state.currentUser?.id)
  const [filters, setFilters] = useState<MarketFilters>(initial)
  const [draft, setDraft] = useState<MarketFilters>(initial)
  const [schoolName, setSchoolName] = useState('')
  const options = useQuery({queryKey: ['market-options', user], queryFn: marketplaceApi.options})
  const result = useQuery({queryKey: ['market-list', user, filters], queryFn: () => marketplaceApi.search(filters)})
  const change = (patch: Partial<MarketFilters>) => setDraft(current => ({...current,...patch}))
  return <div className={s.page}>
    <header className={s.header}><div><h1>{text('title')}</h1><div className={s.muted}>{text('intro')}</div></div><Link className={s.primary} href="/marketplace/new">+ {text('new')}</Link></header>
    <div className={s.actions}>{[false,true].map(mine => <button key={String(mine)} className={`${s.chip} ${filters.mine === mine ? s.selected : ''}`} onClick={() => {setFilters(f => ({...f,mine,page:0}));change({mine})}}>{text(mine ? 'mine' : 'all')}</button>)}</div>
    <form className={s.panel} onSubmit={e => {e.preventDefault();setFilters({...draft,page:0})}}>
      <label className={s.field}>{text('search')}<input value={draft.keyword || ''} maxLength={100} onChange={e => change({keyword: e.target.value})}/></label>
      <details style={{marginTop:16}}><summary style={{cursor:'pointer',padding:'8px 0'}}>{language === 'ko' ? '학교·지역·상세 필터' : 'School, location & more filters'}</summary><div className={s.fields}>
        <SchoolPicker value={draft.universityId} name={schoolName} onChange={u => {change({universityId: u?.id, ...(u ? {country:u.countryCode,city:u.city} : {})});setSchoolName(u?.name || '')}}/>
        <label className={s.field}>{text('country')}<select value={draft.country || ''} onChange={e => {change({country:e.target.value,universityId:undefined,city:''});setSchoolName('')}}><option value="">{text('any')}</option><CountryOptions countries={options.data?.countries || []}/></select></label>
        <label className={s.field}>{text('city')}<input value={draft.city || ''} maxLength={100} onChange={e => change({city:e.target.value,universityId:undefined})}/></label>
        <label className={s.field}>{text('category')}<select value={draft.category || ''} onChange={e => change({category:e.target.value})}><option value="">{text('any')}</option>{categories.map(c => <option key={c} value={c}>{label(c)}</option>)}</select></label>
        <label className={s.field}>{text('currency')}<select value={draft.currency || ''} onChange={e => change({currency:e.target.value})}><option value="">{text('any')}</option>{options.data?.currencies.map(c => <option key={c.code}>{c.code}</option>)}</select></label>
      </div></details>
      <div className={s.actions}>{tags.map(tag => <button type="button" aria-pressed={draft.tag === tag} className={`${s.chip} ${draft.tag === tag ? s.selected : ''}`} key={tag} onClick={() => change({tag: draft.tag === tag ? undefined : tag})}>{label(tag)}</button>)}</div>
      <div className={s.row}>
        <label className={s.check}><input type="checkbox" checked={!!draft.free} onChange={e => change({free: e.target.checked || undefined})}/>{text('freeOnly')}</label>
        <label className={s.check}><input type="checkbox" checked={!!draft.bundle} onChange={e => change({bundle: e.target.checked || undefined})}/>{text('bundlesOnly')}</label>
        <label className={s.check}><input type="checkbox" checked={draft.availableOnly} onChange={e => change({availableOnly:e.target.checked})}/>{text('available')}</label>
      </div>
      <div className={s.actions}><button className={s.primary}>{text('apply')}</button><button className={s.button} type="button" onClick={() => {setDraft(initial);setFilters(initial);setSchoolName('')}}>{text('reset')}</button></div>
      {options.isError && <><MarketError error={options.error}/><button type="button" className={s.button} onClick={() => void options.refetch()}>{text('retry')}</button></>}
    </form>
    {result.isPending ? <p>{text('loading')}</p> : result.isError ? <><MarketError error={result.error}/><button className={s.button} onClick={() => void result.refetch()}>{text('retry')}</button></> : <>
      {!result.data.content.length && <p className={s.panel}>{text('empty')}</p>}
      <div className={s.grid}>{result.data.content.map(item => <Link className={s.card} href={`/marketplace/${item.id}`} key={item.id}>
        <div className={s.cover}>{!item.hidden && <Photo id={item.details.photoIds[0]} alt={item.details.title}/>}</div><div className={s.cardBody}>
          <span className={s.badge}>{label(item.status)}</span>{item.details.bundle && <span className={s.badge}>{text('bundle')}</span>}
          <h2>{item.details.title}</h2><Price details={item.details}/><div className={s.muted}>{item.universityName || item.details.location.city}</div>
          {item.details.tags.map(tag => <span key={tag} className={s.badge}>{label(tag)}</span>)}{item.hidden && <p>{text('hidden')}</p>}
        </div></Link>)}</div>
      <div className={s.actions}><button className={s.button} disabled={filters.page === 0} onClick={() => setFilters(f => ({...f,page:f.page-1}))}>{text('previous')}</button><span>{filters.page + 1}</span><button className={s.button} disabled={!result.data.hasNext} onClick={() => setFilters(f => ({...f,page:f.page+1}))}>{text('next')}</button></div>
    </>}
  </div>
}
