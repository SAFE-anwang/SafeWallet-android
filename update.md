2022年6月30日版本号V0.24.0.2.0630
新增功能如下：
1.safe跨链功能：safe<->bsc间跨链互转
2.safe市场行情下内置safe官方推特
3.创建新钱包时safe，btc，ltc，dash初始同步区块为当前区块

2022年5月30日版本号V0.24.0.2.0530
新增功能如下：

1.safe跨链功能：safe<->eth间跨链互转。

2.集成telegram客户端，无缝对接电服群。

3.交易记录移到钱包余额下。

4.中英文支持

5.增加SAFE专区：跨链和锁仓（待开发）

6.解决多子钱包切换时无法同步问题

7.解决多子钱包切换时交易记录混淆问题

说明：跨链测试环境修改方法
1、maket工程assets下面的initial_coins_list文件最后一行，0xee9c1ea4dcf0aaf4ff2d78b6ff83aa69797b65eb替换成0x32885f2faf83aeee39e2cfe7f302e3bb884869f4

2、maket工程io.horizontalsystems.marketkit.storage.MarketDatabase类27行，数据库版本号修改一下

3、app工程 io.horizontalsystems.bankwallet.core.managers.EvmNetworkManager类15行，Ropsten网络注释打开


2022年3月31日版本号v0.24.0.1

1.增加vpn设置入口，vpn连接状态显示，vpn和tor二选一，或全不选。

2.解决市值排名随机出现未按币种市值排序。

3.增加ETH，BNB币收款交易记录显示，解决所有币的交易记录显示刷新问题.

4.解决恢复钱包时ETH，BNB币没有交易记录问题。

5.增加SAFE同步数据块的处理机制，逻辑如下：  

  （1）创建恢复钱包初始化时通过调用api获取种子ip地址缓存；如果调用api失败就用默认ip；api:https://chain.anwang.org/insight-api-safe/utils/address/seed
  
  （2）每次同步数据（创建钱包，恢复钱包，日常数据同步）从缓存数据库中随机取5个种子节点ip,通过发送消息VersionMessage解析其返回信息获取所有查询种子节点的区块高度。
  
  （3）.比对获得各种子节点的区块高度大小，选择区块高度最大连接成功次数多的种子节点进行连接开始同步数据。  
  
6.增加新建钱包时安全中心下区块链的更新方式选择，包括API和blockchain。  


