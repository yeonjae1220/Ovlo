'use client'
import { useState } from 'react'
import { useUniversitySearch } from '@/hooks/useUniversity'
import { useDebounce } from '@/hooks/useDebounce'
import type { School } from '@/api/calendar'

export default function SchoolPicker({ value, onChange, ko }: { value: School | null; onChange: (school: School | null) => void; ko: boolean }) {
  const [search, setSearch] = useState('')
  const query = useUniversitySearch(useDebounce(search, 250))
  return <div className="cal-school-picker">
    {value ? <div className="cal-row"><strong>{value.name}</strong><button type="button" onClick={() => { onChange(null); setSearch('') }}>{ko ? '변경' : 'Change'}</button></div>
      : <><label>{ko ? '학교 검색' : 'Find a university'}<input value={search} onChange={e => setSearch(e.target.value)} maxLength={100} placeholder={ko ? '학교 이름을 입력하세요' : 'University name'} /></label>
        {query.isFetching && <p role="status">{ko ? '검색 중…' : 'Searching…'}</p>}
        {query.isError && <p role="alert">{ko ? '검색에 실패했습니다.' : 'Search failed.'} <button type="button" onClick={() => query.refetch()}>{ko ? '재시도' : 'Retry'}</button></p>}
        {search && query.isSuccess && query.data.length === 0 && <p>{ko ? '학교를 찾지 못했습니다. 영문 이름으로도 검색해 보세요.' : 'No university found. Try its English name.'}</p>}
        <ul>{query.data?.map(s => <li key={s.id}><button type="button" onClick={() => onChange(s)}>{s.name} <small>{s.countryCode}</small></button></li>)}</ul></>}
  </div>
}
