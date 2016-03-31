package org.jsoup.helper;

import java.net.MalformedURLException;
import java.net.URL;

public class Cookie {

	private String key;
	private String value;
	private String path;
	private String domain;
	private Long expires;

	public Cookie() {

	}

	public Cookie(String key, String value, String path, String domain, Long expires) {
		this.key = key;
		this.value = value;
		this.path = path;
		this.domain = domain;
		this.expires = expires;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Long getExpires() {
		return expires;
	}

	public void setExpires(Long expires) {
		this.expires = expires;
	}

	public boolean match(String url) {
		Long timestamp = System.currentTimeMillis();
		if (this.expires < timestamp) {
			return false;
		}

		try {
			URL uurl = new URL(url);
			String realDomain = "." + uurl.getAuthority();
			String realPath = uurl.getPath();
			if (realPath.isEmpty()) {
				realPath = "/";
			}
			return realDomain.matches(".*" + domain) && realPath.startsWith(path);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean equals(Object obj) {
		Cookie theOther = (Cookie) obj;
		return this.key.equals(theOther.getKey()) && this.domain.equals(theOther.getDomain())
				&& this.path.equals(theOther.getPath());
	}

	public int hashCode() {
		return this.key.hashCode();
	}

	public String toString() {
		return "Cookie [key=" + key + ", value=" + value + ", path=" + path + ", domain=" + domain + ", expires="
				+ expires + "]";
	}

	public static void main(String[] args) throws Exception {
		Cookie cookie = new Cookie("sessionid", "111", "/", "passport2.chaoxing.com", System.currentTimeMillis() * 2);
		System.out.println(cookie.match("http://mooc1-1.chaoxing.com/visit/courses"));
	}

}