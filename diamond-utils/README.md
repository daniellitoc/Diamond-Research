### IO
    Watchable       // Path实现了该接口，Path为file封装类，同时封装WatchService和WatchKey交互API
    WatchKey        // 监控事件，可针对某一目录或文件，添加不同的监听行为(create/delete/modify)，如果是目录，建立磁盘和内存之间的完整映射关系(包含子目录)，当定时任务调用监控事件的check方法，其主要校对映射关系用于发现变化。
    WatchService    // 定时任务：间隔默认为500毫秒，检测所有已注册(watchedKeys队列)的Watch事件(Watchey)，若事件发生变化，将其转移到changedKeys队列中。

### domain
    ConfigInfo      // 主体交互
    ConfigInfoEx    // 用于批量操作

### cache
    SimpleCache     // 具备过期功能的缓存，非LRU(不具备清理)