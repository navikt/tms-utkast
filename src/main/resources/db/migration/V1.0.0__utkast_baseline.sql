CREATE TABLE IF NOT EXISTS utkast(
    packet jsonb not null,
    opprettet timestamp without time zone not null,
    sistEndret timestamp without time zone,
    slettet timestamp with time zone
);

CREATE INDEX ON utkast ((packet->'eventId'));
CREATE INDEX ON utkast ((packet->'fnr'));


