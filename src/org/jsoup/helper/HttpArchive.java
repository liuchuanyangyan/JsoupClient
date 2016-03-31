package org.jsoup.helper;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.jsoup.Connection.KeyVal;
import org.jsoup.Connection.Request;
import org.jsoup.Connection.Response;

public class HttpArchive {

	public ErrMsg errMsg;
	public Request req;
	public Response resp;

	public HttpArchive(Request req, Response resp) {
		this.req = req;
		this.resp = resp;
	}

	public String getRequestRaw() {
		String path = req.url().getPath();
		String queryString = req.url().getQuery();
		if (queryString != null && !queryString.isEmpty()) {
			path += "?" + req.url().getQuery();
		}
		String result = req.method().toString() + " " + path + " HTTP/1.1\r\n";
		result += "Host: " + req.url().getAuthority() + "\r\n";
		for (Entry<String, String> entry : req.headers().entrySet()) {
			result += entry.getKey() + ": " + entry.getValue() + "\r\n";
		}

		if (!req.data().isEmpty()) {
			result += "\r\n";
		}

		// TODO 目前的Log仅支持参数形式的post消息体
		Iterator<KeyVal> iter = req.data().iterator();
		int postParamCount = 0;
		while (iter.hasNext()) {
			postParamCount++;
			KeyVal kv = iter.next();
			result += kv.key() + "=" + kv.value() + "&";
		}
		if (postParamCount > 0) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	public String getResppnseRaw() {
		String result;
		if (resp == null) {
			// http request fail
			result = errMsg.statusCode + " " + errMsg.msg;
		} else {
			result = "HTTP/1.1 " + resp.statusCode() + " " + resp.statusMessage() + "\r\n";
			for (Entry<String, String> entry : resp.headers().entrySet()) {
				result += entry.getKey() + ": " + entry.getValue() + "\r\n";
			}
			result += "\r\n";
			result += resp.body();
		}
		return result;
	}

	/**
	 * 从自定义的log文件中解析出HttpArchive列表
	 */
	public static List<HttpArchive> resolveFromText(String raw) {
		// TODO
		return null;
	}

	/**
	 * 从单个HAR文本中解析出HttpArchive对象
	 */
	public static HttpArchive resolveSingleHarFromText(String raw) {
		String httpRequestLabel = "----HttpRequest----";
		String httpResponseLabel = "----HttpRespone----";
		int reqStartIndex = raw.indexOf(httpRequestLabel);
		int respStartIndex = raw.indexOf(httpResponseLabel);
		if (reqStartIndex < 0 || respStartIndex <= respStartIndex) {
			throw new IllegalArgumentException("invalid format");
		}

		String requestText = raw.substring(reqStartIndex, respStartIndex);
		Scanner scan = new Scanner(requestText);
		String reqFirseLine = scan.nextLine();
		// TODO parse request

		while (scan.hasNext()) {
			String line = scan.nextLine();
			if (httpRequestLabel.equals(raw) || httpResponseLabel.equals(raw)) {
				continue;
			}

		}

		// TODO
		return null;
	}

	public String getResponseBody() {
		return resp.body();
	}

	public byte[] getResponseByteData() {
		return resp.bodyAsBytes();
	}

	public String toString() {
		String currentTime = new Timestamp(System.currentTimeMillis()).toString();
		return "----HttpArchive----\r\n" + System.currentTimeMillis() + " " + currentTime
				+ "\r\n----HttpRequest----\r\n" + getRequestRaw() + "\r\n----HttpResponse----\r\n" + getResppnseRaw()
				+ "\r\n";
	}

	public static final class ErrMsg {
		public int statusCode;
		public String msg;

		public ErrMsg(int statusCode, String msg) {
			this.statusCode = statusCode;
			this.msg = msg;
		}

		public static ErrMsg err(String msg) {
			return new ErrMsg(-1, msg);
		}

	}
}
