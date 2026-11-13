'use client'
import Link from 'next/link'
import {useParams, useRouter} from 'next/navigation'
import {useState} from 'react'
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query'
import {marketplaceApi} from '../../api/marketplace'
import type {Listing, ListingStatus} from '../../api/marketplace'
import {useAuthStore} from '../../store/authStore'
import {LANGUAGE_LABELS, SUPPORTED_UI_LANGUAGES} from '../../i18n/messages'
import {useMarketText} from './marketplaceText'
import {Photo, Price, MarketError} from './MarketplaceShared'
import MarketplaceForm from './MarketplaceForm'
import s from './Marketplace.module.css'

export default function MarketplaceDetailPage({editing = false}: {editing?: boolean}) {
  const params = useParams(); const id = String(params?.id || '')
  const user = useAuthStore(state => state.currentUser?.id)
  const {text} = useMarketText()
  const result = useQuery({queryKey:['market-detail',user,id],queryFn:() => marketplaceApi.get(id),enabled:!!id})
  // Keep the editor mounted across new versions, visibility changes and background read failures.
  if(editing && result.data?.sellerId === Number(user)) return <MarketplaceForm key={`${user}:${id}`} listing={result.data} loadError={result.error} onRetry={() => void result.refetch()}/>
  if(result.isPending) return <p>{text('loading')}</p>
  if(result.isError) return <div className={s.page}><Link href="/marketplace">{text('back')}</Link><Link className={s.button} href="/marketplace/blocks">{text('manageBlocks')}</Link><MarketError error={result.error}/><button className={s.button} onClick={() => void result.refetch()}>{text('retry')}</button></div>
  const item = result.data
  if(editing) return <div className={s.page}><Link href={`/marketplace/${id}`}>{text('back')}</Link></div>
  return <Details key={`${user}:${item.id}`} item={item}/>
}
function Details({item}: {item: Listing}) {
  const {text,label,language} = useMarketText(); const router=useRouter(); const cache=useQueryClient()
  const user=useAuthStore(state => state.currentUser?.id)
  const own=Number(user)===item.sellerId
  const [target,setTarget]=useState<string>(language)
  const [translated,setTranslated]=useState(false)
  const [reportOpen,setReportOpen]=useState(false)
  const [reason,setReason]=useState('')
  const [blocked,setBlocked]=useState(false)
  const refresh=async () => {await cache.invalidateQueries({queryKey:['market-detail',user,String(item.id)]});await cache.invalidateQueries({queryKey:['market-list']})}
  const status=useMutation({mutationFn:(next:ListingStatus)=>marketplaceApi.status(item,next),onSuccess:refresh})
  const remove=useMutation({mutationFn:()=>marketplaceApi.remove(item),onSuccess:async()=>{await cache.invalidateQueries({queryKey:['market-list']});router.push('/marketplace')}})
  const contact=useMutation({mutationFn:()=>marketplaceApi.contact(item.id),onSuccess:room=>router.push(`/chat/${room}`)})
  const report=useMutation({mutationFn:()=>marketplaceApi.report(item.id,reason),onSuccess:()=>setReportOpen(false)})
  const block=useMutation({mutationFn:()=>marketplaceApi.block(item.sellerId),onSuccess:async()=>{setBlocked(true);await cache.invalidateQueries({queryKey:['market-list']})}})
  const unblock=useMutation({mutationFn:()=>marketplaceApi.unblock(item.sellerId),onSuccess:async()=>{setBlocked(false);await refresh()}})
  const translation=useQuery({queryKey:['market-translation',user,item.id,item.version,target],queryFn:()=>marketplaceApi.translate(item.id,target),enabled:translated && !blocked && !item.hidden,retry:false,staleTime:Infinity})
  const tx=translated?translation.data:undefined
  if(blocked) return <div className={s.page}><p>{text('blocked')}</p><button className={s.button} disabled={unblock.isPending} onClick={()=>unblock.mutate()}>{text('unblock')}</button><MarketError error={unblock.error}/><Link className={s.button} href="/marketplace">{text('back')}</Link></div>
  const d=item.details
  return <div className={s.page}>
    <Link className={s.button} href="/marketplace">← {text('back')}</Link>
    {item.hidden && <p className={s.notice}>{text('hidden')}</p>}
    {!item.hidden && <div className={s.gallery} aria-label={text('photos')}>{d.photoIds.map((id,i)=><div className={s.slide} key={id}><Photo id={id} alt={`${d.title} (${i+1}/${d.photoIds.length})`}/></div>)}</div>}
    <div className={s.panel}>
      <span className={s.badge}>{label(item.status)}</span><span className={s.badge}>{label(d.category)}</span><span className={s.badge}>{label(d.condition)}</span>
      {d.tags.map(tag=><span className={s.badge} key={tag}>{label(tag)}</span>)}
      <h1>{tx?.title || d.title}</h1><Price details={d}/>{d.bundle && <div className={s.muted}>{text('eachBundle')}</div>}
      <p>{item.sellerName} · {item.universityName || d.location.city}</p>
      <div className={s.row}><span className={s.badge}>{text('direct')}</span>{d.bundle && <span className={s.badge}>{text(d.partialPickup?'partial':'allTogether')}</span>}</div>
      {item.status==='PARTIALLY_TAKEN' && <p className={s.notice}>{text('remaining')}</p>}
      <div className={s.actions}>
        <select aria-label={text('translate')} value={target} onChange={e=>{setTarget(e.target.value);setTranslated(false)}} style={{width:'auto'}}>{SUPPORTED_UI_LANGUAGES.map(l=><option key={l} value={l}>{LANGUAGE_LABELS[l]}</option>)}</select>
        <button className={s.button} disabled={item.hidden || translation.isFetching} onClick={()=>{setTranslated(true);if(translated) void translation.refetch()}}>{translation.isFetching?text('loading'):text('translate')}</button>
        {translated && <button className={s.button} onClick={()=>setTranslated(false)}>{text('original')}</button>}
      </div>
      {translated && <MarketError error={translation.error}/>}{tx?.machineTranslated && <p className={s.notice}>{text('translation')} · {text('translationHint')}</p>}
      <h2 className={s.sectionTitle}>{text('included')}</h2><p className={s.body}>{tx?.description || d.description}</p>
      <hr/><h2 className={s.sectionTitle}>{text('pickup')}</h2><p>{d.location.countryCode} · {d.location.city} · {d.location.meetingPoint}</p>
      {(d.pickupFrom || d.pickupUntil) && <p>{d.pickupFrom && `${text('from')}: ${d.pickupFrom}`} {d.pickupUntil && `${text('until')}: ${d.pickupUntil}`}</p>}
      {own ? <div className={s.actions}>
        {!item.hidden && item.status!=='COMPLETED' && <><Link className={s.button} href={`/marketplace/${item.id}/edit`}>{text('edit')}</Link><label className={s.field}>{text('status')}<select disabled={status.isPending} value={item.status} onChange={e=>status.mutate(e.target.value as ListingStatus)}>{(['AVAILABLE','RESERVED',...(d.partialPickup?['PARTIALLY_TAKEN']:[]),'COMPLETED'] as ListingStatus[]).map(st=><option value={st} key={st}>{label(st)}</option>)}</select></label></>}
        <button className={s.button} disabled={remove.isPending || status.isPending} onClick={()=>{if(window.confirm(text('deleteConfirm'))) remove.mutate()}}>{text('delete')}</button>
      </div> : <div className={s.actions}><button className={s.primary} disabled={item.hidden || item.status==='COMPLETED' || contact.isPending} onClick={()=>contact.mutate()}>{text('contact')}</button><button className={s.button} onClick={()=>setReportOpen(!reportOpen)}>{text('report')}</button><button className={s.button} disabled={block.isPending} onClick={()=>{if(window.confirm(text('blockHint'))) block.mutate()}}>{text('block')}</button></div>}
      {item.status==='COMPLETED' && <p>{text('completed')}</p>}
      {[status.error,remove.error,contact.error,report.error,block.error].map((error,i)=><MarketError key={i} error={error}/>)}
      {report.isSuccess && <p role="status">{text('reported')}</p>}
      {reportOpen && <form onSubmit={e=>{e.preventDefault();report.mutate()}}><label className={s.field}>{text('reason')}<textarea required maxLength={1000} value={reason} onChange={e=>setReason(e.target.value)}/></label><button className={s.button} disabled={report.isPending || !reason.trim()}>{text('report')}</button></form>}
    </div>
  </div>
}
