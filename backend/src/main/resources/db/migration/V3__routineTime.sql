-- 새 컬럼 추가
ALTER TABLE eeum.routine
  ADD COLUMN act_time_kst time(0);

-- 기존 timestamptz → 한국시간 변환 후 시:분만 추출
UPDATE eeum.routine
   SET act_time_kst = (act_time AT TIME ZONE 'Asia/Seoul')::time(0);

-- 원래 act_time 제거
ALTER TABLE eeum.routine
  DROP COLUMN act_time;

-- 새 컬럼 이름 변경
ALTER TABLE eeum.routine
  RENAME COLUMN act_time_kst TO act_time;
