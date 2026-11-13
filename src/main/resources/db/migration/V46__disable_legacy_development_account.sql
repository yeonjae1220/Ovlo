-- Preserve associated content and history while disabling the reserved legacy development identity.
-- A non-password sentinel also prevents login if the account is accidentally reactivated later.
UPDATE member SET status='SUSPENDED', password='!disabled-legacy-development-account'
WHERE lower(email)='dev@dev.com' AND provider='LOCAL';
