create table notification_message_activity_log
(
 id uuid primary key default gen_random_uuid(),
 message_id uuid references notification_message (message_id),
 topic_id uuid references notification_topic (topic_id),
 sender uuid references notification_user(user_id),
 receiver uuid references notification_user(user_id),
 meta json,
 action_taken_at timestamp not null,
 created_at timestamp not null default current_timestamp
);

create index notification_message_activity_log_idx on notification_message_activity_log(message_id, topic_id, sender, receiver);
