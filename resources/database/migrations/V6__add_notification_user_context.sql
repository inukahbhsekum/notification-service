create table notification_user_context
(
 context_id primary key default gen_random_uuid(),
 user_id uuid references notification_user( user_id ),
 available boolean
 on_line boolean
 available_for_messages boolean
 messages_read int
 messages_pending int
 messages_sent int
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create index notification_user_context_idx on notification_user_context(user_id, context_id);
