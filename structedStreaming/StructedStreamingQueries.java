package com.shuding.structedStreaming;

import com.shuding.domain.DeviceData;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import scala.Tuple4;

import java.sql.Date;

/**
 * Created by lenovo on 2017/1/13.
 */
public class StructedStreamingQueries {
    public static void main(String[] args){
        System.setProperty("hadoop.home.dir", "D:\\Program Files\\hadoop-2.5.2");
        SparkSession sparkSession = SparkSession
                .builder()
                .appName("StructedStreamingQueries")
                .config("spark.default.parallelism","20")
                .master("local[12]")
                .getOrCreate();
        //Create DataFrame representing the stream of input lines from connection to localhost:9999
        Dataset<Row> lines = sparkSession
                .readStream()
                .format("socket")
                .option("host","localhost")
                .option("port",9999)
//                .option("includeTimestamp",true)
                .load();


        /*Dataset<Row> deviceDataDF = lines.as(Encoders.STRING()).map(new MapFunction<String, Tuple4<String, String, Double, Date>>() {
            @Override
            public Tuple4<String, String, Double, Date> call(String s) throws Exception {
                String[] splited = s.split(" ");
                String time[] = splited[3].split("-");
                Date date = new Date(Integer.valueOf(time[0]) - 1900, Integer.valueOf(time[1]) - 1, Integer.valueOf(time[2]));
                return new Tuple4<String, String, Double, Date>(splited[0], splited[1], Double.valueOf(splited[3]), date);
            }
        }, Encoders.tuple(Encoders.STRING(), Encoders.STRING(), Encoders.DOUBLE(), Encoders.DATE())).toDF("device", "type", "signal", "time");*/

        Dataset<Row> deviceDataDF = lines.as(Encoders.STRING()).map(new MapFunction<String, DeviceData>() {
            @Override
            public DeviceData call(String s) throws Exception {
                String[] splited = s.split(" ");
                String time[] = splited[3].split("-");
                Date date = new Date(Integer.valueOf(time[0]) - 1900, Integer.valueOf(time[1]) - 1, Integer.valueOf(time[2]));
                DeviceData deviceData = new DeviceData();
                deviceData.setDevice(splited[0]);
                deviceData.setType(splited[1]);
                deviceData.setSignal(Double.valueOf(splited[2]));
                deviceData.setTime(date);
                return deviceData;
            }
        },Encoders.bean(DeviceData.class)).toDF("device", "type", "signal", "time");

//        Dataset<Row> noAggDF = deviceDataDF.select("device").where("signal > 2");

        //print new data to console
        /*deviceDataDF.writeStream()
                .format("console")
                .start();*/

        //write new data to Parquet files
//        noAggDF.writeStream();

        //DF with aggregation
        Dataset<Row> aggDF = deviceDataDF.groupBy("device").count();

        //print updated aggregations to console
        StreamingQuery streamingQuery = aggDF.writeStream()
                .outputMode("complete")
                .format("console")
                .start();

        //Have all the aggregations in an in-memory table
     /*    aggDF.writeStream()
                .queryName("aggregates") //this query name will be the table name
                .outputMode("complete")
                .format("memory")
                .start();*/

//        sparkSession.sql("select * from aggregates").show();  //interactively query in-memory table
        try {
            streamingQuery.awaitTermination();
        } catch (StreamingQueryException e) {
            e.printStackTrace();
        }
    }
}
