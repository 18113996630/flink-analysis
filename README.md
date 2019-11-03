# flink-analysis
使用flink进行nginx日志监控，检测异常访问ip，并将数据发送到业务系统
**com.hrong.analysis.source.NginxLogSource**为自定义的Source，监控nginx日志并实现去重的功能
**com.hrong.analysis.ip.IllegalIpAnalysis**为主要的检测逻辑代码
