package es.upm.dit.ging.predictor
import com.mongodb.spark._
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.ml.feature.{Bucketizer, StringIndexerModel, VectorAssembler}
import org.apache.spark.sql.functions.{concat, from_json, lit}
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions.struct
import org.apache.spark.sql.functions.to_json


object MakePrediction {

  def main(args: Array[String]): Unit = {
    println("Fligth predictor starting...")

    val spark = SparkSession
      .builder
      .appName("StructuredNetworkWordCount")
      .master("spark://spark-master:7077")
      .getOrCreate()
    import spark.implicits._

    //Load the arrival delay bucketizer
    val base_path= "/home/ibdn/practica_creativa"
    val arrivalBucketizerPath = "%s/models/arrival_bucketizer_2.0.bin".format(base_path)
    print(arrivalBucketizerPath.toString())
    val arrivalBucketizer = Bucketizer.load(arrivalBucketizerPath)
    val columns= Seq("Carrier","Origin","Dest","Route")

    //Load all the string field vectorizer pipelines into a dict
    val stringIndexerModelPath =  columns.map(n=> ("%s/models/string_indexer_model_"
      .format(base_path)+"%s.bin".format(n)).toSeq)
    val stringIndexerModel = stringIndexerModelPath.map{n => StringIndexerModel.load(n.toString)}
    val stringIndexerModels  = (columns zip stringIndexerModel).toMap

    // Load the numeric vector assembler
    val vectorAssemblerPath = "%s/models/numeric_vector_assembler.bin".format(base_path)
    val vectorAssembler = VectorAssembler.load(vectorAssemblerPath)

    // Load the classifier model
    val randomForestModelPath = "%s/models/spark_random_forest_classifier.flight_delays.5.0.bin".format(
      base_path)
    val rfc = RandomForestClassificationModel.load(randomForestModelPath)

    //Process Prediction Requests in Streaming
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("subscribe", "flight-delay-ml-request")
      .load()
    df.printSchema()

    val flightJsonDf = df.selectExpr("CAST(value AS STRING)")

    val struct = new StructType()
      .add("Origin", DataTypes.StringType)
      .add("FlightNum", DataTypes.StringType)
      .add("DayOfWeek", DataTypes.IntegerType)
      .add("DayOfYear", DataTypes.IntegerType)
      .add("DayOfMonth", DataTypes.IntegerType)
      .add("Dest", DataTypes.StringType)
      .add("DepDelay", DataTypes.DoubleType)
      .add("Prediction", DataTypes.StringType)
      .add("Timestamp", DataTypes.TimestampType)
      .add("FlightDate", DataTypes.DateType)
      .add("Carrier", DataTypes.StringType)
      .add("UUID", DataTypes.StringType)
      .add("Distance", DataTypes.DoubleType)
      .add("Carrier_index", DataTypes.DoubleType)
      .add("Origin_index", DataTypes.DoubleType)
      .add("Dest_index", DataTypes.DoubleType)
      .add("Route_index", DataTypes.DoubleType)

    val flightNestedDf = flightJsonDf.select(from_json($"value", struct).as("flight"))
    flightNestedDf.printSchema()

    // DataFrame for Vectorizing string fields with the corresponding pipeline for that column
    val flightFlattenedDf = flightNestedDf.selectExpr("flight.Origin",
      "flight.DayOfWeek","flight.DayOfYear","flight.DayOfMonth","flight.Dest",
      "flight.DepDelay","flight.Timestamp","flight.FlightDate",
      "flight.Carrier","flight.UUID","flight.Distance")
    flightFlattenedDf.printSchema()

    val predictionRequestsWithRouteMod = flightFlattenedDf.withColumn(
      "Route",
                concat(
                  flightFlattenedDf("Origin"),
                  lit('-'),
                  flightFlattenedDf("Dest")
                )
    )

    // Dataframe for Vectorizing numeric columns
    val flightFlattenedDf2 = flightNestedDf.selectExpr("flight.Origin",
      "flight.DayOfWeek","flight.DayOfYear","flight.DayOfMonth","flight.Dest",
      "flight.DepDelay","flight.Timestamp","flight.FlightDate",
      "flight.Carrier","flight.UUID","flight.Distance",
      "flight.Carrier_index","flight.Origin_index","flight.Dest_index","flight.Route_index")
    flightFlattenedDf2.printSchema()

    val predictionRequestsWithRouteMod2 = flightFlattenedDf2.withColumn(
      "Route",
      concat(
        flightFlattenedDf2("Origin"),
        lit('-'),
        flightFlattenedDf2("Dest")
      )
    )

    // Vectorize string fields with the corresponding pipeline for that column
    // Turn category fields into categoric feature vectors, then drop intermediate fields
    val predictionRequestsWithRoute = stringIndexerModel.map(n=>n.transform(predictionRequestsWithRouteMod))

    //Vectorize numeric columns: DepDelay, Distance and index columns
    val vectorizedFeatures = vectorAssembler.setHandleInvalid("keep").transform(predictionRequestsWithRouteMod2)

    // Inspect the vectors
    vectorizedFeatures.printSchema()

    // Drop the individual index columns
    val finalVectorizedFeatures = vectorizedFeatures
        .drop("Carrier_index")
        .drop("Origin_index")
        .drop("Dest_index")
        .drop("Route_index")

    // Inspect the finalized features
    finalVectorizedFeatures.printSchema()

    // Make the prediction
    val predictions = rfc.transform(finalVectorizedFeatures)
      .drop("Features_vec")

    // Drop the features vector and prediction metadata to give the original fields
    val finalPredictions = predictions.drop("indices").drop("values").drop("rawPrediction").drop("probability")

    // Inspect the output
    finalPredictions.printSchema()

    // define streaming query para MongoDB
//val dataStreamWriter = finalPredictions
//.writeStream
//  .format("mongodb")
//  .option("spark.mongodb.connection.uri", "mongodb://mongo:27017")
//  .option("spark.mongodb.database", "agile_data_science")
//  .option("checkpointLocation", "/tmp/checkpoints-mongo")
//  .option("spark.mongodb.collection", "flight_delay_ml_response")
//  .outputMode("append")
//  .start()

val hdfsOutputPath = "hdfs://namenode1:8020/user/youruser/flight_delay_ml_response"

val hdfsWriter = finalPredictions
  .writeStream
  .format("parquet")
  .option("path", hdfsOutputPath)
  .option("checkpointLocation", "/tmp/checkpoints-hdfs")
  .outputMode("append")
  .start()



val kafkaOutput = finalPredictions.selectExpr("to_json(struct(*)) as value")

// define streaming query para Kafka
val kafkaWriter = kafkaOutput.writeStream
  .format("kafka")
  .option("kafka.bootstrap.servers", "kafka:9092")
  .option("topic", "flight-delay-ml-response")
  .option("checkpointLocation", "/tmp/checkpoints-kafka")
  .outputMode("append")
  .start()

// Console Output para depuración
val consoleOutput = finalPredictions.writeStream
  .outputMode("append")
  .format("console")
  .start()

// Esperar terminación de todos los streams
kafkaWriter.awaitTermination()
consoleOutput.awaitTermination()
hdfsWriter.awaitTermination()
    
  }

}
