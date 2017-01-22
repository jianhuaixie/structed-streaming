#<center>NLP Lecture 3 (cs224d)</center>
### 主要内容：如何建立损失函数并且找到optimum point；如何评估生成的VSM。

## Refresher：word2vec model
<center>![](http://s1.sinaimg.cn/middle/002RSgYjzy788g84yfSc0&690)</center>

	公式中的参数有2d*V个，d是每个单词向量的长度，V是单词的个数，
	输入输出单词向量表示法不一样，所以是2倍的。

## Gradient Descent（GD）
对损失函数求极值，当然最容易想到的是Gradient Descent，也就是说对损失函数的theta向量求导得到最佳下降梯度，然后用原theta减去这个梯度乘以step就得到更新后的向量了。这种方法就是每次找的方向都很精准，每次都往最陡的下坡方向走，但是，但是，但是，损失函数是对所有windows进行相加，而每求一个梯度都得把所有的windows求一次和，对于很大的数据集，就不行了，SGD要来解决这个问题了。
#### Calculating all gradients
- 通过梯度下降对每一个center vector v在一个窗口计算梯度
- 同样也需要计算外部的向量v'
- 在每一个窗口都需要计算更新所有的参数

讲这么多，还是有点混乱，还不如直接来一个例子。

	sentence：“I like dog.”
	
	size c=1
	
	在第一个窗口计算梯度：internal vector V-like and 
	external vectors V'-I and V'-dog
	
	然后在另外一个窗口再次同样计算。

#### Compute all vector gradients
对损失函数求极值的所有参数，定义成一个集合，这是一个长向量**θ**，在此处的案例中，有d维的向量（d是每个单词向量的长度）和V（单词的个数）。
需要在所有batch（是全部训练数据集）的所有窗口上进行梯度计算。
对于**θ**中的每一个元素都需要更新参数。

	while True:
		theta_grad = evaluate_gradient(J,corpus,theta)
		theta = theta - alpha*theta_grad
	//alpha:step size	

## Stochastic Gradient Descent（SGD）
SGD不再对整个windows的和求梯度，而是对一个window求梯度之后就立即更新theta。这个方法收敛快，但每一次更新都是针对这一个window里的单词更新的，这意味着，一次更新最多更新2c-1个单词的参数，也就是（2c-1）*d个参数。这样的话就不必每次更新整个参数矩阵，仅仅更新window的单词在参数矩阵中对应的列就好了。这样提高了计算效率和存储效率。

	while True:
		window = sample_window(corpus)
		theta_grad = evaluate_gradient(J,window,theta)
		theta = theta - alpha*theta_grad

## The skip-gram model and negative sampling
求概率公式使用到了softmax，它的分母是对所有的words求和，vocabulary通常是很大的，计算一次的代价是很大的。基本的逻辑是softmax公式，分子需要尽量大（让正确的word脱颖而出），其他整个单词表的求和尽可能少，可以采用skip-gram和negative sampling的方法，将整个单词表的数量压缩，其他随机的word和center word组合的值越小越好，这两个方式让分母尽可能少的逻辑是一致的。
定义一个noise distribution用来选取随机的word。
<center>![](http://s1.sinaimg.cn/middle/002RSgYjzy788sMWn7ye0&690)</center>

	
	其中σ是sigmod函数，目的是要两个向量靠的更近方向更一致，
	那么sigmod函数中两个向量靠的近似值就越大，反之越小。


## The continuous bag of words model（CBWM）
CBWM类似Skip-gram的逆运算：从周围词向量的总和中预测一个center word。最终我们得到了L和L'的参数矩阵，相当于得到了这个共生矩阵里单词的信息，怎么处理这两个矩阵呢？一种办法是直接相加L_final=L+L'，另外一种方法是把两个矩阵连接起来，这样的结果就综合了输入矩阵和输出矩阵的信息。

## How to evaluate word vectors？
Intrinsic vs Extrinsic

##### Intrinsic
- 在一个特殊的/中间子任务的阶段进行评估
- 快速计算
- 帮助去理解系统
- 除非对真实任务的归一化已经完成，要不就不清楚其是否真的有用

##### Extrinsic
- 对真实任务的评估
- 花费长时间去计算精准性
- 如果子系统有问题或者其子迭代或者其他子系统是有问题的，评估也是不清楚的
- 如果能用替代子系统的方法提高精准性，那么Winning

#### Intrinsic Evaluation
Intrinsic evaluation是对VSM的一个简单迅速的评估。这种评估方法不放到整个系统中评估，而仅仅是评估一个subtask，其评估过程也会很快，可以很好的理解真个系统，但是如果不放到实际的系统中，就不知道其表现是否足够好。

- Intrinsic evaluation的第一种评估是Syntactic评估，这种评估方法问题比较少。
- 第二种是semantic评估，存在一词多义的问题，还有corpus的数据比较旧的问题，这两个问题都会影响评估效果。Glove word vector是至今Intrinsic evaluation效果最好的model，Asymmetric context只评估左边window的单词效果不好。More training time and more data对评估效果很有帮助。

#### Extrinsic Evaluation
Extrinsic Evaluation就是把VSM放到实际的任务中进行评估，花费时间较长，如果效果不好的话也不清楚是VSM的问题还是其他模块的问题或者是interaction的问题。有一个简单的办法确认是不是VSM的问题，把这个subsystem用其他subsystem替换，如果精度提高那就换上。

#### Ambiguity
面对一个单词是多个意思，肯定不能用mean vector来处理，那只是把多个意思综合起来，这显然是不可取的。

- 收集固定大小的上下文窗口出现的所有的词(例如,5个之前和5个之后的词)
- 每个上下文是由上下文词语的向量的加权平均(使用idf-weighting)
- 应用球面k-means聚类这些上下文表示。
- 最后,每个出现的词都跟其联合的聚类是绑定的，用这个聚类集合去训练这个词的语义表示。
简单的说就是使用k-means聚类将如同的context先聚类出来，再给每个centerid赋予相应的word，再把相应的context归给这个word，最后再用我们之前的普通训练方法训练，这就解决了一词多义的问题。

#### Softmax and cross entropy error
评估一个单词发生的概率，很简单也是可以用softmax，这个方法cost太大，所以用skip-gram更好一些，我们的目的是使得参数让结果向量中ground-truth位的值最大。之前我们用的是sigmod函数配合內积的方法，还有一种方法也能很好的解决这个问题，那就是cross entropy error（交叉熵损失）。cross entropy可以用来评估两个概率之间差异的大小H(p,q)。P代入ground-truth概率向量，q代入计算的概率向量，目的是使得两者差异最小也就是使H(p,q)最小，展开H(p,q)=H(p)+D_KL(p||q)。由于p本身的性质，第一项天然就会为0，不为零也没关系，因为需要改变q中的参数，p始终是fix的，不会影响计算结果。所以求H(p,q)就会等价为求D_KL(p||q)的最小值。

#### training word vectors or not？
Word vectors should not be retrained if the training data set is small.If the training set is large，retraining may improve performance.

- 第一，当dataset比较小的时候，我们进行GD进行theta的更新只能更新到dataset中的word，其他的word的向量不用被更新到，而我们的softmax weight W是根据新的dataset进行判别的，那么这样的结果就会使dataset中不包含的word的判别出问题。
- 第二，当dataset比较大的时候，假设涵盖了整个vocabulary，我们不论是进行GD更新theta还是对softmax weight W进行更新就是对整个vocabulary进行更新，不会出现遗漏的问题，而这种方法增添进来了新的dataset里的信息有可能会使精确度增加。