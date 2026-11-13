import api from '../utils/axios'

export type ListingStatus = 'AVAILABLE' | 'RESERVED' | 'PARTIALLY_TAKEN' | 'COMPLETED'
export interface ListingDetails {
  title: string; description: string; sourceLanguage: string; category: string; condition: string
  free: boolean; bundle: boolean; partialPickup: boolean
  price: { amount: string | number; currency: string }
  location: { countryCode: string; city: string; universityId: number | null; meetingPoint: string }
  pickupFrom: string | null; pickupUntil: string | null; tags: string[]; photoIds: string[]
}
export interface Listing {
  id: number; sellerId: number; sellerName: string; universityName: string | null
  details: ListingDetails; status: ListingStatus; hidden: boolean; version: number; createdAt: string
}
export interface MarketFilters {
  country?: string; city?: string; universityId?: number; keyword?: string; category?: string
  currency?: string; tag?: string; free?: boolean; bundle?: boolean; availableOnly: boolean; mine: boolean; page: number
}
export interface ListingTranslation { listingId: number; version: number; language: string; title: string; description: string; machineTranslated: boolean }
export const marketplaceApi = {
  search: (params: MarketFilters) => api.get<{content: Listing[]; hasNext: boolean}>('/marketplace', { params }).then(r => r.data),
  options: () => api.get<{ currencies: {code: string; digits: number}[]; countries: string[] }>('/marketplace/options').then(r => r.data),
  get: (id: string) => api.get<Listing>(`/marketplace/${id}`).then(r => r.data),
  create: (details: ListingDetails) => api.post<Listing>('/marketplace', details).then(r => r.data),
  edit: (listing: Listing, details: ListingDetails) => api.put<Listing>(`/marketplace/${listing.id}`, details, {params: {version: listing.version}}).then(r => r.data),
  status: (listing: Listing, status: ListingStatus) => api.patch<Listing>(`/marketplace/${listing.id}/status`, {version: listing.version, status}).then(r => r.data),
  remove: (listing: Listing) => api.delete(`/marketplace/${listing.id}`, {params: {version: listing.version}}),
  upload: (file: File) => { const data = new FormData(); data.append('file', file); return api.post<{id: string}>('/marketplace/photos', data, {headers: {'Content-Type':'multipart/form-data'}}).then(r => r.data.id) },
  photo: (id: string, signal: AbortSignal) => api.get<Blob>(`/marketplace/photos/${id}`, {responseType: 'blob', signal}).then(r => r.data),
  contact: (id: number) => api.post<{roomId: number}>(`/marketplace/${id}/contact`).then(r => r.data.roomId),
  translate: (id: number, language: string) => api.post<ListingTranslation>(`/marketplace/${id}/translations/${language}`).then(r => r.data),
  report: (id: number, reason: string) => api.post(`/marketplace/${id}/reports`, {reason}),
  blocks: (before?: number) => api.get<{content: {memberId: number; nickname: string}[]; nextBefore: number | null}>('/marketplace/blocks', {params: {before}}).then(r => r.data),
  block: (id: number) => api.put(`/marketplace/blocks/${id}`),
  unblock: (id: number) => api.delete(`/marketplace/blocks/${id}`),
}
export function marketError(error: unknown) {
  const e = error as {response?: {data?: {message?: string}}; message?: string}
  return e.response?.data?.message || e.message || 'Request failed. Please try again.'
}
