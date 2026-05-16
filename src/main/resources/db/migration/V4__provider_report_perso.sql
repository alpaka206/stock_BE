alter table localization_jobs add column provider_job_id varchar(120);
alter table localization_jobs add column provider_payload text;

create index idx_localization_jobs_provider_job on localization_jobs (provider, provider_job_id);

create table report_deliveries (
    id uuid primary key,
    report_schedule_id uuid references report_schedules (id) on delete set null,
    user_id varchar(120) not null,
    locale varchar(16) not null,
    cadence varchar(16) not null,
    delivery_email varchar(240) not null,
    subject varchar(240) not null,
    text_body text not null,
    html_body text not null,
    status varchar(24) not null,
    error_message text,
    generated_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_report_deliveries_user_generated on report_deliveries (user_id, generated_at desc);
