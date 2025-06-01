create type notification_medium_types as enum ('websocket', 'push_notification', 'sms', 'whatsapp', 'email', 'in_app', 'message_queue', 'webhook');

create table notification_medium
(
 medium_id uuid primary key default gen_random_uuid(),
 medium_name text,
 medium_supported_apps notification_medium_types[],
 minimum_supported_size real,
 created_by uuid references notification_user(user_id),
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp 
);

alter table notification_message
add column message_medium uuid references notification_medium(medium_id);

create index notification_message_medium_type on notification_message(message_medium);

alter table notification_message_activity_log
add column message_medium uuid references notification_medium(medium_id),
add column message_supported_apps notification_medium_types;