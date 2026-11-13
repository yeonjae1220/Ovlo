import { useI18n } from '../../i18n/I18nProvider'

const en = {
  title: 'Campus marketplace', intro: 'A new home for your things. Find everyday essentials nearby.',
  new: 'Sell or give away', mine: 'My listings', all: 'All listings', search: 'Search items', apply: 'Apply filters', reset: 'Reset',
  country: 'Country', city: 'City', school: 'University', schoolSearch: 'Search a university', any: 'All', clear: 'Clear',
  category: 'Category', condition: 'Condition', tag: 'Situation', currency: 'Requested currency',
  currencyHint: 'Choose the currency you want to receive, including your home currency. No automatic conversion.',
  freeOnly: 'Free giveaways', bundlesOnly: 'Bundles', available: 'Available items only', empty: 'No items match these filters.',
  next: 'Next', previous: 'Previous', loading: 'Loading…', retry: 'Try again', back: 'Back to marketplace',
  sale: 'For sale', giveaway: 'Free giveaway', single: 'One item', bundle: 'Several items together',
  photos: 'Photos', photoHint: '1–8 JPEG/PNG photos, up to 8 MB each. The first photo is the cover.',
  addPhotos: 'Add photos', remove: 'Remove', moveLeft: 'Move earlier', moveRight: 'Move later',
  photoError: 'Could not load photo', titleLabel: 'Title', description: 'What is included?',
  descriptionHint: 'List included items, excluded items visible in the photos, and any defects. For partial pickup, keep the remaining items up to date.',
  price: 'Total asking price', partial: 'Allow people to take part of the bundle', allTogether: 'Take the whole bundle',
  pickup: 'Pickup location', pickupHint: 'Use a campus or public place. Arrange the exact meeting point in chat.',
  from: 'Pickup from', until: 'Pickup by', datesHint: 'Optional, inclusive dates in the pickup location.',
  tags: 'Situation tags', language: 'Original post language', save: 'Publish listing', saveEdit: 'Save changes', saving: 'Saving…',
  verifyHint: 'A verified school email is required to upload photos and publish. Browse and contact sellers with an active account.',
  included: 'Items and pickup details', direct: 'Local pickup', free: 'Free', eachBundle: 'for the whole bundle',
  contact: 'Ask in chat', edit: 'Edit listing', status: 'Listing status', delete: 'Delete listing', deleteConfirm: 'Delete this listing? This cannot be undone.',
  translate: 'Translate post', original: 'Show original', translation: 'Machine translation', translationHint: 'Check the original for quantities, exclusions and pickup arrangements. Price and currency stay unchanged.',
  report: 'Report listing', reason: 'Reason for reporting', reported: 'Report submitted', block: 'Block seller', blocked: 'Seller blocked',
  blockHint: 'Their listings will be hidden and new marketplace messages between you will be blocked.', unblock: 'Undo block',
  manageBlocks: 'Blocked members', noBlocks: 'You have not blocked anyone.',
  blocksHint: 'Manage your marketplace blocks. Removing your block does not remove a block made by the other member.',
  draftConflict: 'This listing changed elsewhere. Your draft has been preserved.',
  reloadLatest: 'Load latest details', discardDraft: 'Discard your draft and load the latest listing?',
  hidden: 'This listing has been hidden by a moderator.', completed: 'This listing is completed.',
  remaining: 'Some items have been taken — ask about what remains.', required: 'Complete the required fields and add at least one photo.',
  uploading: 'Uploading photos…', photoLimit: 'Choose up to 8 JPEG/PNG photos, each under 8 MB.',
  error: 'Something went wrong. Please try again.', stale: 'The listing changed. Reload it before trying again.',
}
type Key = keyof typeof en
const ko: Record<Key,string> = {
  title: '학교 근처 중고거래', intro: '떠나는 사람의 물건이, 새로 온 사람의 일상이 되도록.',
  new: '판매·나눔 올리기', mine: '내 판매글', all: '전체 상품', search: '물건 검색', apply: '필터 적용', reset: '초기화',
  country: '국가', city: '도시', school: '학교', schoolSearch: '학교 검색', any: '전체', clear: '선택 해제',
  category: '카테고리', condition: '사용 상태', tag: '상황', currency: '받고 싶은 통화',
  currencyHint: '현지 통화나 귀국 후 사용할 본국 통화를 직접 선택하세요. 자동 환전하지 않습니다.',
  freeOnly: '무료 나눔', bundlesOnly: '묶음만', available: '거래 가능한 상품만', empty: '조건에 맞는 상품이 없습니다.',
  next: '다음', previous: '이전', loading: '불러오는 중…', retry: '다시 시도', back: '중고거래로 돌아가기',
  sale: '판매', giveaway: '무료 나눔', single: '물건 하나', bundle: '여러 물건 묶음',
  photos: '상품 사진', photoHint: 'JPEG·PNG 1~8장, 한 장당 최대 8MB. 첫 번째 사진이 대표 사진입니다.',
  addPhotos: '사진 추가', remove: '제거', moveLeft: '앞으로', moveRight: '뒤로',
  photoError: '사진을 불러오지 못했습니다', titleLabel: '제목', description: '어떤 물건을 가져갈 수 있나요?',
  descriptionHint: '포함 물건, 사진에 있지만 제외되는 물건, 하자를 적어주세요. 부분 수령을 허용하면 남은 물건도 갱신해주세요.',
  price: '전체 판매 가격', partial: '묶음에서 일부만 가져가도 돼요', allTogether: '일괄 인수만 가능',
  pickup: '거래 장소', pickupHint: '학교나 공공장소를 적어주세요. 정확한 약속 장소는 채팅에서 정하세요.',
  from: '수령 가능 시작일', until: '수령 마감일', datesHint: '선택 입력이며, 거래 장소 기준으로 시작일과 마지막 날을 포함합니다.',
  tags: '상황 태그', language: '게시글 원문 언어', save: '판매·나눔 올리기', saveEdit: '수정 저장', saving: '저장 중…',
  verifyHint: '사진 업로드와 상품 등록에는 학교 이메일 인증이 필요합니다. 로그인 회원은 조회와 문의가 가능합니다.',
  included: '물건 구성과 거래 안내', direct: '직접 가져가기', free: '무료 나눔', eachBundle: '묶음 전체 가격',
  contact: '채팅으로 문의', edit: '게시글 수정', status: '거래 상태', delete: '게시글 삭제', deleteConfirm: '판매글을 삭제할까요? 되돌릴 수 없습니다.',
  translate: '게시글 번역', original: '원문 보기', translation: '자동 번역', translationHint: '수량·제외 물건·거래 조건은 원문도 확인하세요. 가격과 통화는 그대로 표시합니다.',
  report: '게시글 신고', reason: '신고 사유', reported: '신고가 접수되었습니다', block: '판매자 차단', blocked: '판매자를 차단했습니다',
  blockHint: '서로의 상품과 새로운 중고거래 채팅 메시지를 차단합니다.', unblock: '차단 취소',
  manageBlocks: '차단한 회원', noBlocks: '차단한 회원이 없습니다.',
  blocksHint: '내가 설정한 중고거래 차단을 관리합니다. 상대방이 설정한 차단은 해제되지 않습니다.',
  draftConflict: '다른 곳에서 게시글이 변경되었습니다. 작성 중인 내용은 유지했습니다.',
  reloadLatest: '최신 내용 불러오기', discardDraft: '작성 중인 내용을 버리고 최신 게시글을 불러올까요?',
  hidden: '운영자가 숨긴 게시글입니다.', completed: '거래가 완료된 상품입니다.',
  remaining: '일부 물건은 거래가 끝났습니다. 남은 물건을 문의하세요.', required: '필수 항목과 사진 1장 이상을 입력해주세요.',
  uploading: '사진 업로드 중…', photoLimit: '사진은 JPEG·PNG 최대 8장, 한 장당 8MB까지 가능합니다.',
  error: '처리하지 못했습니다. 다시 시도해주세요.', stale: '게시글이 변경되었습니다. 새로 불러온 뒤 다시 시도해주세요.',
}
export const categories = ['HOUSEHOLD','FURNITURE','ELECTRONICS','BOOKS','CLOTHING','MIXED','OTHER']
export const conditions = ['NEW','LIKE_NEW','GOOD','WORN']
export const tags = ['RETURN_HOME','MOVING_OUT','ARRIVAL_ESSENTIALS']
const labels: Record<string,[string,string]> = {
  HOUSEHOLD:['생활용품','Household'], FURNITURE:['가구','Furniture'], ELECTRONICS:['전자기기','Electronics'], BOOKS:['교재·책','Books'], CLOTHING:['의류','Clothing'], MIXED:['여러 종류','Mixed items'], OTHER:['기타','Other'],
  NEW:['미사용','Unused'], LIKE_NEW:['거의 새것','Like new'], GOOD:['사용감 적음','Good'], WORN:['사용감 있음','Well used'],
  RETURN_HOME:['귀국 정리','Returning home'], MOVING_OUT:['이사 정리','Moving out'], ARRIVAL_ESSENTIALS:['입국 생활용품','Arrival essentials'],
  AVAILABLE:['판매·나눔 중','Available'], RESERVED:['예약 중','Reserved'], PARTIALLY_TAKEN:['일부 거래 완료','Partly taken'], COMPLETED:['거래 완료','Completed'],
}
export function useMarketText() {
  const {language} = useI18n()
  return {language, text: (key: Key) => (language === 'ko' ? ko : en)[key], label: (key: string) => labels[key]?.[language === 'ko' ? 0 : 1] ?? key}
}
