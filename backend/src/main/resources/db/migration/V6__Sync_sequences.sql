
SELECT setval('user_account_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_account), true);
SELECT setval('country_id_seq', (SELECT COALESCE(MAX(id), 1) FROM country), true);
SELECT setval('collection_id_seq', (SELECT COALESCE(MAX(id), 1) FROM collection), true);
SELECT setval('post_id_seq', (SELECT COALESCE(MAX(id), 1) FROM post), true);
SELECT setval('post_media_id_seq', (SELECT COALESCE(MAX(id), 1) FROM post_media), true);
SELECT setval('travel_permission_id_seq', (SELECT COALESCE(MAX(id), 1) FROM travel_permission), true);
SELECT setval('follow_id_seq', (SELECT COALESCE(MAX(id), 1) FROM follow), true);
SELECT setval('like_post_id_seq', (SELECT COALESCE(MAX(id), 1) FROM like_post), true);
SELECT setval('like_comment_id_seq', (SELECT COALESCE(MAX(id), 1) FROM like_comment), true);
SELECT setval('comment_id_seq', (SELECT COALESCE(MAX(id), 1) FROM comment), true);
SELECT setval('notification_id_seq', (SELECT COALESCE(MAX(id), 1) FROM notification), true);
