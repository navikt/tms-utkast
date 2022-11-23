DROP INDEX IF EXISTS packet_eventid_unique_idx;
CREATE UNIQUE INDEX packet_utkastid_unique_idx ON utkast( (packet->>'utkastId') ) ;
