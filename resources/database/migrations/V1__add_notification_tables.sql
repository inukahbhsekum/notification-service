create table user
(
 user_id uuid primary key default get_random_uuid(),
 user_first_name varchar(100),
 user_middle_name varchar(100),
 user_last_name varchar(100),
 user_type varchar(100),
 user_metadata jsonb,
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);


create table notification_topic
(
 topic_id uuid primary key default get_random_uuid(),
 title text not null,
 description text,
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);


create table notification_message
(
 message_id uuid primary key default get_random_uuid(),
 message_text text,
 topic_id uuid references notification_topic (topic_id),
 sender uuid references user (user_id),
 receiver uuid references user (user_id),
 created_at timestamp not null default current_timestamp,
 published_at timestamp
);