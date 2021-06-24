package my.cute.parser.youtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JSPageImpl implements JSPage {
	
	private final static int DEFAULT_TIMEOUT = 10000;
	
	private final URL url;
	private final BufferedReader reader;
	private final List<String> lines;
	private final Pattern endOfLine = Pattern.compile("};$");
	
	JSPageImpl(String url, int timeout) throws MalformedURLException, IOException, SocketTimeoutException {
		this.url = new URL(url);
		URLConnection con = this.url.openConnection();
		con.setConnectTimeout(timeout);
		con.setReadTimeout(timeout);
		this.reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
		this.lines = new ArrayList<>();
	}
	
	JSPageImpl(String url) throws MalformedURLException, IOException {
		this(url, DEFAULT_TIMEOUT);
	}

	@Override
	public String extract(Pattern pattern) throws IOException, SocketTimeoutException {
		try {
			//check saved lines first
			for(String line : this.lines) {
				Matcher matcher = pattern.matcher(line);
				if(matcher.find()) return matcher.group(1);
			}
			
			//no match found. check remainder of stream
			String line;
			while((line = this.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if(matcher.find()) return matcher.group(1);
			}
			
			//no match
			return null;
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("pattern to extract must contain a capturing group");
		}
	}
	
	@Override
	public String extract(String pattern) throws IOException, SocketTimeoutException {
		return this.extract(Pattern.compile(pattern));
	}
	
	@Override
	public void close() throws IOException {
		this.reader.close();
	}
	
	private String readLine() throws IOException, SocketTimeoutException {
		String line = "";
		
		do {
			String singleLine = this.reader.readLine();
			if(singleLine == null) break;
			line += singleLine;
		} while(!endOfLine.matcher(line).find());
		
		if(line.isEmpty()) {
			//end of stream
			return null;
		} else {
			this.lines.add(line);
			return line;
		}
	}

}
