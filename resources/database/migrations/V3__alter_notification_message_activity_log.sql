drop index notification_message_activity_log_idx;

alter table notification_message_activity_log 
drop column receiver,
add column receivers UUID[];

create index notification_message_activity_log_idx on notification_message_activity_log(message_id, topic_id, sender);