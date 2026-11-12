-- Existing standalone inquiries remain valid roots. Turns are appended, never overwritten.
ALTER TABLE office_inquiry ADD COLUMN previous_inquiry_id BIGINT REFERENCES office_inquiry(id);
CREATE UNIQUE INDEX uq_inquiry_previous ON office_inquiry(previous_inquiry_id);
ALTER TABLE office_consultation ADD COLUMN shared_conversation TEXT;
