#<center>NLP Lecture 2 (cs224d)</center>
### 主要内容：如何用数字语言表达一个自然词（自然语言中的一个词）

## WordNet
用一个包含了上位词关系和同义词集合的词汇网络来做分类，将自然词进行分开表示。这样的分布式表达方式是存在巨大缺陷的。很明显，同义词之间缺乏一些细微的区别，没有办法跟进一些新出现的词，而且比较主观，需要花费精力去创造调优，比较难以精准计算词的相似度。

这种类似于OHE（One-hot-encoder）的表达方式来表达一个词，代价是很明显的，单词有多少，向量就要有多长，整个向量只有一个位置是1，其他都是0。

## Distributional similarity based representations
要理解一个单词的语义，很明显需要上下文，简单点的，至少需要通过这个词的周围的单词组合成的语义来帮助理解吧。

如何去让neighbors来帮助表达这个单词的语义呢？答案就是co-occurrence matrix（共生矩阵）。

使用共生矩阵表示单词，有两种表示法。

- 第一种是用word-document（行代表word，列代表document）的方法，这种方法比较容易学习到单词的general topics，能学习到其隐含的语义。
- 第二种是用word-windows（行代表word，列代表windows）的方法，这种方法比较容易学习到单词的Syntatic and semantic information。这个windows的选择一般是5-10之间。

使用共生矩阵来表达一个单词，也是有一些问题的。

- 比如随着单词的增加，矩阵的也是急速增大。
- 维度太高（和单词数目一致），需要的存储量很大。
- 接下来就是做分类模型时会有矩阵稀疏性问题。
- 模型缺少鲁棒性。

但是办法总是比问题多，解决办法就是用更低的维度向量来表示单词。

### Low dimensional vectors
其基本思路是：将一些常用的重要的词做一个稠密向量，但是如何将高纬度的矩阵降维呢，SVD登场了。
经过SVD处理后，所有后续的模型，包括深度学习模型，一个单词就用一个稠密向量来表示了。

### Hacks to Co-occurrence matrix

- 功能性词太多，比如the,is,has等。有两个解决办法，min(CM,t),t是一个设定的阀值，另外一个就是把这些功能性词全部忽略掉。
- 建立共生矩阵不是那么合理，距离单词越近的词和这个词的相关性越高，现实中并不是如此嘛，住得近的邻居关系就一定是最好的？当然也有解决办法，使用一个Ramped windows（斜的窗）来建立共生矩阵，类似于用一个weight乘count，距离越远weight越小反之越大（weight是一个钟形函数）。
- 用皮尔逊相关系数来替代counts，将negative values设为0。

### Interesting semantic patterns emerge in the vectors

- Hierachical Clustering来聚类行向量可以进行同义词聚类。
- 同义的单词在word-spaces里，同义词靠的近，不同意思的词靠的远。
- 相似的semantic pair word之间的向量距离也类似。、

### problems with SVD

- 计算量巨大。
- SVD是一次计算好的，不容易添加新的单词和document到矩阵。
- 其和DL模型不是那么默契，但是办法总是比问题多嘛，word2vec可以走上历史舞台了。

## Word2vec
### Main idea of word2vec
word2vec和之前的方法不同，共生矩阵是一次性计算完的，word2vec建立模型是慢慢的一个单词一个单词进来。首先设定init parameter，然后根据这个parameter预测输入进来的word的context，然后如果context和ground truth类似就不变，如果不类似就惩罚参数，也就是调整参数。

### Details of word2vec
对于一个单词，在其窗口（长度为c）预测其周围的词，需要尽可能预测准确，在数学上的表达就是如下：

<center>![](http://s3.sinaimg.cn/middle/002RSgYjzy7886oozsu72&690)</center>

	T代表windows的个数，c代表context的wide，p使用softmax计算。
这就是目标函数，最大化这个对数概率。对于上面这个公式，还是不够具体，对于log里面的p(**w**t+j | **w**t)，其有一个最简单的表达公式：

<center>![](http://s1.sinaimg.cn/middle/002RSgYjzy788g84yfSc0&690)</center>
	
	其中v和v'分别是输入和输出向量代表w，也就是说每个单词都有两个向量。
	本质上来说，这是一个动态的逻辑回归。

对目标函数进行拉格朗日转换后，得到损失函数，然后用梯度下降进行优化得到模型的参数。

### GloVe：Combining the best of both worlds

- 快速训练
- 扩展到巨型语料库
- 对于小语料库和小的向量都有非常好的表现

