ALTER TABLE eeum.user_home
  ADD COLUMN is_primary boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX ux_user_home_primary_one
  ON eeum.user_home(user_id)
  WHERE is_primary = true;

WITH firsts AS (
  SELECT user_id, MIN(user_home_id) AS first_id
  FROM eeum.user_home
  GROUP BY user_id
)
UPDATE eeum.user_home uh
SET is_primary = true
FROM firsts f
WHERE uh.user_home_id = f.first_id;