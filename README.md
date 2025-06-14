# Sistema de Predicción de Vuelos

Este proyecto implementa un sistema de predicción de retrasos en vuelos basado en microservicios. Inicialmente consistía en varios servicios independientes (Flask, Kafka, Spark Streaming, MongoDB), pero ha sido evolucionado con mejoras significativas en la arquitectura y el despliegue.

## Descripción del Proyecto

El sistema permite realizar predicciones de retrasos en vuelos. Las mejoras realizadas incluyen:

1. Contenerización completa mediante Docker.
2. Uso de Kafka como sistema de mensajería para la lectura y escritura de predicciones en vez de mongo.
3. Incorporación de un flujo en Apache NiFi que consume predicciones de Kafka y las escribe en un txt.
4. Almacenamiento de predicciones en HDFS en lugar de MongoDB.
5. Despliegue de toda la infraestructura en Kubernetes (aunque Spark no se encuentra funcional, lo que impide la generación de predicciones).

Adicionalmente, se ha creado un read_parquet.py para ver en un df los .parquet de HDFS.

## Puesta en marcha

### Despliegue local con Docker

Levantar los contenedores:

```bash
docker compose up
```
## Inicialización de datos en MongoDB (solo para cargar la bbdd la primera vez que se crea)

Copiar los datos al contenedor de MongoDB:

```bash
docker cp . mongo:/data
docker exec -ti mongo bash
```
y dentro de la terminal de mongo:

```bash
cd data
./resources/import_distances.sh
```

Ahora el servicio de predicción debería estar accesible en (http://localhost:5001/flights/delays/predict_kafka)

Para configurar el flujo de nifi, acceder a (http://localhost:8443/nifi)

Una vez realizadas las predicciones, se puede comprobar que se escriben en HDFS entrando a (http://localhost:9870)

## Despliegue con kubernetes

Iniciar minikube:
```bash
minikube start
```
(Si estamos en los ordenadores del laboratorio, habra que habilitar primero la virtualizacion con k8s)

Desplegamos todos los pods con este comando:
```
kubectl apply -f k8s/
```

Comprobamos que todos los pods se han levantado correctamente:
```bash
kubectl get pods
```
Para cargar en mongo la bbdd (igual que en docker):
```bash
kubectl cp . <nombre_pod_mongo>:/data
kubectl exec -ti <nombre_pod_mongo> -- bash
cd data
./resources/import_distances.sh
```

Podemos acceder al frontend obteniendo la ip de minikube con
```bash
minikube ip
```
y accediendo al puerto (30001/flights/delays/predict_kafka)

El servicio estará disponible y podrás enviar los datos del vuelo para predecir, pero se quedará en "processing...". Esto se debe a que:

-La petición se escribe en el topic de kafka

-Pero spark no funciona correctamente, por lo que no realiza la predicción

## Nota
Tanto las imágenes personalizadas para docker (flask-app) como k8s(flask-app, proxy y spark) están subidas a dockerhub por simplicidad, pero están hechas a partir de los Dockerfiles que se encuentran en el repositorio (o bien en /dockerfiles o en /resources/web).

