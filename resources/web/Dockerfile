# Usa una imagen base de Python
FROM python:3.10-slim

# Define la ruta raíz del proyecto en el contenedor
ENV PROJECT_HOME=/app

# Establece el directorio de trabajo
WORKDIR /app/resources/web

# Copia todo el proyecto al contenedor
COPY . /app/resources/web

# Instala dependencias
RUN pip install --no-cache-dir -r requirements.txt

# Expone el puerto del Flask
EXPOSE 5001

# Comando para lanzar Flask
CMD ["python", "predict_flask.py"]
