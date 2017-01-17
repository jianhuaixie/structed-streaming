package com.shuding.structedStreaming;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;
import scala.actors.threadpool.Arrays;

import java.util.Iterator;

/**
 * Created by lenovo on 2017/1/6.
 */
public class StructedStreamingStoreFile {
    public static void main(String[] args) {
        System.setProperty("hadoop.home.dir", "D:\\Program Files\\hadoop-2.5.2");
        SparkSession sparkSession = SparkSession
                .builder()
                .appName("StructedStreamingStoreFile")
                .master("local[12]")
                .getOrCreate();
        //Create DataFrame representing the stream of input lines from connection to localhost:9999
        Dataset<Row> socketDF = sparkSession
                .readStream()
                .format("socket")
                .option("host","localhost")
                .option("port",9999)
                .load();
        socketDF.isStreaming();
        socketDF.printSchema();

        Dataset<String> words = socketDF
                .as(Encoders.STRING())
                .flatMap(new FlatMapFunction<String, String>() {
                    public Iterator<String> call(String s) throws Exception {
                        return Arrays.asList(s.split(" ")).iterator();
                    }
                },Encoders.STRING());
        //Read all the csv files written atomically in a directory
        StructType userSchema = new StructType().add("name","string").add("age","integer");
        Dataset<Row> csvDF = sparkSession
                .readStream()
                .option("sep",",")
                .schema(userSchema)
                .csv("D:\\path\\to\\directory");

        Dataset<Row> wordCounts = words.groupBy("value").count();
        // Start running the query that prints the running counts to the console
        StreamingQuery streamingQuery = wordCounts.writeStream()
                .outputMode("complete")
                .format("console")
                .start();

        try {
            streamingQuery.awaitTermination();
        } catch (StreamingQueryException e) {
            e.printStackTrace();
        }
    }
}
