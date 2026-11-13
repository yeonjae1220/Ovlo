'use client'
import Link from 'next/link'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {marketplaceApi} from '../../api/marketplace'
import {useAuthStore} from '../../store/authStore'
import {useMarketText} from './marketplaceText'
import {MarketError} from './MarketplaceShared'
import s from './Marketplace.module.css'

export default function MarketplaceBlocksPage() {
  const user=useAuthStore(state => state.currentUser?.id)
  return <BlockList key={user} user={user}/>
}
function BlockList({user}: {user?: string}) {
  const {text}=useMarketText(), cache=useQueryClient()
  const [cursors,setCursors]=useState<(number | undefined)[]>([undefined])
  const before=cursors[cursors.length-1]
  const result=useQuery({queryKey:['market-blocks',user,before],queryFn:()=>marketplaceApi.blocks(before),enabled:!!user})
  const unblock=useMutation({mutationFn:marketplaceApi.unblock,onSuccess:async()=>{
    await Promise.all([
      cache.invalidateQueries({queryKey:['market-blocks',user]}),
      cache.invalidateQueries({queryKey:['market-list',user]}),
      cache.invalidateQueries({queryKey:['market-detail',user]}),
      cache.invalidateQueries({queryKey:['market-translation',user]}),
    ])
  }})
  return <div className={s.page}>
    <Link className={s.button} href="/marketplace">{text('back')}</Link>
    <h1>{text('manageBlocks')}</h1><p className={s.muted}>{text('blocksHint')}</p>
    <MarketError error={unblock.error}/>
    {result.isPending ? <p>{text('loading')}</p> : result.isError ? <><MarketError error={result.error}/><button className={s.button} onClick={()=>void result.refetch()}>{text('retry')}</button></> : <>
      {!result.data.content.length && <p>{text('noBlocks')}</p>}
      {result.data.content.map(member=><div className={`${s.panel} ${s.row}`} key={member.memberId}>
        <span>{member.nickname}</span><button className={s.button} disabled={unblock.isPending} onClick={()=>unblock.mutate(member.memberId)}>{text('unblock')}</button>
      </div>)}
      <div className={s.actions}>
        <button className={s.button} disabled={cursors.length===1 || unblock.isPending} onClick={()=>setCursors(values=>values.slice(0,-1))}>{text('previous')}</button>
        <button className={s.button} disabled={result.data.nextBefore===null || unblock.isPending} onClick={()=>{const next=result.data.nextBefore;if(next!==null)setCursors(values=>[...values,next])}}>{text('next')}</button>
      </div>
    </>}
  </div>
}
