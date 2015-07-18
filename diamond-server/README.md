### 数据库
    id/dataId/group/content/md5/createTime/updateTime

### 内存MD5
    Key: group/dataId
    Value: MD5(content)

### 本地磁盘
    文件：webapps/config-data/groupId/dataId
    内容：content

### 节点记录
    文件：classpath:node.properties
    用途：用于通知

### system.properties
    dump_config_interval：默认为600

### DumpConfigInfoTask: 上一次任务执行完毕后的dump_config_interval秒后，重新执行一次该任务
    获取数据库中所有配置信息
    遍历
        记录MD5到内存
        写入到本地磁盘

### /admin.do
    POST    method=postConfig   dataId  group   content         保存配置信息
        验证失败转发到/admin/config/new
        验证成功
            存储到数据库
            记录MD5到内存
            写入到本地磁盘
            通知除当前节点外的其他所有节点（格式：http://{node}/diamond-server/notify.do）
            返回包含当前元素的分页列表（/admin/config/list或/admin/config/list_json）
    GET     method=deleteConfig id                              删除配置信息
        根据ID从数据库查找配置信息
        根据配置信息的dataId和group删除本地文件
        清除内存中的MD5
        从数据库中删除
        通知除当前节点外的其他所有节点（格式：http://{node}/diamond-server/notify.do）
        转发到/admin/config/list
    POST    method=upload   dataId  group   contentFile         保存配置信息（文件上传）
        验证失败转发到/admin/config/upload
        验证成功
            参考method=postConfig
    POST    method=updateConfig dataId  group   content         更新配置信息
        验证失败转发到/admin/config/edit
        验证成功
            通过dataId和group更新数据库
            记录MD5到内存
            写入到本地磁盘
            通知除当前节点外的其他所有节点（格式：http://{node}/diamond-server/notify.do）
            返回包含当前元素的分页列表（/admin/config/list或/admin/config/list_json）
    POST    method=reupload dataId  group   contentFile         更新配置信息（文件上传）
            验证失败转发到/admin/config/edit
            验证成功
                参考method=updateConfig
    GET     method=listConfig   dataId  group   pageNo  pageSize        分页展示配置信息的列表（基于数据节点的精确查询）
        返回[dataId]或[group]的分页列表（/admin/config/list或/admin/config/list_json）
    GET     method=listConfigLike   dataId  group   pageNo  pageSize    分页展示配置信息的的列表（模糊查询）
        返回[dataId]或[group]的分页列表（/admin/config/list或/admin/config/list_json）
    GET     method=detailConfig dataId  group                   进入编辑页面
        获取指定配置信息，转发到/admin/config/edit
    POST    method=batchQuery   dataIds group
        dataIds按照'(char) 2'分隔
        遍历dataIds，从数据库中获取配置信息并汇总
        转发到/admin/config/batch_result
    POST    method=batchAddOrUpdate allDataIdAndContent group   批量添加或更新
        allDataIdAndContent按照'(char) 1'拆解成行，每行按照'(char) 2'分隔成dataId和content
        遍历配置信息，判断配置是否存在，若存在，则更新，否则添加（更新和添加流程参考上面）
        转发到/admin/config/batch_result
    GET     method=listUser                                 展示用户
        获取所有用户列表
        转发到/admin/user/list
    POST    method=addUser  userName    password            添加用户
    GET     method=deleteUser   userName                    删除用户
    GET     method=changePassword   userName    password    改变用户密码
    GET     method=reloadUser                               重新加载用户

    POST    method=setRefuseRequestCount    count           设置可请求次数
    GET     method=getRefuseRequestCount                    获取可请求次数

### AuthorizationFilter: 验证Session中是否存在user属性，不存在转发到/jsp/login.jsp。

### /login.do
    POST    method=login    username    password            登录
        验证成功添加用户到Session，转发到admin/admin
        否则转发到login
    GET     method=logout                                   注销

### /notify.do
    GET     method=notifyConfigInfo dataId  group           加载配置到本地磁盘
        加载配置
        如果存在
            记录MD5到内存
            写入到本地磁盘
        否则
            清除内存中的MD5
            从数据库中删除

### /config.co
    GET     group       dataId                              请求单个节点配置
        验证dataId
        获取客户端IP，获取不到返回400
        根据计数器判断是否可用，不能用返回503
        获取内存中的数据MD5，获取不到返回404
        判断此时如果有人修改指定配置信息，返回304          // 双重检查
        生成节点所在本地文件
        判断此时如果有人修改指定配置信息，返回304          // 双重检查
        转发到指定文件
    POST    Probe-Modify-Request                            检测节点配置是否发生变化
        验证Probe-Modify-Request
        获取客户端IP，获取不到返回400
        根据计数器判断是否可用，不能用返回503
        按照'(char) 1'拆解成行，每行按照'(char) 2'分隔成dataId和[group]和[md5]
        验证内存中的MD5是否和传入的相等，不等意味着发生变化
        返回所有发生变化的配置信息，dataId和group使用'(char) 2'拼接，各个配置按照'(char) 1'拼接
        返回200

# 总结
    数据库主要承担配置最终的持久化工作，客户端流量的请求不依赖数据库，依赖于本地文件。
        本地文件承担数据库不可用(RPC)现象，但本地文件过多(ulimit)，可以考虑专门的本地存储如: LevelDB。
        考虑数据库流量造成的压力问题，可以添加Cache解决，但无法杜绝RPC问题。
    本地数据最新保证方式
        后台添加/修改配置，同步更新，并通知其他同应用更新
        应用启动时，同时更新所有配置到本地，以及后续后台定时任务更新所有配置到本地
    内存MD5和本地文件的状态，可假定认为具备原子性。
