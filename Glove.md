#<center>Glove</center>
### Glove：Global Vectors for word representation
现在处理capture fine-grained semantic的能力和caputure syntactic的能力还不错。semantic基于term-document这样的模式效果好，syntactic基于term-document的模式效果好，但有没有一种方法能将这两个东西综合一下呢？于是Glove应用而生了，事实证明，Glove胜过了similarity tasks（相似度识别任务）和named entity recognition（命名实体识别）。

- LSA(Latent Semantic Analysis)这样的方法能高效利用统计信息，但在词类比任务，预测一个次优的向量空间结构方面表现相对差。
- SG(skip-gram)这样的方法在词类比任务方面表现比较好，但是如果语料是在单独的本地上下文窗口而不是global co-occurrence counts上训练得到的，这样的方法在这样的预料上进行统计就比较差了。

Glove的目的就是要把这两个创意结合起来。

### Improving Word Representations via Global Context and Multiple Word Prototypes

- 如何用Neural Network实现Global Context-Aware Neural Language Model（全局情境感知神经语言模型）
- 解决一词多义的问题将Multi-Prototype引入模型

![](http://img.blog.csdn.net/20150712180828277?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
The local score preserves word order and syntactic information,while the global score uses a weighted average which the global score uses a weighted average which is similar to bag-of-words features,capturing more of the semanties and topics of the document.
意思：本地的得分表征的是语序和语法信息，全局得分用了一个加权平均，类似于词袋特征，能够获取更多的语义信息和文档的主题信息。




