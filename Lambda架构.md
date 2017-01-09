# <center>Lambda架构</center>
###概述
Lambda架构的目标是设计出一个能满足实时大数据系统关键特性的架构，包括有：高容错、低延时和可扩展等。Lambda架构整合离线计算和实时计算，融合不可变性（Immunability），读写分离和复杂性隔离等一系列架构原则，可集成Hadoop，Kafka，Storm，Spark，Hbase等各类大数据组件。

###架构
Lambda架构划分为三层，分别是批处理层，服务层，和加速层。最终实现的效果，可以使用下面的表达式来说明。
	
	query = function(alldata)

![](http://www.sentric.ch/wp-content/uploads/2013/03/Lambda_Arch2-613x488.jpg)

Lambda基于下列原则：

* 1.人为容错性human fault-tolerance – 系统容易数据丢失或数据损坏，大规模时可能是不可挽回的。

* 2.数据不可变性data immutability – 数据存储在它的最原始的形式不变的，永久的。

* 3.重新计算recomputation – 因为上面两个原则，运行函数重新计算结果是可能的。

### 批处理层(Batch Layer, Apache Hadoop)
  批处理层主用由Hadoop来实现，负责数据的存储和产生任意的视图数据。计算视图数据是一个连续的操作，因此，当新数据到达时，使用MapReduce迭代地将数据聚集到视图中。 将数据集中计算得到的视图，这使得它不会被频繁地更新。根据你的数据集的大小和集群的规模，任何迭代转换计算的时间大约需要几小时。

### 服务层(Serving layer ,Cloudera Impala)
服务层是由Cloudera Impala框架来实现的，整体而言，使用了Impala的主要特性。从批处理输出的是一系列包含预计算视图的原始文件，服务层负责建立索引和呈现视图，以便于它们能够被很好被查询到。

由于批处理视图是静态的，服务层仅仅需要提供批量地更新和随机读，而Cloudera Impala正好符合我们的要求。为了使用Impala呈现视图，所有的服务层就是在Hive元数据中创建一个表，这些元数据都指向HDFS中的文件。随后，用户立刻能够使用Impala查询到视图。

Hadoop和Impala是批处理层和服务层极好的工具。Hadoop能够存储和处理千兆字节（petabytes）数据，而Impala能够查询快速且交互地查询到这个数据。可是，批处理和服务层单独存在，无法满足实时性需求。原因是MapReduce在设计上存在很高的延迟，它需要花费几小时的时间来将新数据展现给视图，然后通过媒介传递给服务层。这就是为什么我们需要加速层的原因。

### 加速层 (Speed layer, Storm, Apache HBase)
 在本质上，加速层与批处理层是一样的，都是从它接受到的数据上计算而得到视图。加速层就是为了弥补批处理层的高延迟性问题，它通过Strom框架计算实时视图来解决这个问题。实时视图仅仅包含数据结果去供应批处理视图。同时，批处理的设计就是连续重复从获取的数据中计算批处理视图，而加速层使用的是增量模型，这是鉴于实时视图是增量的。加速层的高明之处在于实时视图作为临时量，只要数据传播到批处理中，服务层中相应的实时视图结果就会被丢掉。这个被称作为“完全隔离”，意味着架构中的复杂部分被推送到结构层次中，而结构层的结果为临时的，大大方便了连续处理视图。

 令人疑惑的那部分就是呈现实时视图，以便于它们能够被查询到，以及使用批处理视图合并来获得全部的结果。由于实时视图是增量的，加速层需要同时随机的读和写。为此，我将使用Apache Hbase数据库。HBase提供了对Storm连续地增量化实时视图的能力，同时，为Impala提供查询经批处理视图合并后得到的结果。Impala查询存储在HDFS中批处理视图和存储在HBase中的实时视图，这使得Impala成为相当完美的工具。

这里的速度层主要处理新数据和服务层更新造成的高延迟补偿，利用流处理系统（Storm，Spark）和随机read/write数据存储库来计算实时视图（HBase)，这些视图有效期一直到它们能通过批处理和服务层获得时为止。

为了获得一个完成结果，批处理和实时视图都必须被同时查询和融合（实时代表新数据）。

### 预计算 (Batch Layer来完成)
理想状态下，任何数据访问都可以从表达式Query= function(alldata)开始，但是，若数据达到相当大的一个级别（例如PB），且还需要支持实时查询时，就需要耗费非常庞大的资源。一个解决方式是预运算查询函数（precomputedquery funciton）。书中将这种预运算查询函数称之为Batch View(A)，这样当需要执行查询时，可以从BatchView中读取结果。这样一个预先运算好的View是可以建立索引的，因而可以支持随机读取(B)。于是系统就变成：

	(A)batchview = function(all data)
	(B)query = function(batch view)

### View
View是一个和业务关联性比较大的概念，View的创建需要从业务自身的需求出发。一个通用的数据库查询系统，查询对应的函数千变万化，不可能穷举。但是如果从业务自身的需求出发，可以发现业务所需要的查询常常是有限的。BatchLayer需要做的一件重要的工作就是根据业务的需求，考察可能需要的各种查询，根据查询定义其在数据集上对应的Views。

### Speed Layer
SpeedLayer可以总结为：

	（C）RealtimeView＝function(RealtimeView，newdata)

### Lambda Architecture 优点

- 容错性：SpeedLayer中处理的数据不断写入BatchLayer，当BatchLayer中重新计算数据集包含SpeedLayer处理的数据集后，当前的RealtimeView就可以丢弃，这就意味着SpeedLayer处理中引入的错误，在BatchLayer重新计算时都可以得到修证。这点也可以看成时CAP理论中的最终一致性（EventualConsistency）的体现。

- 复杂性隔离。BatchLayer处理的是离线数据，可以很好的掌控。Speed Layer采用增量算法处理实时数据，复杂性比Batch Layer要高很多。通过分开BatchLayer和Speed Layer，把复杂性隔离到Speed Layer，可以很好的提高整个系统的鲁棒性和可靠性。

### Serving Layer
Serving Layer的职责包含：

- 对batchView和RealTimeView的随机访问

- 更新BatchVeiw和RealTimeView，并负责结合两者的数据，对用户提供统一的接口

综上所诉，Serving Layer采用如下等式表示：

	（D）Query＝function(BatchViews，RealtimeView)

