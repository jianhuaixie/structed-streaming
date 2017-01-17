package com.shuding.structedStreaming;

import com.shuding.domain.DeviceData;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.expressions.javalang.typed;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;



/**
 * Created by lenovo on 2017/1/12.
 */
public class BasicOperations {
    public static void main(String[] args){
        System.setProperty("hadoop.home.dir", "D:\\Program Files\\hadoop-2.5.2");
        SparkSession sparkSession = SparkSession
                .builder()
                .appName("StructedStreamingStoreFile")
                .master("local[12]")
                .getOrCreate();
        //Create DataFrame representing the stream of input lines from connection to localhost:9999
        /*Dataset<Row> df = sparkSession
                .readStream()
                .format("socket")
                .option("host","localhost")
                .option("port",9999)
                .load();
        df.isStreaming();
        df.printSchema();*/
        JavaRDD<String> lines = sparkSession.sparkContext().textFile("D:\\path\\to\\directory\\device.txt", 5).toJavaRDD();
        JavaRDD<Row> deviceRDD = lines.map(new Function<String, Row>() {
            public Row call(String s) throws Exception {
                String[] splited = s.split("\\t");
                String time[] = splited[3].split("-"); //2017-01-01
                Date date = new Date(Integer.valueOf(time[0])-1900, Integer.valueOf(time[1])-1, Integer.valueOf(time[2]));
                return RowFactory.create(splited[0],splited[1],Double.valueOf(splited[2]),date);
            }
        });
        List<StructField> fields = new ArrayList<StructField>();
        fields.add(DataTypes.createStructField("device",DataTypes.StringType,true));
        fields.add(DataTypes.createStructField("type",DataTypes.StringType,true));
        fields.add(DataTypes.createStructField("signal",DataTypes.DoubleType,true));
        fields.add(DataTypes.createStructField("time",DataTypes.DateType,true));
        StructType schema = DataTypes.createStructType(fields);
        Dataset<Row> df = sparkSession.createDataFrame(deviceRDD,schema);
        df.show();
//        df.select("device").where("signal > 2.1").show();
        Dataset<DeviceData> ds = df.as(ExpressionEncoder.javaBean(DeviceData.class));
        ds.show();
        ds.filter(new FilterFunction<DeviceData>() {
            public boolean call(DeviceData deviceData) throws Exception {
                return deviceData.getSignal()>2.2;
            }
        }).map(new MapFunction<DeviceData, String>() {
            public String call(DeviceData deviceData) throws Exception {
                return deviceData.getDevice();
            }
        },Encoders.STRING()).show();

        ds.groupBy("type").count().show();

        //Running average signal for each device type
        ds.groupByKey(new MapFunction<DeviceData, String>() {
            public String call(DeviceData deviceData) throws Exception {
                return deviceData.getType();
            }
        },Encoders.STRING()).agg(typed.avg(new MapFunction<DeviceData,Double>(){
            @Override
            public Double call(DeviceData value) throws Exception{
                return value.getSignal();
            }
        })).show();
        
        sparkSession.close();
    }
}
