#!/bin/bash

# Spark master y workers
kubectl apply -f spark-master-dep.yaml
kubectl apply -f spark-master-ser.yaml
kubectl apply -f spark-worker-1-dep.yaml
kubectl apply -f spark-worker-1-ser.yaml
kubectl apply -f spark-worker-2-dep.yaml
kubectl apply -f spark-worker-2-ser.yaml

# Spark submit (Deployment que lanza el job)
kubectl apply -f spark-submit-dep.yaml

# Hadoop HDFS (namenode + datanode)
kubectl apply -f namenode1-dep.yaml
kubectl apply -f namenode1-ser.yaml
kubectl apply -f datanode1-dep.yaml
kubectl apply -f datanode1-ser.yaml

# Nifi
kubectl apply -f nifi-dep.yaml
kubectl apply -f nifi-ser.yaml

# Job para crear los topics en Kafka
kubectl apply -f init-kafka-job.yaml


kubectl port-forward svc/spark-master 8080:8080

kubectl port-forward svc/spark-worker-1 8081:8081

kubectl port-forward svc/spark-worker-2 8082:8081

kubectl port-forward svc/nifi 8443:8443

