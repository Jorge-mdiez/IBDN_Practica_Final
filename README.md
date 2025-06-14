# Sistema de Predicción de Vuelos

Este proyecto implementa un sistema de predicción de retrasos en vuelos basado en microservicios. Inicialmente consistía en varios servicios independientes (Flask, Kafka, Spark Streaming, MongoDB), pero ha sido evolucionado con mejoras significativas en la arquitectura y el despliegue.

## Descripción del Proyecto

El sistema permite realizar predicciones de retrasos en vuelos. Las mejoras recientes incluyen:

1. Contenerización completa mediante Docker.
2. Uso de Kafka como sistema de mensajería para la lectura y escritura de predicciones (eliminando MongoDB en este flujo).
3. Incorporación de un flujo en Apache NiFi que consume predicciones de Kafka y las procesa cada 10 segundos.
4. Almacenamiento de predicciones en HDFS en lugar de MongoDB.
5. Despliegue de toda la infraestructura en Kubernetes (aunque Spark aún no funciona en este entorno, impidiendo la generación completa de predicciones).

## Arquitectura

- Flask App (API REST)
- Apache Kafka (mensajería)
- Apache Spark Streaming (procesamiento de streaming)
- Apache NiFi (procesamiento de datos)
- HDFS (almacenamiento de predicciones)
- Docker (contenedorización)
- Kubernetes (orquestación)
- MongoDB (solo para carga inicial de datos)

## Puesta en marcha

### Despliegue local con Docker

Levantar los contenedores:

```bash
docker compose up

## Inicialización de datos en MongoDB (solo para carga de distancias)

Copiar los datos al contenedor de MongoDB:

```bash
docker cp . mongo:/data
docker exec -ti mongo mongo

Ahora el servicio de predicción debería estar accesible en localhost:5001/flights/delays/predict_kafka
Para configurar el flujo de nifi, acceder a localhost:8443/nifi
Una vez realizadas las predicciones, se puede comprobar que se escriben en HDFS entrando a localhost:9870
