package my.cute.parser.youtube;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * simple wrapper around a URL to make it easier to work with the two parts of 
 * a url that we care about: the protocol (here called access, as in the original vlc
 * script) and everything else (here called path)
 */
class ParsedURL {

	private final URL url;
	private final String fullPath;
	
	ParsedURL(String url) throws MalformedURLException {
		this.url = new URL(url);
		StringBuilder pathBuilder = new StringBuilder(this.url.getAuthority());
		pathBuilder.append(this.url.getPath());
		if(this.url.getQuery() != null) {
			pathBuilder.append("?");
			pathBuilder.append(this.url.getQuery());
		}
		this.fullPath = pathBuilder.toString();
	}
	
	public String getAccess() {
		return this.url.getProtocol();
	}
	
	public String getPath() {
		return this.fullPath;
	}
	
	public String getAuthority() {
		return this.url.getAuthority();
	}
	
	public URL getURLObject() {
		return this.url;
	}
	
	public String toString() {
		return this.url.toString();
	}
	
}
