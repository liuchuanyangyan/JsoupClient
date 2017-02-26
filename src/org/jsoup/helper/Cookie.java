package org.jsoup.helper;

import java.net.MalformedURLException;
import java.net.URL;

public class Cookie {

	private final String key;
	private final String value;
	private final String path;
	private final String domain;
	private final Long expires;

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

	public String getValue() {
		return value;
	}

	public String getPath() {
		return path;
	}

	public String getDomain() {
		return domain;
	}

	public Long getExpires() {
		return expires;
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

}