# <center>Lambda Architecture</center>
## Question
 - not enough accuracy of prediction
 - complex data input
 - real-time is not supported
## Solution
 - need Speed Layer for model optimization
 - unite data input include static data and streaming input
 - introduce streaming processing flatform

### Introduce Lambda Architecture
Lambda Architecture的目标是满足实时大数据处理能力的架构，要能实现高容错、低延时和可扩展等。核心就是整合离线计算和实时计算，其融合了数据不可变性，复杂性隔离的原则。
### Architecture
Lambda Architecture划分为三层，Batch Layer、Serving Layer和Speed Layer。

 - **Batch Layer**	批处理可以采用Spark和Hadoop来实现，实现数据的存储和产生视图数据。此处的计算需要数个小时。
 - **Serving Layer** 提供统一被查询的接口，需要实现展示层，可以让业务部门实时掌握模型的不足点。
 - **Speed Layer** 加速层就是为优化模型而设计，当预测覆盖率不够时，不断将新的未覆盖到的数据进行优化后，增量计算后优化模型，提高覆盖率。
![](http://lambda-architecture.net/img/la-overview_small.png)

## How to do it
### Batch Layer
数据的存储，采用Sqoop不断将关系型数据库的数据增量抽取到数据仓库中。计算框架采用Spark，替代Hadoop的MR。调度系统Oozie定时调度ETL和模型训练等工作。
### Serving Layer
采用MySQL + Zeppelin的组合，实现视图的存储和展现，方便业务人员对异常数据进行操作。
### Speed Layer
重点中的重点，采用Kafka + Spark Streaming + MySQL的组合，需要预测的数据通过Kafka进入加速层，将实时进行预测的数据不断写入MySQL数据库，预测准确概率超过阀值的直接追加到数据仓库，否则进入MySQL数据库，服务层就可以实时知道哪些数据需要人为判断或者更新。


## Key point of optimization for randomforest model
- 太多的分类类别，数据量和硬件无法支持一个有足够表达力的模型去进行分类，采用分而治之的办法，将数据首先按照brand进行分组，然后在组内训练一个进行分类的随机森林模型。部分解决了准确率的问题。

- 准确率和覆盖率是一个矛盾，需要确保准确率，就需要降低覆盖率，引入Speed Layer可以不断将未覆盖到的数据，通过人工处理后加入模型的训练数据源，这样可以不断提高覆盖率。