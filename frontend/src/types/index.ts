// ─── Auth ───────────────────────────────────────────────────────────────────
export interface AuthToken {
  accessToken: string
  refreshToken: string
  memberId: string
}

// ─── Member ─────────────────────────────────────────────────────────────────
export type MemberStatus = 'ACTIVE' | 'WITHDRAWN'
export type DegreeType = 'BACHELOR' | 'MASTER' | 'DOCTOR'
export type GradeLevel = 'FRESHMAN' | 'SOPHOMORE' | 'JUNIOR' | 'SENIOR' | 'GRADUATE'

export interface LanguageSkill {
  language: string
  proficiency: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'NATIVE'
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
  authorId: number
  title: string
  content: string
  comments: Comment[]
  likeCount: number
  dislikeCount: number
  deleted: boolean
}

export interface CreatePostRequest {
  boardId: string
  title: string
  content: string
  mediaIds?: string[]
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
  id: string
  uploaderId: string
  mediaType: MediaType
  originalFilename: string
  fileSize: number
  storagePath: string
  storageType: StorageType
  createdAt: string
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

export interface ChatRoom {
  chatRoomId: number
  type: ChatRoomType
  name?: string
  participantIds: number[]
}

export interface CreateChatRoomRequest {
  type: ChatRoomType
  name?: string
  participantIds: number[]
}

// ─── Pagination ─────────────────────────────────────────────────────────────
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// ─── API Error ───────────────────────────────────────────────────────────────
export interface ApiError {
  status: number
  code: string
  message: string
}
