alter table utkast add column slettesEtter timestamp with time zone;

create index utkast_slettes_etter_index on utkast(slettesEtter);
