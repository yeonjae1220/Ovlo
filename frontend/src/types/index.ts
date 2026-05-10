// ─── Auth ───────────────────────────────────────────────────────────────────
export interface AuthToken {
  accessToken: string
  memberId: number
}

export interface GoogleLoginResult {
  accessToken: string
  memberId: number
  newMember: boolean
}

export interface CompleteOnboardingRequest {
  hometown: string
  homeUniversityId: number
  majorName: string
  degreeType: string
  gradeLevel: number
}

// ─── Member ─────────────────────────────────────────────────────────────────
export type MemberStatus = 'ACTIVE' | 'WITHDRAWN' | 'PENDING_ONBOARDING'
export type MemberRole = 'MEMBER' | 'ADMIN'
export type DegreeType = 'BACHELOR' | 'MASTER' | 'DOCTOR'
export type GradeLevel = 'FRESHMAN' | 'SOPHOMORE' | 'JUNIOR' | 'SENIOR' | 'GRADUATE'

export interface LanguageSkill {
  languageCode: string
  cefrLevel: string
}

export interface UniversityExperience {
  universityId: string
  startDate: string
  endDate?: string
}

export interface ContactInfo {
  type: string
  value: string
}

export interface Member {
  id: string
  email: string
  name: string
  nickname: string
  status: MemberStatus
  role?: MemberRole
  majorName?: string
  degreeType?: DegreeType
  gradeLevel?: GradeLevel
  bio?: string
  profileImageMediaId?: string
  languageSkills: LanguageSkill[]
  universityExperiences: UniversityExperience[]
  contactInfos: ContactInfo[]
  createdAt: string
}

export interface RegisterMemberRequest {
  nickname: string
  name: string
  hometown: string
  email: string
  password: string
  homeUniversityId: number
  majorName: string
  degreeType: string
  gradeLevel: number
}

export interface UpdateMemberProfileRequest {
  nickname?: string
  bio?: string
}

// ─── University ─────────────────────────────────────────────────────────────
export interface GeoLocation {
  latitude: number
  longitude: number
}

export interface University {
  id: string
  name: string
  countryCode: string
  city: string
  geoLocation?: GeoLocation
  websiteUrl?: string
  description?: string
}

// ─── Board ──────────────────────────────────────────────────────────────────
export type BoardCategory =
  | 'GENERAL'
  | 'LANGUAGE_EXCHANGE'
  | 'TRADE'
  | 'CULTURE'
  | 'HOUSING'
  | 'ACADEMIC'
  | 'JOB'

export type LocationScope = 'UNIVERSITY' | 'REGION' | 'COUNTRY' | 'GLOBAL'

export interface Board {
  id: number
  name: string
  description?: string
  category: BoardCategory
  scope: LocationScope
  universityId?: number
  creatorId: number
  active: boolean
}

export interface CreateBoardRequest {
  name: string
  description?: string
  category: BoardCategory
  scope: LocationScope
  universityId?: number
}

// ─── Post ───────────────────────────────────────────────────────────────────
export type ReactionType = 'LIKE' | 'DISLIKE'

export interface Reaction {
  memberId: string
  reactionType: ReactionType
}

export interface Comment {
  id: number
  postId?: number
  authorId: number
  content: string
  deleted: boolean
}

export interface Post {
  id: number
  boardId: number
  boardName?: string
  authorId: number
  title: string
  content: string
  comments: Comment[]
  likeCount: number
  dislikeCount: number
  deleted: boolean
  createdAt?: string
  likedByMe?: boolean
}

export interface CreatePostRequest {
  boardId: number
  title: string
  content: string
}

export interface UpdatePostRequest {
  title?: string
  content?: string
  mediaIds?: string[]
}

export interface CreateCommentRequest {
  content: string
}

export interface ReactToPostRequest {
  reactionType: ReactionType
}

// ─── Follow ─────────────────────────────────────────────────────────────────
export interface Follow {
  id: string
  followerId: string
  followeeId: string
  followedAt: string
}

// ─── Media ──────────────────────────────────────────────────────────────────
export type MediaType =
  | 'IMAGE_JPEG'
  | 'IMAGE_PNG'
  | 'IMAGE_HEIC'
  | 'IMAGE_WEBP'
  | 'VIDEO_MP4'

export type StorageType = 'LOCAL' | 'S3'

export interface MediaFile {
  mediaId: number
  uploaderId: number
  mediaType: MediaType
  storageType: StorageType
  originalFilename: string
  fileSize: number
}

// ─── Chat ───────────────────────────────────────────────────────────────────
export type ChatRoomType = 'DM' | 'GROUP'

export interface Message {
  id: string
  chatRoomId: string
  senderId: string
  content: string
  sentAt: string
}

/** Backend MessageResult — REST history API 및 WebSocket 브로드캐스트 형식 */
export interface HistoryMessage {
  messageId: number
  senderId: number
  content: string
  sentAt: string
}

export interface ChatRoom {
  chatRoomId: number
  type: ChatRoomType
  name?: string
  participantIds: number[]
  participantNicknames: Record<number, string>
  participantProfileImageMediaIds?: Record<number, string>
  unreadCount: number
  participantLastReadAt: Record<number, string> // ISO datetime per memberId
}

export interface CreateChatRoomRequest {
  type: ChatRoomType
  name?: string
  participantIds: number[]
}

// ─── Exchange University ──────────────────────────────────────────────────────
export interface ExchangeUniversity {
  id: number
  nameKo: string
  nameEn: string
  country: string
  city: string
  website?: string
  globalUnivId?: number
  reviewCount: number
  avgRating?: number
}

export interface VideoReview {
  id: number
  youtubeUrl: string
  title: string
  channel: string
  publishedAt: string
  country: string
  city: string
  overallRating?: number
  difficulty?: number
  workload?: number
  recommend?: boolean
  overallTone?: string
  excitementLevel?: number
  costTotal?: string
  costRent?: string
  costFood?: string
  costTransport?: string
  costCurrency?: string
  visaType?: string
  visaCost?: string
  visaDuration?: string
  visaProcessingDays?: string
  dormAvailable?: boolean
  dormType?: string
  dormPrice?: string
  gpaRequirement?: string
  languageReq?: string
  deadlineInfo?: string
  sourceLang?: string
  qualityScore?: number
  summary?: string
  exchangeInfo?: string
  tags?: string[]
  direction?: 'OUTBOUND' | 'INBOUND' | 'UNKNOWN'
}

// ─── Pagination ─────────────────────────────────────────────────────────────
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
}

// ─── API Error ───────────────────────────────────────────────────────────────
export interface ApiError {
  status: number
  code: string
  message: string
}
