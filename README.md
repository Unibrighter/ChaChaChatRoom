# COMP90015_ChatRoom

==========================
This project uses some basic features of JDK to implement a Simple Multi-User ChatRoom

# Addtional Docs 

==========================
See :

https://github.com/Unibrighter/COMP90015_ChatRoom/raw/master/Distributed%20Systems_COMP90015%202015%20SM2_Project%201%20-%20Chat%20System.pdf

for more information.

##手札
1. 能够接受多个来自不同client的TCP请求
2. 服务器端要能够保有一个chat rooms list
3. client能够通过向服务器发送特殊的json请求而 实现更换聊天室的的请求
4. 服务器能够向各个对应的client发送json,使得client做出相应的反应
5. 一个新的连接socket建立的时候,server根据生成方法得到一个new indentity并通过 json通知客户端
6. 接着将客户放入mainhall(同样通过json完成)
7. 客户端对server发送请求,同时接受消息(json通讯  while循环)
8. client主动断开,则发送一条对应的quit json给服务器
9. 客户端可以发送一条type为 identitychange的消息,对自己的id进行修改;服务器的响应为:

>1.若无效id或者新id已被别人占用,则返回 former和identity一致的回复 =>在客户端需要做" former=identity then the client outputs "Requested identity invalid or in use"的解析
>2.若有效,则对所有的socket client告知这条新消息

10. 类似的换房间,没有成功则保持原状态,否则则通知"现在的room所有client","请求的room所有client"有关roomchangge
11. 如果来到的是mainhall,还要接着告知roomcontents 和一个roomlist

=================================
>如此看来,服务器端是重头戏,因为json相应的解析与响应都是在服务器端完成的...
>另外一点,要使用一定的的数据结构将各个不同的thread,客户端,以及聊天室管理起来
>
>现在暂时有两种思路:

###I.	将每一个聊天室设置为一个ChatRoomManager?(vector)来形成每一个chatroom进行独立的维护管理
好处是新建一个类,可以通过类方法对"整体聊天人数"进行管理

###II.	将每个客户端设置一个Tag(内置属性),依照这个属性来对各个不同的连接(socket)进行管理,这样将消息的显示与否放在客户端来决定
好处是不用过多的在意管理数据结构

个人认为可以综合上面的考虑
每一个ChatRoomManager内部有一个线程池(不好,因为涉及到聊天室转换的时候,调度容易出差错)
每一个ChatRoomManager只是针对一个Socket Connecttion 初始化得到的Thread进行管理,这样维护的是独立的ClientThread,ClientThread可以自由添加


============================================
对于ServerMain,肯定有一部分的方法是synchronized的
比如setupChatRoom等方法等



对于ChatRoomManager
现在的思路是:
一个Vector<ClientWrap>

一个线程while(true)
不断从各个的ClientWrap中吮吸msg
然后发出去

但是这又引出了另外一个问题:
**当一个inputStream的readLine方法阻塞了以后那么while循环就卡住了,怎么解决?**

暂时想到的一个比较糟糕的解决办法是,将Client设置为一个Runnable的实例
然后私有属性中添加ChatRoomManager中对整个list的引用(更确切的说是指这个广播频道)
将其视作一个必须被同步控制的变量.


=============================================
虽然simple json是支持序列化的(也即其能够通过使用ObjectStream进行传输)
但是我还是决定使用String来进行传输
原因有两点:
1. 如果使用String传输,比较容易添加"\n"换行符作为结尾,这样能够更好的对TCP/IP流进行以行的形式进行读取
2. String形成json再来获取变量也很方便,直接传输JsonObject反而违反了json简单快捷,能够用String表达的初衷.json的设计本来就是让其以文本的形式表现一个Object

=============================================
现在又有另一个问题:
实际响应由客户端发出包含命令的json请求时,
比如一个**"新建一个聊天室"**这个命令,
这个添加一个新的ChatRoomManager到ChatServer的Vector中的这一部分代码应该放在哪一个部分中?

在ClientWrap中?
还是ChatServer中?
似乎都不太合适

另外,操作的句柄应该是什么?

------------
以上的问题,我现在还只能思考到一个耦合度非常高(所以说比较糟糕)的方案,

ServerHandler中设置全局的ChatServer作为句柄.
将每一个命令都是为ChatSever和ClientWrap之间的互动

=============================================

最后的决定是,将ServerHandler中的所有方法全部放入ClientWrap中,以client调用自己自身的方法的形式来实现和服务器的对话!

==============================================
报告中可以谈一谈在服务器端和客户端都对数据进行正则表达式验证的考虑

- 不要信任客户端提交的数据
- 考虑在客户端尽量屏蔽掉不合法的数据,减少非法请求的压力.

=============================================
如何解决光标在输入的时候受到屏幕输出干扰的影响?(最后考虑)

=============================================
对黑名单的请求问题,从ChatroomManager到ChatRoomServer
再到最后将该数据结构放在ClientWrap中的考虑

=====
客户端响应返回roomcontent需要修改
