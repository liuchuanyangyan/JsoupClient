package org.jsoup.helper;

import java.io.IOException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

public class JsoupClient {
	private final Set<Cookie> cookies = new HashSet<>();
	private final boolean followRedirect;
	private Proxy proxy;

	public JsoupClient() {
		this.followRedirect = true;
	}

	public JsoupClient(boolean followRedirect) {
		this.followRedirect = followRedirect;
	}

	private static String encodeUrl(String url) {
		if (url == null)
			return null;
		return url.replaceAll(" ", "%20");
	}

	public Connection getGetConn(String url) {
		Connection conn = Jsoup.connect(url).ignoreContentType(true).followRedirects(false).timeout(10000);
		synchronized (cookies) {
			for (Cookie cookie : cookies) {
				if (cookie.match(url)) {
					conn.cookie(cookie.getKey(), cookie.getValue());
				}
			}
		}
		return conn;
	}

	public Connection getPostConn(String url) {
		return getGetConn(url).method(Method.POST);
	}

	public Response executeWithResp(Connection conn) throws IOException {
		return executeWithResp(conn, 10);
	}

	public Response executeWithResp(Connection conn, int maxRetryTimes) throws IOException {
		return executeWithResp(conn, maxRetryTimes, 0);
	}

	private Response executeWithResp(Connection conn, int maxRetryTimes, int redirectTimes) throws IOException {
		IOException ioException = null;
		for (int i = 1; i <= maxRetryTimes; i++) {
			int delaySeconds = (i < 5) ? i * 2 : 10;
			try {
				synchronized (this) {
					if (proxy != null) {
						conn.proxy(proxy);
					}
				}
				HttpConnection.Response resp = (HttpConnection.Response) (conn.execute());
				Set<Cookie> cookieSet = resp.getCookieSet();
				for (Cookie cookie : cookieSet) {
					synchronized (this.cookies) {
						if (cookies.contains(cookie)) {
							cookies.remove(cookie);
							cookies.add(cookie);
						}
					}
				}
				if (resp.hasHeader("Location") && followRedirect) {
					if (redirectTimes > 20) {
						return resp;
					}
					String location = resp.header("Location");
					Connection redirectConn = getGetConn(encodeUrl(location));
					redirectConn.userAgent(conn.request().header("User-Agent"));
					String referrer = conn.request().header("Referer");
					if (referrer != null) {
						redirectConn.referrer(referrer);
					}

					Response redirectedResp = executeWithResp(redirectConn, 1, redirectTimes++);
					return redirectedResp;
				}
				return resp;
			} catch (HttpStatusException hse) {
				System.err.println(
						"HttpStatusException:" + hse.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
				ioException = hse;
			} catch (UnknownHostException uhe) {
				System.err.println(
						"UnknownHostException:" + uhe.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
				ioException = uhe;
			} catch (IOException ioe) {
				System.err.println("IOException" + ioe.getMessage() + ", 次数 " + i + "，" + delaySeconds + " 秒后继续尝试");
				ioException = ioe;
			}
			if (i < maxRetryTimes) {
				try {
					Thread.sleep(delaySeconds * 1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw ioException;
	}

	public synchronized void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public void clearCookies() {
		synchronized (this.cookies) {
			cookies.clear();
		}
	}

}
