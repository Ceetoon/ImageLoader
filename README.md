# ImageLoader
练手项目图片加载功能，支持磁盘和内存缓存。亮点是在线程池管理的基础上用队列去管理，每次取任务队列队尾的任务执行，这样可以让用户当前想看到图片优先加载。
