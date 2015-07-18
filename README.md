# Diamond #

一个持久配置管理中心，核心功能是使应用在运行中感知配置数据的变化。

## 附加功能

* 源码分析

## 使用

### 创建数据库

    create database diamond;
    grant all on diamond.* to root@'%'  identified by 'root';
    use diamond;
    create table config_info (
        'id' bigint(64) unsigned NOT NULL auto_increment,
        'data_id' varchar(255) NOT NULL default ' ',
        'group_id' varchar(128) NOT NULL default ' ',
        'content' longtext NOT NULL,
        'md5' varchar(32) NOT NULL default ' ',
        'gmt_create' datetime NOT NULL default '2010-05-05 00:00:00',
        'gmt_modified' datetime NOT NULL default '2010-05-05 00:00:00',
        PRIMARY KEY  ('id'),
        UNIQUE KEY 'uk_config_datagroup' ('data_id','group_id')
    );

### 配置diamond-server

* 配置/src/resources/jdbc.properties
* 配置/src/resources/node.properties
* 执行mvn clean package -Dmaven.test.skip

### 部署到diamond-server到指定Tomcat(node.properties)

### 创建主被Tomcat

* 添加domains文件到webapps，内容为node.properties

### 发布数据

### 订阅数据

* 配置Constants.DAILY_DOMAINNAME为被ip
* 配置Constants.DEFAULT_DOMAINNAME为主ip
* 配置Constants.DEFAULT_PORT端口

### API

    DiamondManager manager = new DefaultDiamondManager(group, dataId, new ManagerListener() {

        public Executor getExecutor() {
            return null;
        }

        public void receiveConfigInfo(String configInfo) {
        }

    });

    manager.getAvailableConfigureInfomation(timeout);

    manager.close();

## 容灾机制

* 服务端本地文件和数据库两种存储
* 客户端内部failover
* 客户端快照
* MD5校验变化
* 容灾目录

## 不可用触发条件

* 数据库不可用
* 所有server均不可用
* client主动删除了snapshot
* client没有备份配置数据，导致其不能配置“容灾目录”


