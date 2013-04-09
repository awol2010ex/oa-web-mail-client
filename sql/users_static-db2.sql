
CREATE TABLE USERS_STATIC(
 USERNAME VARCHAR(32) NOT NULL,
    TOTALCAPACITY BIGINT 
) ;
 
 ALTER TABLE USERS_STATIC
  ADD PRIMARY KEY
   (USERNAME
   );




delete from users_static;

insert into users_static (totalcapacity ,username)
SELECT
    SUM(LENGTH( t.message_body)),
    t.repository_name
FROM
    inbox t
GROUP BY
    t.repository_name;