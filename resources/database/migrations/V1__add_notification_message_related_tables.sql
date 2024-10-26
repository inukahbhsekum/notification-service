create table notification_message_activity_log
(
 message_id uuid references notification_message (message_id),
 topic_id uuid references notification_topic (topic_id),
 sender uuid references notification_user(user_id),
 receiver uuid references notification_user(user_id),
 published_at timestamp,
 action_taken_at timestamp not null,
 created_at timestamp not null default current_timestamp
);