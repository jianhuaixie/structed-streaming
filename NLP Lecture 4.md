#<center>NLP Lecture 4 (cs224d)</center>
### 主要内容：window classification和Neuron Networks介绍

## Refresher:Classification setup and notation
通常，我们有一个训练数据集，包括下面的组成：
<center>![](http://s12.sinaimg.cn/middle/002RSgYjzy78bzpNDmP8b&690)</center>
	
	Xi表示输入，可以是单词，短语，向量，上下文窗口，句子，文档等。
	Yi表示我们需要预测的标签，可以是情感，其他单词，命名实体等，
	可以是买卖决定，后面要介绍到的多个单词的序列

### Example
The large context you get，the more order of the words you ignore.The less you know whether that word was actually in a position of a adv aj or noun.
意思：你的context选取的越大，越多的单词顺序就会被忽略，越不可能知道这个单词是adj，adv还是noun。打个比方，人看一段话，如果这段话越长，其中的单词顺序也就没那么重要，甚至是错乱一两个单词的顺序也不影响整段话的意思，而其中的一两个单词的具体拼写也没那么重要，即便错了一两个也不影响整段话的意思。

### Classification intuition
简单的分类方法：softmax进行分类
<center>![](http://s9.sinaimg.cn/middle/002RSgYjzy78bBeRGdi38&690)</center>
	
	W是softmax的权重向量，分子的y代表ground truth的index，分母是将所有可能的class的值相加，最后求一个概率。

<center>![](http://s4.sinaimg.cn/middle/002RSgYjzy78bBeRMtB43&690)</center>

	使用了极大似然估计，就是假设所有事件发生时IID的，然后这些事件同时发生的概率就是他们各自概率的乘积，目标就是使这个概率最大的
	参数，这里取log然后求和，最后得到其最小化的权重矩阵。

#### Classification:Regularization
正则化，约束参数用的，主要是为了防止出现overfitting（过拟合）。

### Concept
- Word vector matrix L is also called lookup table.
- Word vectors = word embeddings = word representations

### Classification difference with word vectors
通常，机器学习的θ只是一组参数W：
<center>![](http://s9.sinaimg.cn/middle/002RSgYjzy78bDHMMKA18&690)</center>
只需要更新其决策边界：
<center>![](http://s11.sinaimg.cn/middle/002RSgYjzy78bDHPqqeba&690)</center>
对于深度学习，需要学习W和word vectors x：
<center>![](http://s8.sinaimg.cn/middle/002RSgYjzy78bDHNDddd7&690)</center>

### Loosing generalization by re-training word vectors
If you only have a small training data set,don't train the word vectors.

## Window Classification
如果不用window classification，就容易出现ambiguity这种问题。

window classification如何实现呢？Instead of classifying a single word，just classify a word together with its context window of neighboring words。给center word定义一个label然后连接所有他周围的word vector使其形成一个更长的vector。

然后怎么进行window classification呢？还是使用softmax，只是word vector不再仅仅是center word vector，并且是concatenating all word vectors surrounding it。

## Basic neural networks
- A single neuron简单的说就是多个softmax的组合。
- 多加几个out layers就使得结构更复杂，能力也更强。
- 再增加一个或多个hidden layers 就更666了。

### Intuition of back-propagation
BP的内容，网上一搜一大堆。
The procedure repeatedly adjusts the weights of the connections in the networks so as to minimize a measure of the difference between the actual output vector of the net and the desired output vector.

Connections within a layer of from higher to lower layers are forbidden,but connections can skip intermediate layers.

