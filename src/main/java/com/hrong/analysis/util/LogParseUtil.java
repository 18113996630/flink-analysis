package com.hrong.analysis.util;

import com.hrong.analysis.entity.Log;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @Author hrong
 * @Description
 **/
@Slf4j
public class LogParseUtil {
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z", Locale.ENGLISH);
	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) throws ParseException {
		Date date = formatter.parse("16/Sep/2019:13:02:42 +0800");
		System.out.println(format.format(date));
	}

	/**
	 * 解析nginx日志
	 * @param logString 原始日志
	 * @return log对象
	 */
	public static Log parseLog(String logString) {
		String[] datas = logString.split("\\|");
		String ip = datas[0];
		String user = datas[1];
		String time = datas[2];
		long logTime = 0L;
		String timeFormat = null;
		try {
			Date date = formatter.parse(time);
			logTime = date.getTime();
			timeFormat = format.format(date);
		} catch (ParseException e) {
			log.error("日志转换出错，输入：{} 错误：{}", logString, e.getMessage());
		}
		String requestMethod = datas[3];
		String status = datas[4];
		String bodySize = datas[5];
		String referer = datas[6];
		String userAgent = datas[7];
		String forwarded = datas[8];
		return new Log(ip, user, logTime, timeFormat, requestMethod, status, bodySize, referer, userAgent, forwarded, 1L);
	}
}
