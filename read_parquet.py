from pyspark.sql import SparkSession

spark = SparkSession.builder \
    .appName("ReadParquetHDFS") \
    .getOrCreate()

df = spark.read.parquet("hdfs://namenode1:8020/user/youruser/flight_delay_ml_response")
df.show()

spark.stop()
