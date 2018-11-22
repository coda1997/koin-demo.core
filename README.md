### 配置&运行

一共有三个文件，需要放在一个目录中

```
./
    config.json
    run.sh
    koin-*.jar
```

其中`config.json`存放需要配置的信息，如下：

```json
{
    "port": 8080,
    "userName":"your_username",
    "password":"your_password",
    "dataBasePort":3306,
    "dataBaseIp":"127.0.0.1",
    "dataBaseName":"your_database_name"
}
```

在运行前需要配置好IP、端口、数据库等，之后执行`run.sh`，即可运行。

```shell
./run.sh
```

### API

| url             | method | parameters                 | comment                                                  |
| --------------- | ------ | -------------------------- | -------------------------------------------------------- |
| /point          | get    | floor : Int                          | 获取所有点                                               |
|                 | post   | point : Point              | 添加一个点(json格式的对象)                               |
| /point/{`time`} | get    | time : Long, floor : Int                | 获取时间为`time`之后的所有的点（包括在`time`之后更新的） |
|                 | patch   | time : Long, point : Point | 更新时间为`time`的点数据(更新后点的`time`为新的时间)     |
|                 | delete | time : Long                | 删除时间为`time`的点                                     |
| /building       | get    | -                          | 返回所有大楼的信息                                       |
| /building/{bid} | get    | bid : String               | 获取大楼名为`bid`的信息(floor等)                         |

