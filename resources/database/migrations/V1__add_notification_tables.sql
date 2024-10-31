create table notification_user
(
 user_id uuid primary key default gen_random_uuid(),
 user_first_name varchar(100),
 user_middle_name varchar(100),
 user_last_name varchar(100),
 user_type varchar(100) not null,
 user_metadata json,
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create index notification_user_id_idx on notification_user(user_id, user_type);

create table notification_topic
(
 topic_id uuid primary key default gen_random_uuid(),
 title text not null,
 description text,
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create table user_notification_topic
(
 user_id uuid references notification_user( user_id ),
 topic_id uuid references notification_topic ( topic_id )
);

create table notification_message
(
 message_id uuid primary key default gen_random_uuid(),
 message_text text,
 topic_id uuid references notification_topic (topic_id),
 sender uuid references notification_user(user_id),
 receiver uuid references notification_user(user_id),
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create index notification_message_user on notification_message(message_id, topic_id);
create index notification_message_sender on notification_message(message_id, sender);
create index notification_message_receiver on notification_message(message_id, receiver);
create index notification_message_all on notification_message(message_id, topic_id, sender, receiver);
