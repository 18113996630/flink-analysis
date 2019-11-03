package com.hrong.analysis.ip

import com.alibaba.fastjson.JSONObject
import com.hrong.analysis.source.NginxLogSource
import com.hrong.analysis.util.HttpClientUtil
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * ip检测规则：
  * 1. 同一ip每次请求间隔时间相同
  * 2. 重复请求同一地址次数过多
  */
object IllegalIpAnalysis {
  val sdf: FastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss:SSS")

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      throw new RuntimeException("参数有误，usage：url 窗口时间 访问次数上限")
    }
    val apiUrl = args(0)
    val windowSize = args(1).toLong
    val limitFrequency = args(2).toLong
    println("api地址："+apiUrl+", 窗口地址："+windowSize+", 访问频率次数上限："+limitFrequency)
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    //设置eventTime为流数据时间类型
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //自定义的nginx-source，已实现去重功能
    val sourceData = env.addSource(new NginxLogSource)
    import org.apache.flink.api.scala._
    val requestLog = sourceData
      .filter(log => log.getRequestMethod.contains("/major/"))
      .map(log => SimpleLog(log.getIp, log.getTime, log.getTimeFormat, log.getReferer, log.getUserAgent, 1L))
      .assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[SimpleLog] {
        // 事件时间
        var currentMaxTimestamp = 0L
        val maxOutOfOrder = 3000L
        var lastEmittedWatermark: Long = Long.MinValue

        // Returns the current watermark
        override def getCurrentWatermark: Watermark = {
          // 允许延迟三秒
          val potentialWM = currentMaxTimestamp - maxOutOfOrder
          // 保证水印能依次递增
          if (potentialWM >= lastEmittedWatermark) {
            lastEmittedWatermark = potentialWM
          }
          new Watermark(lastEmittedWatermark)
        }

        override def extractTimestamp(element: SimpleLog, previousElementTimestamp: Long): Long = {
          // 将元素的时间字段值作为该数据的timestamp
          val time = element.time
          if (time > currentMaxTimestamp) {
            currentMaxTimestamp = time
          }
          time
        }
      })
    requestLog.print("中间数据：")
    val illegalData = requestLog.keyBy(_.ip)
      .timeWindow(Time.seconds(windowSize))
      .sum("count").filter(_.count > limitFrequency)
    illegalData.print("非法数据")
    // 调用接口将非法IP入库、存入redis
    illegalData.map(log => {
      val black = new JSONObject()
      black.put("ip", log.ip)
      black.put("checkTime", log.timeFormat)
      black.put("detail", windowSize+"秒钟访问次数：" + log.count)
      black.put("status", 1L)
      black.toJSONString
    }).addSink(blackString => {
      // 将非法数据传给业务系统，由业务系统进行具体的逻辑处理
      val result = HttpClientUtil.httpPost(apiUrl, blackString, null)
      println(result)
    })
    env.execute(getClass.getSimpleName)
  }
}

case class SimpleLog(ip: String, time: Long, timeFormat: String, referer: String, userAgent: String, count: Long)
