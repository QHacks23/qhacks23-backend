docker run -d -e POSTGRES_USER=qhacks -e POSTGRES_PASSWORD=qhacks -e PGDATA=/var/lib/prostgressql/data/pgdata -v /tmp/postgres-data:/var/lib/postgressql/data -p "5432:5432" postgres