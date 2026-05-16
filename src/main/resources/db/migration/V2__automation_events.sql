create table automation_events (
    id uuid primary key,
    source varchar(80) not null,
    event_type varchar(120) not null,
    status varchar(24) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_automation_events_source_created on automation_events (source, created_at desc);
