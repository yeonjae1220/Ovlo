'use client'
import Link from 'next/link'
import {useRouter} from 'next/navigation'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {marketplaceApi} from '../../api/marketplace'
import type {Listing, ListingDetails} from '../../api/marketplace'
import {useAuthStore} from '../../store/authStore'
import {LANGUAGE_LABELS, SUPPORTED_UI_LANGUAGES} from '../../i18n/messages'
import {categories, conditions, tags, useMarketText} from './marketplaceText'
import {Photo, MarketError, SchoolPicker, CountryOptions} from './MarketplaceShared'
import s from './Marketplace.module.css'

export default function MarketplaceForm({listing}: {listing?: Listing}) {
  const {text, label, language} = useMarketText()
  const user = useAuthStore(state => state.currentUser?.id)
  const router = useRouter(); const cache = useQueryClient()
  const [form, setForm] = useState<ListingDetails>(() => listing?.details || {
    title:'',description:'',sourceLanguage:language,category:'HOUSEHOLD',condition:'GOOD',free:false,bundle:true,partialPickup:false,
    price:{amount:'',currency:language === 'ko' ? 'KRW' : 'USD'},location:{countryCode:'',city:'',universityId:null,meetingPoint:''},pickupFrom:null,pickupUntil:null,tags:[],photoIds:[],
  })
  const [schoolName, setSchoolName] = useState(listing?.universityName || '')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<unknown>(null)
  const options = useQuery({queryKey:['market-options',user],queryFn:marketplaceApi.options})
  const save = useMutation({mutationFn: () => listing ? marketplaceApi.edit(listing,form) : marketplaceApi.create(form), onSuccess: async saved => {
    await cache.invalidateQueries({queryKey:['market-list']}); await cache.invalidateQueries({queryKey:['market-detail',user,String(saved.id)]});router.push(`/marketplace/${saved.id}`)
  }})
  const change = (patch: Partial<ListingDetails>) => setForm(current => ({...current,...patch}))
  const upload = async (files: FileList | null) => {
    if (!files?.length) return
    setUploadError(null)
    const selected = Array.from(files)
    if (selected.length + form.photoIds.length > 8 || selected.some(f => f.size > 8*1024*1024 || !['image/jpeg','image/png'].includes(f.type))) {setUploadError(new Error(text('photoLimit')));return}
    setUploading(true)
    try { for(const file of selected) {const id = await marketplaceApi.upload(file);setForm(current => ({...current,photoIds:[...current.photoIds,id]}))} }
    catch(error) {console.error('Marketplace photo upload failed',error);setUploadError(error)} finally {setUploading(false)}
  }
  const move = (index: number, delta: number) => { const photos = [...form.photoIds];[photos[index],photos[index+delta]] = [photos[index+delta],photos[index]];change({photoIds:photos}) }
  const valid = form.title.trim() && form.description.trim() && form.location.countryCode && form.location.city.trim() && form.location.meetingPoint.trim() && form.photoIds.length > 0 && (form.free || Number(form.price.amount) > 0)
  const digits = options.data?.currencies.find(c => c.code === form.price.currency)?.digits ?? 2
  const currencyNames = new Intl.DisplayNames([language],{type:'currency'})
  return <div className={s.page}>
    <Link className={s.button} href={listing ? `/marketplace/${listing.id}` : '/marketplace'}>← {text('back')}</Link>
    <header className={s.header}><h1>{text(listing ? 'edit' : 'new')}</h1></header><p className={s.muted}>{text('verifyHint')}</p>
    <form onSubmit={e => {e.preventDefault();if(valid && !uploading) save.mutate()}}>
      <section className={s.panel}><h2 className={s.sectionTitle}>{text('photos')} · {form.photoIds.length}/8</h2><p className={s.muted}>{text('photoHint')}</p>
        <div className={s.thumbs}>{form.photoIds.map((id,i) => <div key={id}><div className={s.thumb}><Photo id={id} alt={`${text('photos')} ${i+1}`}/></div><div className={s.photoActions}>
          <button type="button" className={s.button} disabled={i===0 || uploading} aria-label={text('moveLeft')} onClick={() => move(i,-1)}>←</button>
          <button type="button" className={s.button} disabled={i===form.photoIds.length-1 || uploading} aria-label={text('moveRight')} onClick={() => move(i,1)}>→</button>
          <button type="button" className={s.button} disabled={uploading} onClick={() => change({photoIds:form.photoIds.filter(p => p !== id)})}>{text('remove')}</button>
        </div></div>)}</div>
        <label className={s.field}>{text('addPhotos')}<input className={s.file} type="file" accept="image/jpeg,image/png" multiple disabled={uploading || save.isPending || form.photoIds.length>=8} onChange={e => {void upload(e.target.files);e.target.value=''}}/></label>
        {uploading && <p role="status">{text('uploading')}</p>}<MarketError error={uploadError}/>
      </section>
      <section className={s.panel}><div className={s.fields}>
        <label className={s.field}>{text('sale')} / {text('giveaway')}<select value={String(form.free)} onChange={e => {const free=e.target.value==='true';change({free,price:{...form.price,amount:free?'0':''}})}}><option value="false">{text('sale')}</option><option value="true">{text('giveaway')}</option></select></label>
        <label className={s.field}>{text('single')} / {text('bundle')}<select value={String(form.bundle)} onChange={e => {const bundle=e.target.value==='true';change({bundle,partialPickup:bundle&&form.partialPickup})}}><option value="false">{text('single')}</option><option value="true">{text('bundle')}</option></select></label>
        {form.bundle && <label className={`${s.check} ${s.full}`}><input type="checkbox" checked={form.partialPickup} onChange={e => change({partialPickup:e.target.checked})}/>{text('partial')}</label>}
        <label className={`${s.field} ${s.full}`}>{text('titleLabel')} *<input required maxLength={120} value={form.title} onChange={e => change({title:e.target.value})}/></label>
        <label className={`${s.field} ${s.full}`}>{text('description')} *<textarea required maxLength={5000} rows={7} value={form.description} onChange={e => change({description:e.target.value})}/><span className={s.muted}>{text('descriptionHint')}</span></label>
        <label className={s.field}>{text('category')}<select value={form.category} onChange={e => change({category:e.target.value})}>{categories.map(c => <option key={c} value={c}>{label(c)}</option>)}</select></label>
        <label className={s.field}>{text('condition')}<select value={form.condition} onChange={e => change({condition:e.target.value})}>{conditions.map(c => <option key={c} value={c}>{label(c)}</option>)}</select></label>
        <label className={s.field}>{text('price')}<input type="number" required={!form.free} disabled={form.free} min={form.free ? 0 : 10**-digits} max="999999999999" step={10**-digits} value={form.price.amount} onChange={e => change({price:{...form.price,amount:e.target.value}})}/></label>
        <label className={s.field}>{text('currency')}<select value={form.price.currency} onChange={e => change({price:{...form.price,currency:e.target.value}})}>{options.data ? options.data.currencies.map(c => <option key={c.code} value={c.code}>{c.code} · {currencyNames.of(c.code)}</option>) : <option>{form.price.currency}</option>}</select></label>
        <p className={`${s.muted} ${s.full}`}>{text('currencyHint')}</p>
        <label className={s.field}>{text('language')}<select value={form.sourceLanguage} onChange={e => change({sourceLanguage:e.target.value})}>{SUPPORTED_UI_LANGUAGES.map(l => <option key={l} value={l}>{LANGUAGE_LABELS[l]}</option>)}</select></label>
      </div><h2 className={s.sectionTitle}>{text('tags')}</h2><div className={s.row}>{tags.map(tag => <button key={tag} type="button" aria-pressed={form.tags.includes(tag)} className={`${s.chip} ${form.tags.includes(tag) ? s.selected : ''}`} onClick={() => change({tags:form.tags.includes(tag)?form.tags.filter(t=>t!==tag):[...form.tags,tag]})}>{label(tag)}</button>)}</div></section>
      <section className={s.panel}><h2 className={s.sectionTitle}>{text('pickup')}</h2><div className={s.fields}>
        <SchoolPicker value={form.location.universityId} name={schoolName} onChange={u => {change({location:{...form.location,universityId:u?.id || null,...(u ? {countryCode:u.countryCode,city:u.city || form.location.city} : {})}});setSchoolName(u?.name || '')}}/>
        <label className={s.field}>{text('country')} *<select required value={form.location.countryCode} onChange={e => {change({location:{...form.location,countryCode:e.target.value,universityId:null}});setSchoolName('')}}><option value="">—</option><CountryOptions countries={options.data?.countries || []}/></select></label>
        <label className={s.field}>{text('city')} *<input required maxLength={100} value={form.location.city} onChange={e => change({location:{...form.location,city:e.target.value}})}/></label>
        <label className={s.field}>{text('pickup')} *<input required maxLength={200} value={form.location.meetingPoint} onChange={e => change({location:{...form.location,meetingPoint:e.target.value}})}/></label>
        <p className={`${s.muted} ${s.full}`}>{text('pickupHint')}</p>
        <label className={s.field}>{text('from')}<input type="date" value={form.pickupFrom || ''} max={form.pickupUntil || undefined} onChange={e => change({pickupFrom:e.target.value || null})}/></label>
        <label className={s.field}>{text('until')}<input type="date" value={form.pickupUntil || ''} min={form.pickupFrom || undefined} onChange={e => change({pickupUntil:e.target.value || null})}/></label>
        <p className={`${s.muted} ${s.full}`}>{text('datesHint')}</p>
      </div></section>
      <MarketError error={options.error}/>{options.isError && <button type="button" className={s.button} onClick={() => void options.refetch()}>{text('retry')}</button>}
      <MarketError error={save.error}/>{!valid && <p className={s.muted}>{text('required')}</p>}
      <div className={s.actions}><button className={s.primary} disabled={!valid || uploading || save.isPending || !options.data}>{text(save.isPending ? 'saving' : listing ? 'saveEdit' : 'save')}</button></div>
    </form>
  </div>
}
