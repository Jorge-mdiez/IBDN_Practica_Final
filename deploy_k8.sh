#!/bin/bash

kubectl cp . <copiaraquinombrepodmongo>:/data

kubectl exec -ti <copiaraquinombrepodmongo> -- bash
cd /data
./resources/import_distances.sh
