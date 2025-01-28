create type user_types as enum('publisher','receiver','manager');

create table notification_user
(
 user_id uuid primary key default gen_random_uuid(),
 user_first_name varchar(100),
 user_middle_name varchar(100),
 user_last_name varchar(100),
 user_type user_types not null,
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
 topic_id uuid references notification_topic ( topic_id ),
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create table notification_message
(
 message_id uuid primary key default gen_random_uuid(),
 message_text text,
 topic_id uuid references notification_topic (topic_id),
 created_by uuid references notification_user(user_id),
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp
);

create type message_status as enum ('sent', 'recieved', 'read');

create table user_message_details
(
  user_id uuid references notification_user( user_id ),
  message_id uuid references notification_message ( message_id ),
  topic_id uuid references notification_topic ( topic_id ),
  status message_status not null
);

create index notification_message_user on notification_message(message_id, topic_id);
