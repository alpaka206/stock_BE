create table user_accounts (
    id uuid primary key,
    provider varchar(40) not null,
    provider_user_id varchar(160) not null,
    email varchar(240) not null,
    display_name varchar(160) not null,
    locale varchar(16) not null,
    role varchar(40) not null,
    active boolean not null default true,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_user_accounts_provider_user unique (provider, provider_user_id),
    constraint uq_user_accounts_email unique (email)
);

create index idx_user_accounts_email on user_accounts (email);

create table refresh_sessions (
    id uuid primary key,
    user_id uuid not null references user_accounts (id) on delete cascade,
    token_hash varchar(96) not null unique,
    user_agent varchar(512),
    ip_address varchar(80),
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_refresh_sessions_user_expires on refresh_sessions (user_id, expires_at desc);
create index idx_refresh_sessions_token_hash on refresh_sessions (token_hash);
