package org.jsoup;

import java.io.IOException;

import org.jsoup.Connection.Response;

/**
 * Signals that a HTTP request resulted in a not OK HTTP response.
 */
public class HttpStatusException extends IOException {
	private int statusCode;
	private String url;
	private Response resp;

	public HttpStatusException(String message, int statusCode, String url, Response resp) {
		super(message);
		this.statusCode = statusCode;
		this.url = url;
		this.resp = resp;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return super.toString() + ". Status=" + statusCode + ", URL=" + url;
	}

	public Response getResp() {
		return resp;
	}

	public void setResp(Response resp) {
		this.resp = resp;
	}
}
