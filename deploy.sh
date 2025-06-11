#!/bin/bash
docker compose up --build -d
docker cp . mongo:/data
docker exec -ti mongo bash
cd data
./resources/import_distances.sh
docker exec -ti mongo /data/resources/import_distances.sh
