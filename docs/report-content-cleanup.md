# 대학 리포트 현지 통화 정리

승인 정책(2026-09-27): 현지 통화로 이미 적힌 금액을 보존하고 환산 가격을 제거한다.
`€600 (≈60,000 won)`은 `€600`으로, 미국 대학의 `60,000 won (~$50 USD)`는
`$50 USD`로 만든다. 어느 금액이 실제로 맞는지 추측하거나 새 환율로 계산하지 않는다.
한국 대학의 원화 가격은 현지 가격이므로 유지한다.

비자 카드·섹션과 신청 절차, 서류, 기간, 취업 허용 정보는 보존한다. 비자 비용에도 같은
정리 규칙을 적용한다. 한국 외 D-2 표기는 별도 사실 검증 목록에만 기록하며 수정하지 않는다.
이 문서는 비자 전체 삭제를 제안했던 이전 초안을 대체한다. `--visa-policy` 모드는 없다.

## 범위와 보수적 예외

- `summary`, `body`, `content`의 문자열을 정리하고 `title` 불변을 검사한다.
- 현지 금액과 명확히 연결된 외화 환산(괄호 또는 환산 연결어)을 제거한다.
- 해외 대학의 원화 가격만 있는 괄호·구조화 비용 필드·순수 비용 문장을 제거한다.
  좁게 식별 가능한 비용절은 제거하되, 설명과 섞여 구분이 어려운 문장은 보존한다.
- 국가·통화가 불분명하거나 실제 외화 청구액일 수 있으면 자동으로 현지 통화로 바꾸지 않는다.
  잔존 원화와 외화 통화 라벨을 `exceptions.jsonl`로 출력한다. 이는 발견 항목 수이며
  중복을 제거한 대학 수나 확정 오류 수가 아니다.
- 국가 통화/언어별 별칭은 버전 관리되는 `report_currency_data.json`을 사용한다.
  외부 환율 API나 LLM을 호출하지 않는다. 표에 없는 국가는 그대로 두고 예외로 출력한다.
- 프론트엔드의 자동 원화 환산도 제거한다. 두 상세 화면은 저장된 가격을 그대로 표시하고,
  월 비용과 임대료가 모두 없으면 빈 비용 카드를 숨긴다.

## 재현

최신 운영 스냅샷을 읽기 전용으로 내보낸다. 생성 SQL과 원문은 커밋하지 않는다.

```sql
SELECT row_to_json(x) FROM (
  SELECT t.*, g.country_code, g.name_en
  FROM university_report_translation t
  JOIN university_report r ON r.id=t.report_id
  LEFT JOIN global_universities g ON g.id=r.global_univ_id
  ORDER BY t.report_id,t.lang
) x;
```

```bash
python3 -B -m unittest discover -s scripts -p test_clean_report_currency.py
python3 -B scripts/clean_report_currency.py \
  output/currency-cleanup/before.jsonl output/currency-cleanup/proposal \
  --run-id r20260927_local
```

`after.jsonl`은 전량 결과, `changes.jsonl`은 변경 필드의 전체 원문/결과,
`exceptions.jsonl`은 짧은 예외 문맥, `visa-audit.jsonl`은 D-2 별도 점검 목록이다.
전체 콘텐츠를 모델에 입력할 필요 없이 코드로 전량 검사하고 예외 표본만 읽을 수 있다.
생성 단계에서 재실행 멱등성, 제목, 비자 유형/기간, 근로 허용 필드 보존을 검사한다.

## 운영 반영과 검증

1. 변경량이 큰 행, 각 언어의 카드·본문, 예외를 검토한다.
2. `psql -X -v ON_ERROR_STOP=1`로 `review.sql`을 실행한다. 실제 DB 트리거를 거치되
   마지막에 ROLLBACK한다. 백업 테이블이 남지 않고 원문이 유지되는지 확인한다.
3. `apply.sql`을 실행한다. 스냅샷의 네 필드가 현재 값과 다르면 전체 중단한다.
   잠금 대기는 5초, 문장 실행은 60초로 제한한다. 정확한 before/after 값을 넣는 방식이며
   DB에서 자유형 본문을 정규식으로 치환하지 않는다.
4. `report_currency_backup_<run-id>`에 전체 원래 행을 보존하고, 갱신 및 after 행 저장을
   같은 트랜잭션으로 커밋한다. 기존 백업이 있으면 실패하므로 덮어쓰지 않는다.
5. 새로 내보낸 DB의 모든 행/네 필드를 `after.jsonl`과 비교한다. 변경 건수와 백업 건수를
   대조하고, TUM(70)의 7개 언어에서 비자 비용 `€600` 및 비자 안내 보존을 확인한다.
6. `/api/v1/university-reports/70?lang=en` 등 공개 API와 리포트/교환대학 상세 화면을 확인한다.
   코드 PR의 CI 성공 후 merge commit으로 병합하고, 운영 이미지 SHA와 Ready 상태를 확인한다.

데이터 정리는 DB에 즉시 반영된다. 프론트 환산 제거는 이미지 배포가 필요하다.
이미 열린 화면은 새로고침해야 한다.

## 복구

`rollback.sql`은 현재 행 전체가 저장된 after 행과 같은지 검사한다. 적용 이후 추가 수정이
발견되면 전체 중단하여 후속 작업을 덮어쓰지 않는다. 통과하면 원래 네 필드와 `updated_at`을
복원한다. 백업 테이블과 원문 스냅샷은 검증 완료 후에도 보관한다.

## 생성 경로의 한계

현재 생성 파이프라인은 이 저장소 밖에서 DB에 적재한다. 이 변경은 기존 데이터 정리와
화면 환산 제거이며, 미래 생성의 차단 게이트가 아니다. 생성 프롬프트에서 현지 통화만
요청하고 적재 전에 이 정리기의 `clean_row` 및 예외 검사를 연결하는 후속 변경이 필요하다.
비자 유형·금액의 사실 확인은 공식 근거를 대조하는 별도 작업이다.
