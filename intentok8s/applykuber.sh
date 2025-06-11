#!/bin/bash

kubectl apply -f mongo-dep.yaml
kubectl apply -f mongo-ser.yaml
kubectl apply -f kafka-dep.yaml
kubectl apply -f kafka-ser.yaml
kubectl apply -f flask-dep.yaml
kubectl apply -f flask-ser.yaml
kubectl apply -f proxy-dep.yaml
kubectl apply -f proxy-ser.yaml

#Para que podamos abrirlo en localhost ya que es tipo ClusterIP y no es accesible desde fuera del clúster
kubectl port-forward svc/flask-app 5001:5001
