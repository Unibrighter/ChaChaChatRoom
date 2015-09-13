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
