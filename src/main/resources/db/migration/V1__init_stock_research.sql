create table instruments (
    id uuid primary key,
    symbol varchar(32) not null,
    name varchar(160) not null,
    market varchar(16) not null,
    exchange varchar(40) not null,
    security_code varchar(40) not null,
    sector varchar(80),
    currency varchar(12) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_instruments_symbol_exchange unique (symbol, exchange)
);

create index idx_instruments_symbol on instruments (symbol);
create index idx_instruments_market on instruments (market);
create index idx_instruments_name on instruments (name);

create table price_bars (
    id uuid primary key,
    instrument_id uuid not null references instruments (id) on delete cascade,
    trade_date date not null,
    open_price numeric(20, 6) not null,
    high_price numeric(20, 6) not null,
    low_price numeric(20, 6) not null,
    close_price numeric(20, 6) not null,
    volume numeric(24, 0) not null,
    provider varchar(80) not null,
    source_key varchar(160) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_price_bars_instrument_date_provider unique (instrument_id, trade_date, provider)
);

create index idx_price_bars_instrument_date on price_bars (instrument_id, trade_date desc);

create table source_materials (
    id uuid primary key,
    instrument_id uuid references instruments (id) on delete set null,
    kind varchar(32) not null,
    provider varchar(80) not null,
    publisher varchar(120) not null,
    title varchar(360) not null,
    summary text,
    source_url varchar(1000),
    source_key varchar(240) not null,
    language varchar(16) not null,
    published_at timestamp with time zone,
    fetched_at timestamp with time zone not null,
    raw_payload text,
    checksum varchar(96) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_source_materials_provider_key unique (provider, source_key)
);

create index idx_source_materials_kind_published on source_materials (kind, published_at desc);
create index idx_source_materials_instrument on source_materials (instrument_id);

create table research_snapshots (
    id uuid primary key,
    user_id varchar(120),
    instrument_id uuid not null references instruments (id) on delete cascade,
    note text not null,
    stance varchar(24) not null,
    conviction varchar(24) not null,
    thesis text not null,
    price numeric(20, 6) not null,
    change_percent numeric(12, 6) not null,
    score numeric(10, 4) not null,
    selected_event_title varchar(240),
    selected_event_date varchar(40),
    active_rule_labels text,
    preset_name varchar(160),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_research_snapshots_instrument_created on research_snapshots (instrument_id, created_at desc);
create index idx_research_snapshots_user_created on research_snapshots (user_id, created_at desc);

create table report_schedules (
    id uuid primary key,
    user_id varchar(120) not null,
    locale varchar(16) not null,
    cadence varchar(16) not null,
    delivery_email varchar(240) not null,
    timezone varchar(80) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_report_schedules_user on report_schedules (user_id);

create table subscription_plans (
    id uuid primary key,
    code varchar(40) not null unique,
    name varchar(80) not null,
    monthly_price numeric(12, 2) not null,
    currency varchar(12) not null,
    feature_limits text not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table media_assets (
    id uuid primary key,
    instrument_id uuid references instruments (id) on delete set null,
    material_id uuid references source_materials (id) on delete set null,
    kind varchar(32) not null,
    title varchar(240) not null,
    source_url varchar(1000) not null,
    provider varchar(80) not null,
    language varchar(16) not null,
    published_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table localization_jobs (
    id uuid primary key,
    media_asset_id uuid not null references media_assets (id) on delete cascade,
    provider varchar(80) not null,
    target_language varchar(16) not null,
    status varchar(24) not null,
    dubbed_audio_url varchar(1000),
    subtitle_url varchar(1000),
    error_message text,
    requested_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_localization_jobs_status on localization_jobs (status, requested_at desc);
