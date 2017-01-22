#<center>NLP Lecture 1 (cs224d)</center>
### 主要内容：DNLP的综述

## Pre-requisites
- Python是必须要会的，其中的numpy是重点使用的工具包。
这里有一个简单的入门：[Python Numpy](http://cs231n.github.io/python-numpy-tutorial/ "numpy")
 
- 高等数学，线性代数是必须的啦。

- 概率论和统计分析也是必须滴。

- 核心来了，欲练此功，先学Machine Learning，不要来一个损失函数，求极值和梯度下降就懵逼。

## NLP是个什么鬼？
既然来了，也不能来一个通俗解释。NLP是一个典型的交叉综合学科，综合了计算机科学、人工智能和语言学。

目标就是理解人类说的自然语言，用在比如QA系统上。当然也不能强人所能，想让NLP完全能够理解表达甚至是定义语言是一个虚幻的目标，足够好的语言理解是我们的目标。

## NLP Levels是什么鬼？
类似于NLP的Pipeline。

- 将语言输入计算机，人类语言主要是从两个渠道获得，一个是Speech，一个是Text，首先通过Morphological Analysis(词态分析)。Speech通过音频处理成文本再到计算机理解的词态向量，Text到词态向量。

- 然后是紧接着语法分析，然后是语义解释，最后就是将语义表述出来的处理过程。

## NLP 能做什么鬼？
- NLP能做的还是比较多的，说来简单，比如拼写检查，关键词搜索，寻找同义词。

- 稍微复杂点，比如从一堆文字信息中抽取出产品价格，日期，地址，人名或者公司名。

- 当然，做语义情感理解是必须so easy的，比如电影正面负面评价的情感理解。

- 重点来了，机器翻译用深度学习的NLP取得了不俗的成绩哦。

- 让人兴奋的，做个Siri还不是小case滴。

- 工业界用来做QA系统也是可以的。

### NLP在工业界的应用
搜索、在线广告、自动翻译和辅助翻译，金融和交易市场的语义分析，语音识别等。

### 为什么NPL比较难搞？
说起来，语言不仅仅是词语组合起来那么简单，而是语句，环境，单词，视觉知识等的组合，这么高纬度的表征，想要用神经网络学起来，也不是那么容易的。


## 先了解下Deep Learning（DL）
深度学习是机器学习的一个分支。大部分机器学习的方法之所以有效果，是因为有人精心设计了特征，有这些特征来训练模型，最后能做出很好的预测。

比起机器学习，深度学习不需要人为构造特征（特征工程），其自身就可以学习出特征，然后结合机器学习做预测或分类等。从而实现端到端的学习，想想是不是挺完美的。

深度学习的历史：[Deep learning history](http://www.geetest.com/blog/0431609d3d965f9a)


### 为什么还要搞深度学习
既然已经有了机器学习，还要搞这么难搞的深度学习是为了啥！

- 手动构造特征经常容易出现过度简化，损失信息，不完整的表达事物，同时，特征工程需要花费大量的精力去做设计和验证。

- 深度学习提供了一个非常灵活的，统一的，能够自学的框架来做自然语言，视觉图像等方面的工作。

- 深度学习能做无监督和有监督的学习，就是有label和无label都能搞。

- 深度学习的效果比机器学习更加优秀，当然成本也更大。

- 深度学习之所以搞起来取得突破，其实跟三方面的原因分不开：互联网产生了更多的数据，更快的GPU运算，新的算法。当然首先取得飞跃的是在视觉图像和语音方面，然后才是NLP领域，行业也算有一个共识吧：那就是图像和音频数据，其中的信息，维度还没有那么高，自然语言，经过上万年的演化，有些已经非常抽象，一个单词整合了太多时空元素，神经网络要取得更大的突破，达到飞跃还需要一个过程。

## DNLP
Deep Learning + NLP = Deep NLP（DNLP）

结合NLP和深度学习去解决问题是大的方向。

#### NLP其中的一个代表：Phonology（语音体系）
根据听到的语音特征，将其表达为一个向量，然后神经网络去预测出一个phonemes（语音元素）。
#### NLP其中的一个代表：Morphology（词态体系）
在深度学习中，任何一个词元素都是一个向量，将多个词元素组合能得到一个新的词。
#### NLP其中的一个代表：Syntax（语法体系）
在深度学习中，任何一个单词或者短语都是一个向量，将多个单词或短语组合得到一个新的短语或者句子。
#### NLP其中的一个代表：Semantics（语义体系）
在深度学习中，每一个单词，一个短语或者一段表达都是一个向量，多个向量组合得到一个新的表达式。

#### 学习路径

- 高等数学

- 线性代数

- 凸优化

- 梯度下降