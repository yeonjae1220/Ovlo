import api from '../utils/axios'

export interface School { id: number; name: string }
export interface EventPeriod {
  allDay: boolean; startDate: string | null; endDate: string | null
  startsAt: string | null; endsAt: string | null; timeZone: string
}
export type EventCategory = 'ADMINISTRATION' | 'ACADEMIC' | 'EVENT' | 'OTHER'
export interface EventDetails {
  title: string; description: string | null; category: EventCategory; period: EventPeriod
  location: string | null; actionUrl: string | null; deadlineAt: string | null; audience: string | null
}
export interface CalendarEvent {
  id: number; kind: 'SCHOOL' | 'PERSONAL'; universityId: number | null; universityName: string | null
  details: EventDetails; origin: 'UNIVERSITY' | 'OVLO_CURATED' | null
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | null
  source: { url: string; checkedOn: string } | null; version: number
}
export interface EventInput extends EventPeriod {
  title: string; description: string; category: EventCategory; location: string; actionUrl: string
  deadlineAt: string | null; audience: string; sourceUrl: string | null; sourceCheckedOn: string | null; version?: number
}
export interface CalendarOptions { role: 'MEMBER' | 'UNIV' | 'ADMIN'; subscriptions: School[]; suggestions: School[]; managedSchools: School[] }
export interface UniversityApplication {
  id: number; memberId: number; applicantName: string; universityId: number; universityName: string
  department: string; contactEmail: string; message: string; status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED'
  reviewNote: string | null; createdAt: string; version: number
}
export interface ApplicationInput { universityId: number; department: string; contactEmail: string; message: string }
export interface SchoolRegistrationInput { universityId: number; department: string; message: string; nickname: string; name: string; email: string; password: string }
export const calendarApi = {
  options: () => api.get<CalendarOptions>('/calendar/options').then(r => r.data),
  list: (from: string, to: string, timeZone: string) => api.get<CalendarEvent[]>('/calendar', { params: { from, to, timeZone } }).then(r => r.data),
  manage: (schoolId: number, from: string, to: string, timeZone: string) => api.get<CalendarEvent[]>(`/calendar/schools/${schoolId}/events`, { params: { from, to, timeZone } }).then(r => r.data),
  subscribe: (schoolId: number) => api.put(`/calendar/subscriptions/${schoolId}`),
  unsubscribe: (schoolId: number) => api.delete(`/calendar/subscriptions/${schoolId}`),
  savePersonal: (input: EventInput, id?: number) => id ? api.put(`/calendar/personal/${id}`, input) : api.post('/calendar/personal', input),
  saveSchool: (schoolId: number, input: EventInput, id?: number) => id ? api.put(`/calendar/school-events/${id}`, input) : api.post(`/calendar/schools/${schoolId}/events`, input),
  remove: (event: CalendarEvent) => api.delete(`/calendar/${event.kind === 'PERSONAL' ? 'personal' : 'school-events'}/${event.id}`, { params: { version: event.version } }),
  status: (event: CalendarEvent, action: 'publish' | 'cancel') => api.post(`/calendar/school-events/${event.id}/status`, { action, version: event.version }),
  applications: () => api.get<UniversityApplication[]>('/calendar/applications').then(r => r.data),
  apply: (input: ApplicationInput) => api.post('/calendar/applications', input),
  registerSchool: (input: SchoolRegistrationInput) => api.post('/members/university-registration', input),
}
export function calendarError(error: unknown) {
  const e = error as { response?: { data?: { message?: string } }; message?: string }
  return e.response?.data?.message || e.message || 'Request failed. Please try again.'
}
