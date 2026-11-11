'use client'
import {useAuthStore} from '../../store/authStore'
import MarketplaceForm from './MarketplaceForm'
export default function MarketplaceNewPage() {
  const user=useAuthStore(state=>state.currentUser?.id)
  return <MarketplaceForm key={user}/>
}
