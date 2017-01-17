package com.shuding.structedStreaming;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import scala.actors.threadpool.Arrays;

import java.util.Iterator;

/**
 * Created by lenovo on 2017/1/6.
 */
public class TCPSocketWordCount {
    public static void main(String[] args){
        System.setProperty("hadoop.home.dir", "D:\\Program Files\\hadoop-2.5.2");
        SparkSession sparkSession = SparkSession
                .builder()
                .appName("TCPSocketWordCount")
                .master("local[12]")
                .getOrCreate();
        //Create DataFrame representing the stream of input lines from connection to localhost:9999
        Dataset<Row> lines = sparkSession
                .readStream()
                .format("socket")
                .option("host","localhost")
                .option("port",9999)
                .load();

        //Split the lines into words
        Dataset<String> words = lines
                .as(Encoders.STRING())
                .flatMap(new FlatMapFunction<String, String>() {
                    public Iterator<String> call(String s) throws Exception {
                        return Arrays.asList(s.split(" ")).iterator();
                    }
                },Encoders.STRING());

        //Generate running word count
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
