package org.jsoup.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpArchive.ErrMsg;

public class JsoupClient {
	private static final int DEFAULT_MAX_RETRY_TIMES = 10;
	private static final boolean DEFAULT_USE_PROXY = false;
	private static final boolean DEFAULT_DISABLE_COOKIE = false;
	private static JsoupClient me = new JsoupClient();
	private Set<Cookie> cookies = new HashSet<>();
	private String proxy;
	private boolean followRedirect;
	private int redirectNum; // 用于防止重定向循环，重定向最多20次

	public boolean debug = false;

	public JsoupClient() {
		this.followRedirect = true;
	}

	public static Response executeSingleConn(Connection conn) throws IOException {
		return me.executeWithResp(conn, DEFAULT_MAX_RETRY_TIMES, DEFAULT_USE_PROXY, DEFAULT_DISABLE_COOKIE);
	}

	public Connection getGetConn(String url) {
		Connection conn = Jsoup.connect(url).ignoreContentType(true).followRedirects(false).timeout(10000);
		for (Cookie cookie : cookies) {
			if (cookie.match(url)) {
				conn.cookie(cookie.getKey(), cookie.getValue());
			}
		}
		return conn;
	}

	public Connection getPostConn(String url) {
		return getGetConn(url).method(Method.POST);
	}

	public Response executeWithResp(Connection conn) throws IOException {
		return executeWithResp(conn, DEFAULT_MAX_RETRY_TIMES);
	}

	public Response executeWithResp(Connection conn, int maxRetryTimes) throws IOException {
		return executeWithResp(conn, maxRetryTimes, DEFAULT_USE_PROXY);
	}

	public Response executeWithResp(Connection conn, int maxRetryTimes, boolean useProxy) throws IOException {
		return executeWithResp(conn, maxRetryTimes, useProxy, DEFAULT_DISABLE_COOKIE);
	}

	public Response executeWithResp(Connection conn, int maxRetryTimes, boolean useProxy, boolean disableCookie)
			throws IOException {
		IOException ioException = new IOException();
		HttpArchive har = new HttpArchive(conn.request(), null);
		for (int i = 1; i <= maxRetryTimes; i++) {
			int delaySeconds = (i < 5) ? i * 2 : 10;
			try {
				if (useProxy) {
					conn.proxy(Proxy.Type.HTTP, proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]));
				}
				HttpConnection.Response resp = (HttpConnection.Response) (conn.execute());
				har.resp = resp;
				logHttpArchive(har);
				if (!disableCookie) {
					Set<Cookie> cookieSet = resp.getCookieSet();
					for (Cookie cookie : cookies) {
						cookieSet.add(cookie);
					}
					this.cookies = cookieSet;
				}
				if (resp.hasHeader("Location") && followRedirect) {
					redirectNum++;
					if (redirectNum > 20) {
						redirectNum = 0;
						return resp;
					}
					String location = resp.header("Location");
					Connection redirectConn = getGetConn(encodeUrl(location));
					redirectConn.userAgent(conn.request().header("User-Agent"));
					String referrer = conn.request().header("Referer");
					if (referrer != null) {
						redirectConn.referrer(referrer);
					}

					Response redirectedResp = executeWithResp(redirectConn, 1, useProxy, disableCookie);
					redirectNum = 0;
					return redirectedResp;
				}
				return resp;
			} catch (HttpStatusException hse) {
				har.errMsg = new ErrMsg(hse.getStatusCode(), hse.getMessage());
				logHttpArchive(har);
				System.out.println(
						"HttpStatusException:" + hse.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
				ioException = hse;
			} catch (UnknownHostException uhe) {
				har.errMsg = ErrMsg.err("UnknownHostException:" + uhe.getMessage());
				System.out.println(
						"UnknownHostException:" + uhe.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
				ioException = uhe;
			} catch (SocketTimeoutException ste) {
				har.errMsg = ErrMsg.err("SocketTimeoutException:" + ste.getMessage());
				System.out.println("网络访问失败" + ste.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
			} catch (IOException e) {
				har.errMsg = ErrMsg.err("IOException:" + e.getMessage());
				System.out.println("网络访问失败" + e.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
			}
			if (i < maxRetryTimes) {
				try {
					Thread.sleep(delaySeconds * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		logHttpArchive(har);
		throw ioException;
	}

	private void logHttpArchive(HttpArchive har) {
		if (!debug) {
			return;
		}
		// 每次log写一次文件，仅在debug时可以接受！
		File file = new File("jsoup_client_" + hashCode() + ".log");
		try {
			FileOutputStream out = new FileOutputStream(file, true);
			out.write(har.toString().getBytes("utf-8"));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println(har);
	}

	public Set<Cookie> getCookies() {
		return cookies;
	}

	public void setCookies(Set<Cookie> cookies) {
		this.cookies = cookies;
	}

	public void setFollowRedirect(boolean followRedirect) {
		this.followRedirect = followRedirect;
	}

	public void clearCookies() {
		cookies.clear();
	}

	private static String encodeUrl(String url) {
		if (url == null)
			return null;
		return url.replaceAll(" ", "%20");
	}

}
