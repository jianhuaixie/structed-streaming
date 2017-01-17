package com.shuding.structedStreaming;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import scala.Tuple2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lenovo on 2017/1/12.
 */
public class WindowOperationOnEventTime {
    public static void main(String[] args){
        System.setProperty("hadoop.home.dir", "D:\\Program Files\\hadoop-2.5.2");
        SparkSession sparkSession = SparkSession
                .builder()
                .appName("TCPSocketWordCount")
                .config("spark.default.parallelism","20")
                .master("local[12]")
                .getOrCreate();
        //Create DataFrame representing the stream of input lines from connection to localhost:9999
        Dataset<Row> lines = sparkSession
                .readStream()
                .format("socket")
                .option("host","localhost")
                .option("port",9999)
                .option("includeTimestamp",true)
                .load();

        String windowDuration = "10 seconds";
        String slideDuration = "5 seconds";

        //Split the lines into words,retaining timestamp
        Dataset<Row> words = lines
                .as(Encoders.tuple(Encoders.STRING(), Encoders.TIMESTAMP()))
                .flatMap(
                        new FlatMapFunction<Tuple2<String, Timestamp>, Tuple2<String, Timestamp>>() {
                            @Override
                            public Iterator<Tuple2<String, Timestamp>> call(Tuple2<String, Timestamp> t) {
                                List<Tuple2<String, Timestamp>> result = new ArrayList<Tuple2<String, Timestamp>>();
                                for (String word : t._1.split(" ")) {
                                    result.add(new Tuple2<String, Timestamp>(word, t._2));
                                }
                                return result.iterator();
                            }
                        },
                        Encoders.tuple(Encoders.STRING(), Encoders.TIMESTAMP())
                ).toDF("word", "timestamp");

        //Group the data by window and word and compute the count of each group
        Dataset<Row> windowedCounts = words.groupBy(
                functions.window(words.col("timestamp"), windowDuration, slideDuration),
                words.col("word")
        ).count().orderBy("window");

        // Start running the query that prints the running counts to the console
        StreamingQuery streamingQuery = windowedCounts.writeStream()
                .outputMode("complete")
                .format("console")
                .option("truncate","false")
                .start();
        try {
            streamingQuery.awaitTermination();
        } catch (StreamingQueryException e) {
            e.printStackTrace();
        }
    }

}
