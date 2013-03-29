CREATE TABLE users_static ( username VARCHAR(32) NOT NULL, totalcapacity BIGINT NOT NULL, PRIMARY KEY (username) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
delete from users_static;

insert into users_static (totalcapacity ,username)
SELECT
    SUM(OCTET_LENGTH( t.message_body)),
    t.repository_name
FROM
    inbox t
GROUP BY
    t.repository_name;