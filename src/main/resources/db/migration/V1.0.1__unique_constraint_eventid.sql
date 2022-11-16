DROP INDEX IF EXISTS utkast_expr_idx;
CREATE UNIQUE INDEX packet_eventid_unique_idx ON utkast( (packet->>'eventId') ) ;


