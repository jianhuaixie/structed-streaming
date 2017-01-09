# <center>Structed Stareaming</center>
## Overview
结构化流是一个建立在Spark SQL引擎上的可伸缩的、可容错的流式处理引擎。结构化流既可以表达数据流计算结果，同样也可以在静态数据上表达批处理结果。随着流式数据持续不断的到来，Spark SQL引擎会专注于增量地、连续地运行这些数据，从而不断更新最后的结果。可以可用Scala, Java or Python，基于DataSet/DataFrame API去表达流式处理的聚合，事件窗口操作，流数据和批数据join等，底层的计算执行发生在经过优化的Spark SQL引擎上。最后，整个系统通过checkpoint和Write Ahead Logs（预写日志）来确保端到端的，exactly-once容错保证。简而言之，结构化流提供快速的，可伸缩的，高容错的，端到端exactly-once的流式处理，用户根本就不需要关心流的存在。
## Background
Spark2.0之前的Spark Streaming是把流式计算看成一个个离线计算来完成流计算，提供一套DStream的流API，相比其他的流计算，其优点是容错性和吞吐量上有优势，在2.0版本之前，用户在使用时，如果有流计算，又有离线计算，需要两套API去编写程序，一套是RDD API，一套是DStream API，而且DStream API在易用性上远不如SQL或DataFrame。为了真正将流计算和离线计算在编程API上统一，同时也让Streaming作业能够享受DataFrame/Dataset上所带来的优势：性能提升和API易用，于是提出了Structed Streaming，最后我们只需要基于DataFrame/Dataset可以开发离线计算和流计算的程序，很容易使得Spark在API跟业界所说的DataFlow来统一离线计算和流计算效果一样。
## Quick Example
从一个TCP socket上监听得到文本数据，然后运行world count，如何用结构化流来处理。

	import org.apache.spark.api.java.function.FlatMapFunction;
	import org.apache.spark.sql.*;
	import org.apache.spark.sql.streaming.StreamingQuery;

	import java.util.Arrays;
	import java.util.Iterator;

	SparkSession spark = SparkSession
		.builder()
		.appName("JavaStructuredNetworkWordCount")
		.getOrCreate();

	// Create DataFrame representing the stream of input lines from connection to localhost:9999
	Dataset<Row> lines = spark
	  .readStream()
	  .format("socket")
	  .option("host", "localhost")
	  .option("port", 9999)
	  .load();
	
	// Split the lines into words
	Dataset<String> words = lines
	  .as(Encoders.STRING())
	  .flatMap(
	    new FlatMapFunction<String, String>() {
	      @Override
	      public Iterator<String> call(String x) {
	        return Arrays.asList(x.split(" ")).iterator();
	      }
	    }, Encoders.STRING());
	
	// Generate running word count
	Dataset<Row> wordCounts = words.groupBy("value").count();
	
	// Start running the query that prints the running counts to the console
	StreamingQuery query = wordCounts.writeStream()
	  .outputMode("complete")
	  .format("console")
	  .start();

query.awaitTermination();
这里的DataFrame lines代表一个包含了流式文本数据的无限制的table，这个table包含了"value"这一类，流式文本数据中的每一行都变成了这个table的一行。注意，这个过程并没有在当下接收任何数据，只是定义了数据的转换过程而已，还没有开始启动。紧接着，用.as(Encoders.STRING())将DataFrame转成DataSet。然后用flatMap操作将每一行切成多个词，词的结果集合words是一个DataSet,包含所有的切分出来的词。最后，定义wordCounts，用唯一的value进行分组统计。
我们现在可以设置对流数据的查询。 剩下的只是实际开始接收数据和计算统计。 为此，我们将其设置为在每次更新时将一组完整的计数（由outputMode（“complete”）指定）打印到控制台。 然后使用start()启动流计算。
### 编程模型
结构化流的关键思想是将实时数据流视为一个连续附加的表。 这导致与批处理模型非常相似的新的流处理模型。 您将在静态表上表达您的流计算作为标准批处理查询，Spark将其作为无界输入表上的增量查询运行。 让我们更详细地了解这个模型。
#### 基本概念
将输入数据流视为“输入表”。 到达流上的每个数据项都像添加到输入表的新行。
![](http://spark.apache.org/docs/latest/img/structured-streaming-stream-as-a-table.png)
对输入的查询将生成“结果表”。 每个触发间隔（例如，每1秒），新行将附加到输入表，最终更新结果表。 无论何时更新结果表，我们都希望将更改的结果行写入外部接收器。
![](http://spark.apache.org/docs/latest/img/structured-streaming-model.png)
“Output”定义为写入外部存储器的内容。 output可以在不同模式下定义。

- Complete Mode：整个更新的结果表将被写入外部存储器。 由存储连接器决定如何处理整个表的写入。
	
- Append Mode：只有自上次触发后结果表中附加的新行将被写入外部存储器。 这仅适用于结果表中的现有行不会更改的查询。
 
- Update Mode：只有自上次触发后在结果表中更新的行将被写入外部存储器（在Spark 2.0中尚不可用）。 请注意，这不同于完整模式，因为此模式不输出未更改的行。

请注意，每种模式适用于某些类型的查询。

为了说明这个模型的使用，让我们在上面的快速示例的上下文中理解模型。 第一行DataFrame是输入表，最后的wordCounts DataFrame是结果表。 请注意，在流线上DataFrame生成wordCounts的查询与静态DataFrame完全相同。 但是，当此查询启动时，Spark将不断地从套接字连接中检查新数据。 如果有新数据，Spark将运行一个“增量”查询，将以前运行的计数与新数据组合，以计算更新的计数，如下所示。
![](http://spark.apache.org/docs/latest/img/structured-streaming-example-model.png)
这个模型与许多其他流处理引擎显着不同。 许多流传输系统需要用户自己维护运行的聚合，因此必须关注容错和数据一致性（至少一次，或最多一次或完全一次）。 在这个模型中，Spark专注在有新数据时更新结果表，从而减轻用户对它的推理。 作为示例，让我们看看这个模型如何处理基于事件时间的处理和晚到达的数据。

##### Handling Event-time and Late Data
事件时间是嵌入在数据本身中的时间。 对于许多应用程序，您可能希望在此事件时间操作。 例如，如果要获取IoT设备每分钟生成的事件数，则可能需要使用生成数据的时间（即数据中的事件时间），而不是Spark接收数据的时间。 此事件时间在此模型中可以非常自然地表示——来自设备的每个事件是表中的一行，事件时间是行中的列值。 这允许基于窗口的聚合（例如每分钟的事件数）仅仅是偶数时间列上的特殊类型的分组和聚合 - 每个时间窗口是组，并且每行可以属于多个窗口/组。 因此，可以在静态数据集（例如，来自收集的设备事件日志）以及数据流上一致地定义这种基于事件时间窗的聚合查询，使得用户的生命周期更容易。
此外，该模型自然地处理基于事件时间比预期到达的数据。 由于Spark正在更新结果表，因此当存在延迟数据时，它可以完全控制更新旧聚合，以及清除旧聚合以限制中间状态数据的大小。 由于Spark 2.1，我们支持水印，允许用户指定后期数据的阈值，并允许引擎相应地清除旧的状态。 稍后将在“窗口操作”部分中对此进行详细说明。

##### Fault Tolerance Semantics
提供端到端的一次性语义是结构化流的设计背后的关键目标之一。 为了实现这一点，我们设计了结构化流源，接收器和执行引擎，以可靠地跟踪处理的确切进展，以便它可以通过重新启动和/或重新处理来处理任何类型的故障。 假定每个流源具有偏移量（类似于Kafka偏移量或Kinesis序列号）以跟踪流中的读取位置。 引擎使用检查点和预写日志记录每个触发器中正在处理的数据的偏移范围。 流接收器被设计为用于处理再处理的幂等。 结合使用可重放源和幂等宿，结构化流可以确保在任何故障下的端到端的一次性语义。

##### Basic Operations - Selection, Projection, Aggregation
	
	import org.apache.spark.api.java.function.*;
	import org.apache.spark.sql.*;
	import org.apache.spark.sql.expressions.javalang.typed;
	import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
	
	public class DeviceData {
	  private String device;
	  private String type;
	  private Double signal;
	  private java.sql.Date time;
	  ...
	  // Getter and setter methods for each field
	}
	
	Dataset<Row> df = ...;    // streaming DataFrame with IOT device data with schema { device: string, type: string, signal: double, time: DateType }
	Dataset<DeviceData> ds = df.as(ExpressionEncoder.javaBean(DeviceData.class)); // streaming Dataset with IOT device data
	
	// Select the devices which have signal more than 10
	df.select("device").where("signal > 10"); // using untyped APIs
	ds.filter(new FilterFunction<DeviceData>() { // using typed APIs
	  @Override
	  public boolean call(DeviceData value) throws Exception {
	    return value.getSignal() > 10;
	  }
	}).map(new MapFunction<DeviceData, String>() {
	  @Override
	  public String call(DeviceData value) throws Exception {
	    return value.getDevice();
	  }
	}, Encoders.STRING());
	
	// Running count of the number of updates for each device type
	df.groupBy("type").count(); // using untyped API
	
	// Running average signal for each device type
	ds.groupByKey(new MapFunction<DeviceData, String>() { // using typed API
	  @Override
	  public String call(DeviceData value) throws Exception {
	    return value.getType();
	  }
	}, Encoders.STRING()).agg(typed.avg(new MapFunction<DeviceData, Double>() {
	  @Override
	  public Double call(DeviceData value) throws Exception {
	    return value.getSignal();
	  }
	}));

##### Window Operations on Event Time
滑动事件时间窗口上的聚合通过结构化流传输是很直接的。理解基于窗口的聚合的关键思想与分组聚合非常相似。在分组聚合中，为用户指定的分组列中的每个唯一值维护聚合值（例如计数）。在基于窗口的聚合的情况下，为行的事件时间落入的每个窗口维护聚合值。让我们用插图来理解这个。

想象一下，我们的快速示例被修改，流现在包含行以及生成行的时间。我们不想运行字数，而是计算10分钟内的字数，每5分钟更新一次。也就是说，在10分钟窗口12：00-12：10,12：05-12：15,12：10-12：20等之间接收的字中的字数。注意，12:00 -12:10意味着数据在12:00之后但在12:10之前到达。现在，考虑在12:07收到的一个字。这个词应该增加对应于两个窗口12:00 - 12:10和12:05 - 12:15的计数。因此，计数将通过分组键（即字）和窗口（可以从事件时间计算）来索引。

结果表将如下所示。
![](http://spark.apache.org/docs/latest/img/structured-streaming-window.png)
由于这个窗口类似于分组，在代码中，可以使用groupBy（）和window（）操作来表达窗口化聚合
	
	Dataset<Row> words = ... // streaming DataFrame of schema { timestamp: Timestamp, word: String }

	// Group the data by window and word and compute the count of each group
	Dataset<Row> windowedCounts = words.groupBy(
	  functions.window(words.col("timestamp"), "10 minutes", "5 minutes"),
	  words.col("word")
	).count();

##### Handling Late Data and Watermarking
现在考虑如果事件中的一个到达应用程序的迟到会发生什么。 例如，例如，在12:04（即事件时间）生成的词可以在12:11由应用程序接收。 应用程序应使用时间12:04而不是12:11来更新窗口12:00 - 12:10的旧计数。 这在我们基于窗口的分组中自然地发生 - 结构化流可以长时间地保持部分聚合的中间状态，使得晚期数据可以正确地更新旧窗口的聚集，如下所示。
![](http://spark.apache.org/docs/latest/img/structured-streaming-late-data.png)
但是，要运行此查询的天数，系统必须绑定其积累的中间内存中状态的数量。这意味着系统需要知道何时可以从内存中状态删除旧聚合，因为应用程序将不再接收该聚合的延迟数据。为了实现这一点，在Spark 2.1中，我们引入了水印，让引擎自动跟踪数据中的当前事件时间，并尝试相应地清理旧状态。您可以通过指定事件时间列和根据事件时间预期数据的延迟时间的阈值来定义查询的水印。对于在时间T开始的特定窗口，引擎将保持状态并允许后期数据更新状态，直到（由引擎看到的最大事件时间 - 后期阈值> T）。换句话说，阈值内的滞后数据将被聚合，但是晚于阈值的数据将被丢弃。让我们用一个例子来理解这个。我们可以使用withWatermark()在前面的例子中轻松定义水印，如下所示。
	
	Dataset<Row> words = ... // streaming DataFrame of schema { timestamp: Timestamp, word: String }

	// Group the data by window and word and compute the count of each group
	Dataset<Row> windowedCounts = words
	    .withWatermark("timestamp", "10 minutes")
	    .groupBy(
	        functions.window(words.col("timestamp"), "10 minutes", "5 minutes"),
	        words.col("word"))
	    .count();

在这个例子中，我们定义查询的水印对列“timestamp”的值，并且还定义“10分钟”作为允许数据的延迟的阈值。 如果此查询在Append输出模式（稍后在“输出模式”部分中讨论）中运行，引擎将从列“timestamp”跟踪当前事件时间，并在最终确定窗口计数和添加之前等待事件时间的额外“10分钟” 他们到结果表。 这是一个例证。
![](http://spark.apache.org/docs/latest/img/structured-streaming-watermark.png)
如图所示，由引擎跟踪的最大事件时间是蓝色虚线，并且在每个触发的开始处设置为（最大事件时间 - '10分钟'）的水印是红色线。例如，当引擎观察数据（12:14，dog），它将下一个触发器的水印设置为12:04。对于窗口12:00 - 12:10，部分计数保持为内部状态，而系统正在等待延迟数据。在系统找到数据（即（12:21，owl））使得水印超过12:10之后，部分计数被最终确定并附加到表。此计数将不会进一步更改，因为所有超过12:10的“太晚”数据将被忽略。

请注意，在追加输出模式下，系统必须等待“延迟阈值”时间才能输出窗口的聚合。如果数据可能很晚（比如说1天），并且你喜欢有部分计数而不等待一天，这可能不是理想的。将来，我们将添加更新输出模式，这将允许每次更新聚合写入到每个触发器。
**Conditions for watermarking to clean aggregation state**        

水印应当满足以下条件以清除聚合查询中的状态（从Spark 2.1开始，将来会改变）。

- **Output mode must be Append** 完成模式要求保留所有聚合数据，因此不能使用水印来删除中间状态。

- 聚合必须具有事件时列，或窗口在事件时间列上。

- *withWatermark*必须在与聚合中使用的时间戳列相同的列上调用。 例如，df.withWatermark("time"，"1 min").groupBy("time2").count()在Append输出模式中无效，因为水印定义在与聚合列不同的列上。

- 其中*withWatermark*必须在聚合之前被调用以使用水印细节。 例如，df.groupBy("time").count().withWatermark("time"，"1 min")在Append输出模式中无效。

##### Join Operations
流式的DataFrame可以和静态的DataFrame进行join操作，得到新的DataFrame，下面是例子。

	Dataset<Row> staticDf = spark.read. ...;
	Dataset<Row> streamingDf = spark.readStream. ...;
	streamingDf.join(staticDf, "type");         // inner equi-join with a static DF
	streamingDf.join(staticDf, "type", "right_join");  // right outer join with a static DF

##### Unsupported Operations
但是，请注意，所有适用于静态DataFrames / Datasets的操作在流式DataFrames /数据集中不受支持。 虽然这些不支持的操作中的一些将在未来的Spark版本中得到支持，但还有一些基本上难以有效地在流数据上实现。 例如，输入流数据集不支持排序，因为它需要跟踪流中接收的所有数据。 因此，这在根本上难以有效地执行。 从Spark 2.0开始，一些不受支持的操作如下

- 在流数据集上还不支持多个流聚集。

- 在流数据集上不支持Limit和获取前N行。 

- 不支持对流数据集进行去重操作。

- 基于流数据集的，排序操作仅在聚合之后并在完成输出模式下。

- 在流式数据和静态Datasets数据之间进行Outer joins是部分支持的。
	
	* 一个流式数据集进行Full outer join是不支持的。
	* 一个流式数据集在右边进行Left outer join是不支持的。
	* 一个流式数据级在左边进行Right outer join是不支持的。

- 尚不支持两个流数据集之间的任何类型的连接。

此外，还有一些Dataset方法不能用于流数据集。 它们是将立即运行查询并返回结果的操作，这对流数据集没有意义。 相反，这些功能可以通过显式地启动流查询来完成（参见关于该查询的下一部分）。 

- count() 不能从一个流式数据集返回一个单一的count结果，取而代之的是ds.grouBy.count(),其返回一个流式数据集，会包含一个运行的count结果。

- foreach() 取而代之的是ds.writeStream.foreach(...)

- show() 取而代之的是console sink 

### Starting Streaming Queries
一旦定义了最终结果DataFrame / Dataset，剩下的就是启动流计算。 为此，您必须使用通过Dataset.writeStream()返回的DataStreamWriter。 您必须在此界面中指定以下一个或多个。

- 输出接收器的详细信息：数据格式，位置等

- 输出模式：指定写入输出接收器的内容。

- 查询名称：（可选）指定查询的唯一名称以进行标识

- rigger interval：可选，指定触发间隔。 如果未指定，系统将在上一个处理完成后立即检查新数据的可用性。 如果由于先前处理尚未完成而错过触发时间，则系统将尝试在下一个触发点处触发，而不是在处理完成之后立即触发。

- 检查点位置：对于可以保证端到端容错的某些输出接收器，请指定系统将写入所有检查点信息的位置。 这应该是HDFS兼容容错文件系统中的目录。 检查点的语义将在下一节中更详细地讨论。

#### Output Modes
下面是几种output模式。

- 追加模式（默认） - 这是默认模式，其中只有自上次触发后添加到结果表中的新行将输出到接收器。 这只支持那些添加到结果表中的行从不会更改的查询。 因此，该模式保证每行仅输出一次（假设容错宿）。 例如，只有select，where，map，flatMap，filter，join等的查询将支持Append模式。

- 完成模式 - 每次触发后，整个结果表将输出到接收器。 聚合查询支持此选项。

- 更新模式 - （在Spark 2.1中不可用）只有结果表中自上次触发后更新的行才会输出到接收器。 更多信息将在未来版本中添加。

不同类型的流查询支持不同的输出模式。 这里是兼容性矩阵。
<table>
	<thead>
		<tr>
			<th>Query Type</th>
			<th>Supported Output Modes</th>
			<th>Notes</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>Queries without aggregation</td>
			<td>Append</td>
			<td>支持完整模式注释，因为不可能将所有数据保存在结果表中。</td>
		</tr>
		<tr>
			<td>Queries with aggregation 
			Aggregation on event-time with watermark</td>
			<td>Append, Complete</td>
			<td>追加模式使用水印来删除旧的聚合状态。 但是窗口化聚合的输出被延迟了在`withWatermark()`中指定的晚期阈值，如模式语义，在结束表之后，只有在它们被定义（即，在水印被穿过之后）时，行才能被添加一次。 有关详细信息，请参阅延迟数据部分。
	完成模式不会删除旧的聚合状态，因为根据定义，此模式保留结果表中的所有数据。</td>
		</tr>
		<tr>
			<td>Other aggregations	</td>
			<td>Complete</td>
			<td>不支持追加模式，因为聚合可以更新，因此违反了该模式的语义。
	完成模式不会删除旧的聚合状态，因为根据定义，此模式保留结果表中的所有数据。</td>
		</tr>
	</tbody>
</table>

#### Output Sinks
有几种类型的内置输出接收器。

- 文件接收器 - 将输出存储到目录。

- Foreach sink - 对输出中的记录运行任意计算。 有关更多详细信息，请参阅后面的部分。

- 控制台接收器（用于调试） - 每次有触发器时将输出打印到控制台/ stdout。 支持附加和完成输出模式。 这应该用于低数据量的调试目的，因为在每次触发后，整个输出被收集并存储在驱动程序的内存中。

- 内存接收器（用于调试） - 输出作为内存表存储在内存中。 支持附加和完成输出模式。 这应该用于低数据量的调试目的，因为在每次触发后，整个输出被收集并存储在驱动程序的内存中。

这里是所有接收器的表格，以及相应的设置。
<table>
	<thead>
		<tr>
			<th>Sink</th>
			<th>Supported Output Modes</th>
			<th>Usage</th>
			<th>Fault-tolerant</th>
			<th>Notes</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>File Sink</td>
			<td>Append</td>
			<td>writeStream
				.format("parquet")
				.start()</td>
			<td>Yes</td>
			<td></td>
		</tr>
		<tr>
			<td>Foreach Sink</td>
			<td>All modes</td>
			<td>writeStream
				.foreach(...)
			.start()</td>
			<td>Depends on ForeachWriter implementation</td>
			<td>支持对分区表的写入。 按时间分区可能有用。</td>
		</tr>
		<tr>
			<td>Console Sink</td>
			<td>Append, Complete</td>
			<td>writeStream
				.format("console")
				.start()</td>
			<td>No</td>
			<td>More details in the next section</td>
		</tr>
		<tr>
			<td>Memory Sink</td>
			<td>Append, Complete</td>
			<td>writeStream
				.format("memory")
				.queryName("table")
				.start()</td>
			<td>No</td>
			<td>将输出数据保存为表，用于交互式查询。 表名是查询名称。</td>
		</tr>
	</tbody>
</table>

最后，你必须调用start()才能真正开始执行查询。 这返回一个StreamingQuery对象，它是连续运行的执行的句柄。 您可以使用此对象来管理查询，我们将在下一小节中讨论。 现在，让我们通过几个例子来理解这一切。
		
	// ========== DF with no aggregations ==========
	Dataset<Row> noAggDF = deviceDataDf.select("device").where("signal > 10");
	
	// Print new data to console
	noAggDF
	  .writeStream()
	  .format("console")
	  .start();
	
	// Write new data to Parquet files
	noAggDF
	  .writeStream()
	  .parquet("path/to/destination/directory")
	  .start();
	   
	// ========== DF with aggregation ==========
	Dataset<Row> aggDF = df.groupBy("device").count();
	
	// Print updated aggregations to console
	aggDF
	  .writeStream()
	  .outputMode("complete")
	  .format("console")
	  .start();
	
	// Have all the aggregates in an in-memory table 
	aggDF
	  .writeStream()
	  .queryName("aggregates")    // this query name will be the table name
	  .outputMode("complete")
	  .format("memory")
	  .start();
	
	spark.sql("select * from aggregates").show();   // interactively query in-memory table

最后，你必须调用start()才能真正开始执行查询。 这返回一个StreamingQuery对象，它是连续运行的执行的句柄。 您可以使用此对象来管理查询，我们将在下一小节中讨论。 现在，让我们通过几个例子来理解这一切。
	
	// ========== DF with no aggregations ==========
	Dataset<Row> noAggDF = deviceDataDf.select("device").where("signal > 10");
	
	// Print new data to console
	noAggDF
	  .writeStream()
	  .format("console")
	  .start();
	
	// Write new data to Parquet files
	noAggDF
	  .writeStream()
	  .parquet("path/to/destination/directory")
	  .start();
	   
	// ========== DF with aggregation ==========
	Dataset<Row> aggDF = df.groupBy("device").count();
	
	// Print updated aggregations to console
	aggDF
	  .writeStream()
	  .outputMode("complete")
	  .format("console")
	  .start();
	
	// Have all the aggregates in an in-memory table 
	aggDF
	  .writeStream()
	  .queryName("aggregates")    // this query name will be the table name
	  .outputMode("complete")
	  .format("memory")
	  .start();
	
	spark.sql("select * from aggregates").show();   // interactively query in-memory table

#### Using Foreach
foreach操作允许对输出数据计算任意操作。从Spark 2.1开始，这仅适用于Scala和Java。要使用它，你必须实现接口ForeachWriter（Scala / Java docs），它有一个方法，当触发后产生一系列的行作为输出时被调用。请注意以下要点。

- 编写器必须是可序列化的，因为它将被序列化并发送到执行器以执行。

- 所有三个方法，打开，处理和关闭将被调用的执行者。

- 只有在调用open方法时，写程序必须执行所有的初始化（例如打开连接，启动事务等）。请注意，如果在创建对象时在类中有任何初始化，那么初始化将在驱动程序中进行（因为这是创建实例的地方），这可能不是您想要的。

- 版本和分区是open中的两个参数，它们唯一地表示需要被推出的一组行。版本是一个单调递增的id，随着每个触发器增加。 partition是表示输出的分区的id，因为输出是分布式的，并且将在多个执行器上处理。

- 打开可以使用版本和分区来选择是否需要写行序列。因此，它可以返回true（继续写入）或false（不需要写入）。如果返回false，那么将不会在任何行上调用进程。例如，在部分故障之后，失败触发器的一些输出分区可能已经被提交到数据库。基于存储在数据库中的元数据，写者可以识别已经提交的分区，因此返回false以跳过再次提交它们。

- 每当调用open时，也将调用close（除非JVM由于某些错误而退出）。即使open返回false，也是如此。如果在处理和写入数据时出现任何错误，将使用错误调用close。您有责任清除在开放中创建的状态（例如连接，事务等），以便没有资源泄漏。

### Managing Streaming Queries
启动查询时创建的StreamingQuery对象可用于监视和管理查询。

	StreamingQuery query = df.writeStream().format("console").start();   // get the query object

	query.id();          // get the unique identifier of the running query
	
	query.name();        // get the name of the auto-generated or user-specified name
	
	query.explain();   // print detailed explanations of the query
	
	query.stop();      // stop the query 
	
	query.awaitTermination();   // block until query is terminated, with stop() or with error
	
	query.exception();    // the exception if the query has been terminated with error
	
	query.sourceStatus();  // progress information about data has been read from the input sources
	
	query.sinkStatus();   // progress information about data written to the output sink

您可以在单个SparkSession中启动任意数量的查询。 他们都将同时运行共享集群资源。 您可以使用sparkSession.streams()获取可用于管理当前活动查询的StreamingQueryManager（Scala / Java / Python文档）。
	
	SparkSession spark = ...

	spark.streams().active();    // get the list of currently active streaming queries
	
	spark.streams().get(id);   // get a query object by its unique id
	
	spark.streams().awaitAnyTermination();   // block until any one of them terminates

### Monitoring Streaming Queries
有两个API用于以交互式和异步方式监视和调试活动的查询。
#### Interactive APIs
您可以使用streamingQuery.lastProgress()和streamingQuery.status()直接获取活动查询的当前状态和指标。 lastProgress()在Scala和Java中返回一个StreamingQueryProgress对象，在Python中返回一个具有相同字段的字典。 它具有关于在流的最后触发中所进行的进展的所有信息 - 什么数据被处理，什么是处理速率，等待时间等。还有streamingQuery.recentProgress，其返回最后几个进度的数组。

此外，streamingQuery.status()在Scala和Java中返回StreamingQueryStatus对象，在Python中返回具有相同字段的字典。 它提供有关查询立即执行的操作的信息 - 是触发器活动，正在处理的数据等。

这里有几个例子。
	
	StreamingQuery query = ...

	System.out.println(query.lastProgress());
	/* Will print something like the following.
	
	{
	  "id" : "ce011fdc-8762-4dcb-84eb-a77333e28109",
	  "runId" : "88e2ff94-ede0-45a8-b687-6316fbef529a",
	  "name" : "MyQuery",
	  "timestamp" : "2016-12-14T18:45:24.873Z",
	  "numInputRows" : 10,
	  "inputRowsPerSecond" : 120.0,
	  "processedRowsPerSecond" : 200.0,
	  "durationMs" : {
	    "triggerExecution" : 3,
	    "getOffset" : 2
	  },
	  "eventTime" : {
	    "watermark" : "2016-12-14T18:45:24.873Z"
	  },
	  "stateOperators" : [ ],
	  "sources" : [ {
	    "description" : "KafkaSource[Subscribe[topic-0]]",
	    "startOffset" : {
	      "topic-0" : {
	        "2" : 0,
	        "4" : 1,
	        "1" : 1,
	        "3" : 1,
	        "0" : 1
	      }
	    },
	    "endOffset" : {
	      "topic-0" : {
	        "2" : 0,
	        "4" : 115,
	        "1" : 134,
	        "3" : 21,
	        "0" : 534
	      }
	    },
	    "numInputRows" : 10,
	    "inputRowsPerSecond" : 120.0,
	    "processedRowsPerSecond" : 200.0
	  } ],
	  "sink" : {
	    "description" : "MemorySink"
	  }
	}
	*/
	
	
	System.out.println(query.status());
	/*  Will print something like the following.
	{
	  "message" : "Waiting for data to arrive",
	  "isDataAvailable" : false,
	  "isTriggerActive" : false
	}
	*/

#### Asynchronous API
您还可以通过附加StreamingQueryListener（Scala / Java docs）异步监视与SparkSession相关联的所有查询。 使用sparkSession.streams.attachListener（）附加自定义StreamingQueryListener对象后，当查询启动和停止以及活动查询中存在进度时，您将获得回调。 这里是一个例子，
	
	SparkSession spark = ...

	spark.streams.addListener(new StreamingQueryListener() {
	    @Overrides void onQueryStarted(QueryStartedEvent queryStarted) {
	        System.out.println("Query started: " + queryStarted.id());
	    }
	    @Overrides void onQueryTerminated(QueryTerminatedEvent queryTerminated) {
	        System.out.println("Query terminated: " + queryTerminated.id());
	    }
	    @Overrides void onQueryProgress(QueryProgressEvent queryProgress) {
	        System.out.println("Query made progress: " + queryProgress.progress());
	    }
	});

### Recovering from Failures with Checkpointing
在故障或故意关闭的情况下，您可以恢复先前查询的先前进度和状态，并继续在其停止的地方。 这是通过使用检查点和预写日志来完成的。 您可以配置具有检查点位置的查询，并且查询将保存所有进度信息（即在每个触发器中处理的偏移范围）和正在运行的聚合（例如快速示例中的字计数）到检查点位置。 此检查点位置必须是HDFS兼容文件系统中的路径，并且可以在启动查询时在DataStreamWriter中设置为选项。

	aggDF
	  .writeStream()
	  .outputMode("complete")
	  .option("checkpointLocation", "path/to/HDFS/dir")
	  .format("memory")
	  .start();

