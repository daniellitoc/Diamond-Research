## 体系介绍

### ManagerListener：客户端监听器
    ManagerListenerAdapter: 抽象类，默认实现了获取线程池方法。
        PropertiesListener：自动将content解析为Properties。

### ConfigureInfomation：client端配置信息，传输实体类。

### SubscriberListener
    DefaultSubscriberListener：订阅器监听器，用于管理所有的监听器（添加、删除、获取、发送通知）

### LocalConfigInfoProcessor
    启动：创建指定跟目录并对目录(子目录)下的所有File添加create/delete/modify监听行为。启动新的线程，持续获取新的监听事件。

    接收到新的事件：
        获取事件下的所有影响(WatchEvent)，遍历每个WatchEvent
            如果是create/modify行为
                获取对应的影响文件，验证前两级目录是rootDir/config-data。如果是，添加文件路径和当前事件添加到existFiles中。
            如果是删除行为
                获取前两级目录，如果是rootDir/config-data，证明当前是文件，则从existFiles删除。
                否则其他情况认为是目录，遍历existFiles，如果对应的key(文件路径)以该目录开头，则删除。

    获取配置：参数（cacheDate，force）
        生成文件名：rootDir/config-data/group/dataId
        如果文件名不存在于existFiles中，并且cacheDate中的useLocalConfigInfo为true(文件被删除了)，则设置cacheDate中
            lastModifiedHeader      =   null
            md5                     =   null
            localConfigInfoFile     =   null
            localConfigInfoVersion  =   0
            useLocalConfigInfo      =   false
            返回null
        如果force为true
            获取文件内容并返回
        判断文件名是否和localConfigInfoFile不等或者版本号是否和localConfigInfoVersion不等，则设置cacheDate中
            localConfigInfoFile     =   rootDir/config-data/group/dataId
            localConfigInfoVersion  =   existFiles[localConfigInfoFile]
            useLocalConfigInfo      =   true
            返回文件内容
        否则                                                                          // 文件无变化
            useLocalConfigInfo      =   true
            返回null

    停止：停止获取新监听事件的线程

### SnapshotConfigInfoProcessor
    启动：创建指定跟目录

    保存快照：
        验证group和dataId不能为null，content如果为null，设置为""
        创建rootDir/group目录
        创建rootDir/group/dataId文件
        将content写入rootDir/group/dataId文件

    删除快照：
        如果group或dataId为null或""，退出
        如果rootDir/group目录不存在，退出
        如果rootDir/group/dataId文件不存在，退出
        删除rootDir/group/dataId文件
        如果rootDir/group目录下不存在其他文件，删除目录

    获取配置：
        如果group或dataId为null或""，返回null
        如果rootDir/group目录不存在，返回null
        如果rootDir/group/dataId文件不存在，返回null
        返回rootDir/group/dataId文件内容

### ServerAddressProcessor：用于维护DiamondConfigure的domainNameList属性
    启动：
        设置成员变量DiamondConfigure
        创建HttpClient实例
        初始化DiamondConfigure的domainNameList
            如果domainNameList为null或长度为0
                第一次调用请求服务器列表，如果返回true，写入domainNameList到{user.home}/diamond/ServerAddress，然后退出
                第二次调用请求服务器列表，如果返回true，写入domainNameList到{user.home}/diamond/ServerAddress，然后退出
                从{user.home}/diamond/ServerAddress读取服务器列表到domainNameList
                如果domainNameList为null或长度为0，抛异常
            添加定时任务，每300秒执行一次
                第一次调用请求服务器列表，如果返回true，写入domainNameList到{user.home}/diamond/ServerAddress，然后退出此次任务
                第二次调用请求服务器列表，如果返回true，写入domainNameList到{user.home}/diamond/ServerAddress，然后退出此次任务

    停止：
        关闭定时任务

    请求服务器列表：
        如果是第一次调用，设定地址为Constants.DEFAULT_DOMAINNAME，否则设置地址为Constants.DAILY_DOMAINNAME
        设置端口为Constants.DEFAULT_PORT
        使用get请求调用server:port/domains，响应结果为服务器列表，按行分隔，将结果处理为List<String>，添加到domainNameList，返回true
        如果响应结果非200或者响应中不包含可用的服务器列表，返回false

### DiamondSubscriber
    DefaultDiamondSubscriber

        ConcurrentHashMap<dataId, ConcurrentHashMap<group, CacheData>> cache = new ConcurrentHashMap<>()
        SimpleCache<String> contentCache = new SimpleCache<String>();

        启动：
            设置成员变量SubscriberListener
            创建实例DiamondConfigure给成员变量
            创建LocalConfigInfoProcessor成员变量，并传递{user.home}/diamond/data启动
            创建ServerAddressProcessor成员变量，并启动
            创建SnapshotConfigInfoProcessor成员变量，并传递{user.home}/diamond/snapshot启动
            随机设置一个domainNameList元素位置给domainNamePos成员变量
            使用domainNamePos对应的domainName初始化httpClient成员变量
            启动定时任务，间隔为15秒
                遍历cache中的CacheData
                    调用LocalConfigInfoProcessor的获取配置方法，参数分别为cacheData和false
                    如果内容不为null
                        创建ConfigureInfomation实例，并通过DefaultSubscriberListener触发发送
                        通过snapshotConfigInfoProcessor保存快照
                        继续下一次循环
                    如果cacheData的useLocalConfigInfo为true，继续下一次循环
                遍历cache中的CacheData，生成{dataId}'(char) 2'{group}'(char) 2'{md5}'(char) 1'{dataId}'(char) 2'{group}'(char) 2'{md5}格式
                    通过POST方法请求domainName[domainNamePos]/config.co
                    如果响应结果为200
                        将响应结果按照'(char) 1'分隔记录，每个记录按照'(char) 2'分隔成dataId和group
                        如果cache当中存在对应的dataId和group，添加后台异步任务
                            判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
                            重试n次
                                请求domainName[domainNamePos]/config.co?dataId={dataId}&group={group}
                                    如果为200
                                        设定CacheData的md5、lastModified、
                                        将dataId-group、content作为K/V添加到contentCache中
                                        创建ConfigureInfomation实例，并通过DefaultSubscriberListener触发发送
                                    如果为304
                                        验证md5是否一样
                                    如果为404
                                        设置md5为null
                                        调用snapshotConfigInfoProcessor方法移除快照
                                    其他请求重置domainNamePos，然后重新执行获取配置方法，知道超时或超过重试次数

                    否则变更domainNamePos，重新执行第一步，直到如果超时，抛出异常。
                遍历cache中的CacheData
                    如果useLocalConfigInfo为false且fetchCount为0
                        判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
                        调用snapshotConfigInfoProcessor获取配置信息，如果返回的配置不为null，增加fetchCount并创建ConfigureInfomation实例，并通过DefaultSubscriberListener触发发送
            添加钩子，调用关闭方法

        关闭：
            调用localConfigInfoProcessor关闭方法
            调用serverAddressProcessor关闭方法
            关闭定时任务
            清空cache成员变量

        String getConfigureInfomation(String dataId, String group, long timeout)
            判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
            调用LocalConfigInfoProcessor的获取配置方法，参数分别为cacheData和true
            如果配置不为null
                增加fetchCount，通过snapshotConfigInfoProcessor保存快照，返回配置
            判断contentCache是否存在dataId-group，如果存在，直接返回对应value
            判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
            重试n次
                请求domainName[domainNamePos]/config.co?dataId={dataId}&group={group}
                    如果为200
                        设定CacheData的md5、lastModified、
                        将dataId-group、content作为K/V添加到contentCache中
                        增加fetchCount，通过snapshotConfigInfoProcessor保存快照，返回配置
                    如果为304
                        验证md5是否一样
                    如果为404
                        设置md5为null
                        调用snapshotConfigInfoProcessor方法移除快照
                    其他请求重置domainNamePos，然后重新执行获取配置方法，知道超时或超过重试次数

        public String getAvailableConfigureInfomation(String dataId, String group, long timeout)
            先调用getConfigureInfomation(String dataId, String group, long timeout)，如果存在返回值，直接返回
            判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
            调用snapshotConfigInfoProcessor获取配置信息，如果返回的配置不为null，增加fetchCount

        public String getAvailableConfigureInfomationFromSnapshot(String dataId, String group, long timeout)
            判断cache是否存在指定dataId和group，如果不存在，创建新的CacheData
            调用snapshotConfigInfoProcessor获取配置信息，如果返回的配置不为null，增加fetchCount，然后返回
            调用getConfigureInfomation(String dataId, String group, long timeout)，如果存在返回值，直接返回
        public void addDataId(String dataId, String group)
            如果cache中不存在指定dataId和group的CacheData，使用dataId和groupId创建CacheData并保存到cache中
        public boolean containDataId(String dataId, String group)
            判断cache中是否存在指定dataId和group的CacheData
        public void clearAllDataIds()
            清空cache
        public Set<String> getDataIds()
            返回cache的keySet
        public synchronized void removeDataId(String dataId, String group)
            从cache中移除对应的group，如果对应的dataId的value为empty，移除dataId

### DiamondClientFactory: 返回DefaultDiamondSubscriber单例

### DiamondManager
    DefaultDiamondManager

        public DefaultDiamondManager(String group, String dataId, List<ManagerListener> managerListenerList)
            设定dataId和group
            添加managerListenerList到订阅监听器中
            调用diamondSubscriber.addDataId(String dataId, String group)
            启动diamondSubscriber
        public void close()
            从订阅监听器中移除指定dataId和group
            调用diamondSubscriber.removeDataId(String dataId, String group)
            如果diamondSubscriber.getDataIds()长度为0，调用其close()
        public String getConfigureInfomation(long timeout)
            diamondSubscriber.getConfigureInfomation(this.dataId, this.group, timeout);
        public String getAvailableConfigureInfomation(long timeout)
            diamondSubscriber.getAvailableConfigureInfomation(dataId, group, timeout);
        public String getAvailableConfigureInfomationFromSnapshot(long timeout)
            diamondSubscriber.getAvailableConfigureInfomationFromSnapshot(dataId, group, timeout);

## 总结

* 动态维护服务器列表，访问配置failover(可以更完善)
* 常用的读取顺序：容灾目录 -> 内存 -> 网络 -> 快照。可以简化为：内存 -> 快照（主被） -> 网络。
* 容灾目录的使用度不高，可以结合作为到快照主。