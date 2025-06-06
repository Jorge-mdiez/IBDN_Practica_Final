#!/bin/bash
docker compose up --build -d
docker cp ./data/origin_dest_distances.jsonl mongo:/data
docker exec -ti mongo /data/resources/import_distances.sh