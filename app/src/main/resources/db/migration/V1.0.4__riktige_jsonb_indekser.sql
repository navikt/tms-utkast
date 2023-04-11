create index utkast_index_id on utkast using gin ((packet -> 'utkastId'));
create index utkast_index_ident on utkast using gin ((packet -> 'ident'));
