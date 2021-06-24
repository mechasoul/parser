package my.cute.parser.youtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basically everything is credit to vlc's included playlist parser youtube.luac
 * essentially all code is just translated from lua to java
 */
class YoutubeParserImpl implements YoutubeParser {
	
	private static final Logger logger = LoggerFactory.getLogger(YoutubeParserImpl.class);

	private static final int DEFAULT_PREFERRED_RES = 720;
	private static final int DEFAULT_TIMEOUT = 10000;
	
	private final int prefRes;
	private final int timeout;
	
	YoutubeParserImpl() {
		this(DEFAULT_PREFERRED_RES);
	}
	
	YoutubeParserImpl(int prefRes) {
		this(prefRes, DEFAULT_TIMEOUT);
	}
	
	YoutubeParserImpl(int prefRes, int timeout) {
		this.prefRes = prefRes;
		this.timeout = timeout;
	}
	
	private String getFmt(String fmtList) {
		logger.debug("QX entered getFmt with fmtList: " + fmtList);
		String fmt = null;
		Matcher matcher = GET_FMT.matcher(fmtList);
		while(matcher.find()) {
			fmt = matcher.group(1);
			if(this.prefRes >= Integer.parseInt(matcher.group(2))) 
				break;
		}
		logger.debug("QX found fmt: " + fmt);
		return fmt;
	}
	
	private String jsDescramble(String signature, String jsUrl) {
		logger.debug("QX entered jsDescramble with signature: " + signature + ", jsUrl: " + jsUrl);
		try (JSPage jsPage = new JSPageImpl(jsUrl)) {
			String descrambler = jsPage.extract(DESCRAMBLER);
			if(descrambler == null) {
				logger.info(this + ": couldn't extract youtube video URL signature descrambling function name");
				return signature;
			}
			String rules = jsPage.extract("^" + descrambler + "=function\\([^)]*\\)\\{(.*?)\\};");
			if(rules == null) {
				logger.info(this + ": couldn't extract youtube video URL signature descrambling rules");
				return signature;
			}
			Matcher helperMatcher = DESCRAMBLE_HELPER.matcher(rules);
			if(!helperMatcher.find()) {
				logger.info(this + ": couldn't extract youtube video URL signature transformation helper name");
				this.logGeneralError();
				return signature;
			}
			String helper = helperMatcher.group(1);
			String transformations = jsPage.extract("[ ,]" + helper + "=\\{(.*?)\\};");
			if(transformations == null) {
				logger.info(this + ": couldn't extract youtube video URL signature transformation code");
				return signature;
			}
			
			final Map<String, String> trans = new HashMap<>();
			
			DESCRAMBLE_TRANS.matcher(transformations).results().forEachOrdered(result -> {
				String meth = result.group(1);
				String code = result.group(2);
				if(code.contains(".reverse(")) {
					trans.put(meth, "reverse");
				} else if (code.contains(".splice(")) {
					trans.put(meth, "splice");
				} else if (code.contains("var c=")) {
					trans.put(meth, "swap");
				} else {
					logger.warn(this + ": couldn't parse unknown youtube video URL signature transformation");
				}
			});
			logger.debug("QX found trans: " + trans.toString());
			
			boolean missing = false;
			Matcher indexMatcher = DESCRAMBLE_INDEX.matcher(rules);
			StringBuilder descrambledSig = new StringBuilder(signature);
			while(indexMatcher.find()) {
				String meth = indexMatcher.group(1);
				int index = Integer.parseInt(indexMatcher.group(2));
				String savedTrans = trans.get(meth);
				if(savedTrans == null) {
					logger.warn(this + ": couldn't apply unknown youtube video URL signature transformation");
					missing = true;
				} else if (savedTrans.equals("reverse")) {
					descrambledSig.reverse();
				} else if (savedTrans.equals("splice")) {
					descrambledSig.delete(0, index);
				} else if (savedTrans.equals("swap")) {
					String firstCharacter = descrambledSig.substring(0, 1);
					String indexCharacter = descrambledSig.substring(index, index+1);
					descrambledSig.replace(0, 1, indexCharacter);
					descrambledSig.replace(index, index+1, firstCharacter);
				}
			}
			if(missing) {
				this.logGeneralError();
			}
			logger.debug("QX descrambled sig: " + descrambledSig.toString());
			return descrambledSig.toString();
		} catch (MalformedURLException e) {
			logger.warn(this + ": exception thrown during JSPage construction when descrambling sig! sig: "
					+ signature + ", jsUrl: " + jsUrl, e);
			return signature;
		} catch (SocketTimeoutException e) {
			logger.warn(this + ": socket timeout during signature descramble! sig: " 
					+ signature + ", jsUrl: " + jsUrl, e);
			return signature;
		} catch (IOException e) {
			logger.warn(this + ": general io error during signature descramble! sig: "
					+ signature + ", jsUrl: " + jsUrl, e);
			return signature;
		} 
	}
	
	private String streamUrl(String params, String jsUrl) {
		logger.debug("QX entered streamUrl");
		Matcher urlMatcher = URL_EXTRACT.matcher(params);
		if(!urlMatcher.find()) return null;
		String url = urlMatcher.group(1);
		url = URLDecoder.decode(url, StandardCharsets.UTF_8);
		logger.debug("QX found url: " + url);
		Matcher sMatcher = S_EXTRACT.matcher(params);
		if(!sMatcher.find()) return url;
		String s = sMatcher.group(1);
		s = URLDecoder.decode(s, StandardCharsets.UTF_8);
		logger.debug(this + ": found " + s.length() + "-character scrambled signature for youtube video URL, "
				+ "attempting to descramble...");
		if(jsUrl != null) 
			s = jsDescramble(s, jsUrl);
		else {
			this.logGeneralError();
		}
		Matcher spMatcher = SP_EXTRACT.matcher(params);
		String sp;
		if(spMatcher.find())
			sp = spMatcher.group(1);
		else {
			logger.warn(this + ": couldn't extract signature parameters for youtube video URL, guessing");
			sp = "signature";
		}
		url = url + "&" + sp + "=" + URLEncoder.encode(s, StandardCharsets.UTF_8);
		logger.debug("QX final stream url: " + url);
		return url;
	}
	
	private String pickUrl(String urlMap, String fmt, String jsUrl) {
		logger.debug("QX entered pickUrl with urlMap: " + urlMap + ", fmt: " + fmt + ", jsUrl: " + jsUrl);
		for(String stream : urlMap.split(",")) {
			if(stream.isEmpty()) continue;
			logger.debug("QX found stream: " + stream);
			if(fmt == null) return this.streamUrl(stream, jsUrl);
			
			Matcher itagMatcher = ITAG_EXTRACT.matcher(stream);
			if(!itagMatcher.find()) return this.streamUrl(stream, jsUrl);
			
			String itag = itagMatcher.group(1);
			if(Integer.parseInt(fmt) == Integer.parseInt(itag)) return this.streamUrl(stream, jsUrl);
		}
		return null;
	}
	
	private String pickStream(String path, String streamMap, String jsUrl) {
		String pick = null;
		String fmtString = URLUtils.getUrlParam(path, "fmt");
		if(fmtString != null) {
			logger.debug("QX found fmt string: " + fmtString);
			int fmt = Integer.parseInt(fmtString);
			for(String stream : STREAM_MAP.matcher(streamMap).results()
					.map(result -> result.group(1)).collect(Collectors.toList())) {
				int itag = STREAM_ITAG.matcher(stream).results()
						.map(result -> Integer.parseInt(result.group(1)))
						.findFirst().orElse(-1);
				if(fmt == itag) {
					pick = stream;
					break;
				}
			}
		} else {
			logger.debug("QX checking resolution of streams");
			int bestRes = -1;
			for(String stream : STREAM_MAP.matcher(streamMap).results()
					.map(result -> result.group(1)).collect(Collectors.toList())) {
				int height = HEIGHT_EXTRACT.matcher(stream).results()
						.map(result -> Integer.parseInt(result.group(1)))
						.findFirst().orElse(-1);
				logger.debug("QX found height: " + height);
				if((pick == null) || (height != -1 && bestRes == -1) || 
						(height != -1 && height > bestRes && (this.prefRes < 0 || this.prefRes >= height)) ||
						(height != -1 && this.prefRes > -1 && bestRes > this.prefRes && bestRes > height)) {
					bestRes = height;
					pick = stream;
				}
			}
		}
		logger.debug("QX picked stream: " + pick + ", checking cipher");
		if(pick == null) return null;
		String cipher = getCaptureOrNull(SIG_CIPHER, pick);
		if(cipher == null) cipher = getCaptureOrNull(SIG_CIPHER_BACKUP, pick);
		if(cipher != null) {
			logger.debug("QX found cipher");
			String url = this.streamUrl(cipher, jsUrl);
			if(url != null) return url;
		}
		return getCaptureOrNull(STREAM_URL_EXTRACT, pick);
	}
	
	@Override
	public String parse(String youtubeLink) {
		try {
			return this.parse(new ParsedURL(youtubeLink));
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	public boolean probe(String potentialLink) {
		try {
			return this.probe(new ParsedURL(potentialLink));
		} catch (MalformedURLException e) {
			return false;
		}
	}

	/**
	 * used to test if a given string is a valid youtube link
	 * @param url the url to check
	 * @return true if the given url is a parseable youtube link, false otherwise
	 */
	private boolean probe(ParsedURL url) {
		String access = url.getAccess();
		if(!access.equals("http") && !access.equals("https")) return false;
		String path = url.getPath();
		boolean content = WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(path).find())
				|| path.contains("/get_video_info?")
				|| path.contains("/v/") || path.contains("/embed/");
		return ((PROBE_START.matcher(path).find() && content) 
				|| CONSENT.matcher(path).find() 
				|| YOUTUBE_SHORTENED.matcher(path).find());
	}
	
	private String parse(ParsedURL parsedURL) throws IOException {
		String path = parsedURL.getPath();
		logger.debug("QX parsing " + parsedURL);
		if(CONSENT.matcher(path).find()) {
			logger.debug("QX consent");
			String newPath = URLUtils.getUrlParam(path, "continue");
			if(newPath == null) {
				logger.error(this + ": couldn't handle youtube consent cookie redirection");
				return null;
			} else {
				/*
				 * in original script, a request is made with cookies disabled to avoid consent redirect
				 * not sure how to handle that here so i'll just cross that bridge when (if?) i come to it
				 */
				return this.parse(URLDecoder.decode(newPath, StandardCharsets.UTF_8));
			}
		} else if (YOUTUBE_SHORTENED.matcher(path).find()) {
			logger.debug("QX youtube shortened url detected");
			String videoId = getCaptureOrNull(YOUTUBE_SHORTENED_EXTRACT, path);
			if(videoId == null) {
				logger.error(this + ": couldn't extract video id from youtu.be url");
				return null;
			} else {
				return this.parse(new ParsedURL(parsedURL.getAccess() + "://www.youtube.com/watch?v=" + videoId
						+ URLUtils.copyUrlParam(path, "fmt")));
			}
		} else if (!STANDARD_YOUTUBE.matcher(path).find()) {
			try {
				logger.debug("QX try again with www.youtube.com");
				return this.parse(new ParsedURL(parsedURL.getAccess() + "://" + PATH_START.matcher(path).replaceAll("www.youtube.com/")));
			} catch (MalformedURLException e) {
				throw new AssertionError(e);
			}
		} else if (WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(path).find())) {
			logger.debug("QX found watch patterns");
			String jsUrl = null;
			String fmt = URLUtils.getUrlParam(path, "fmt");
			boolean newLayout = false;
			String newPath = null;
			URLConnection connection = parsedURL.getURLObject().openConnection();
			connection.setConnectTimeout(this.timeout);
			connection.setReadTimeout(this.timeout);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				while(true) {
					String line = reader.readLine();
					if(line == null) break;
					
					if(PLAYER_API.matcher(line).find()) {
						line = reader.readLine();
						if(line == null) break;
					}
					
					if(!newLayout && line.contains("<script nonce=\"")) {
						logger.debug(this + ": detected new youtube HTML code layout");
						newLayout = true;
					}
					if(jsUrl == null) {
						jsUrl = getCaptureOrNull(JS_URL_EXTRACT, line);
						if(jsUrl == null) 
							jsUrl = getCaptureOrNull(JS_EXTRACT, line);
						if(jsUrl != null) {
							jsUrl = UNESCAPE.matcher(jsUrl).replaceAll("/");
							if(JS_LOCAL_PATH.matcher(jsUrl).find()) {
								jsUrl = "//" + parsedURL.getAuthority() + jsUrl;
							}
							jsUrl = JS_PLACEHOLDER_PATH_START.matcher(jsUrl).replaceAll(parsedURL.getAccess() + "://");
						}
						if(jsUrl != null)
							logger.debug("QX found jsUrl: " + jsUrl);
					}
					
					if(line.contains("ytplayer.config")) {
						if(fmt == null) {
							String fmtList = getCaptureOrNull(FMT_LIST_EXTRACT, line);
							if(fmtList != null) {
								fmtList = UNESCAPE.matcher(fmtList).replaceAll("/");
								fmt = this.getFmt(fmtList);
								logger.debug("QX obtained fmt: " + fmt);
							}
						}
						String urlMap = getCaptureOrNull(URL_ENCODED_FMT_MAP_EXTRACT, line);
						if(urlMap != null) {
							logger.debug(this + ": found classic parameters for youtube video stream, parsing...");
							urlMap = replaceUnicodeAmpersands(urlMap);
							urlMap = UNICODE_AMPERSAND.matcher(urlMap).replaceAll("&");
							newPath = this.pickUrl(urlMap, fmt, jsUrl);
							logger.debug("QX obtained newPath via urlMap: " + newPath);
						}
						if(newPath == null) {
							String streamMap = getCaptureOrNull(FORMATS_ESCAPED_EXTRACT, line);
							if(streamMap != null) {
								streamMap = REDUNDANT_ESCAPE.matcher(streamMap).replaceAll("$1");
							} else {
								streamMap = getCaptureOrNull(FORMATS_EXTRACT, line);
							}
							if(streamMap != null) {
								logger.debug(this + ": found new-style parameters for youtube video stream, parsing...");
								streamMap = replaceUnicodeAmpersands(streamMap);
								newPath = pickStream(path, streamMap, jsUrl);
								logger.debug("QX obtained newPath via streamMap: " + newPath);
							}
						}
						if(newPath == null) {
							String hlsvp = getCaptureOrNull(HLS_MANIFEST_ESCAPED_EXTRACT, line);
							if(hlsvp == null) 
								hlsvp = getCaptureOrNull(HLS_MANIFEST_EXTRACT, line);
							if(hlsvp != null) {
								hlsvp = UNESCAPE.matcher(hlsvp).replaceAll("/");
								newPath = hlsvp;
								logger.debug("QX obtained newPath via hlsvp: " + newPath);
							}
						}
					}
				}
			}
			
			if(newPath == null) {
				String videoId = URLUtils.getUrlParam(path, "v");
				if(videoId != null) {
					newPath = parsedURL.getAccess() + "://www.youtube.com/get_video_info?video_id=" + videoId + URLUtils.copyUrlParam(path, "fmt");
					if(jsUrl != null)
						newPath = newPath + "&jsurl=" + URLEncoder.encode(jsUrl, StandardCharsets.UTF_8);
					logger.warn(this + ": couldn't extract video URL, falling back to alternate youtube API");
				}
			}
			if(newPath == null) {
				this.logGeneralError();
				return null;
			}
			/*
			 * once we have new url, it's either going to be a url to another youtube
			 * page from which we can get more information leading to the direct
			 * video link, or it's going to be the direct video link. in the former 
			 * case, probe(url) will return true, and in the latter, i think probe(url)
			 * will return false, so we make this check to determine if we need to do
			 * another iteration of parse()
			 */
			ParsedURL newParsedURL = new ParsedURL(newPath);
			if(this.probe(newParsedURL)) {
				logger.debug("QX probe passed on new url: " + newParsedURL);
				return this.parse(newParsedURL);
			} else {
				return newPath;
			}
		} else if (path.contains("/get_video_info?")) {
			logger.debug("QX found get_video_info");
			URLConnection connection = parsedURL.getURLObject().openConnection();
			connection.setConnectTimeout(this.timeout);
			connection.setReadTimeout(this.timeout);
			String newPath = null;
			try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
				/*
				 * the original lua script reads 1048576 characters into a single line
				 * idk why but thats what i do
				 */
				char[] buffer = new char[1050000];
				if(reader.read(buffer, 0, 1048576) == -1) {
					logger.error(this + ": youtube API output missing");
					return null;
				}
				String line = new StringBuilder().append(buffer).toString().trim();
				String jsUrl = URLUtils.getUrlParam(path, "jsurl");
				if(jsUrl != null) jsUrl = URLDecoder.decode(jsUrl, StandardCharsets.UTF_8);
				logger.debug("QX attempted to find jsUrl, result: " + jsUrl);
				String fmt = URLUtils.getUrlParam(path, "fmt");
				if(fmt == null) {
					String fmtList = getCaptureOrNull(FMT_LIST_URL_EXTRACT, line);
					if(fmtList != null) {
						fmtList = URLDecoder.decode(fmtList, StandardCharsets.UTF_8);
						fmt = this.getFmt(fmtList);
					}
				}
				logger.debug("QX attempted to find fmt, result: " + fmt);
				String urlMap = getCaptureOrNull(URL_ENCODED_FMT_MAP_URL_EXTRACT, line);
				if(urlMap != null) {
					logger.debug(this + ": found classic parameters for youtube video stream, parsing...");
					urlMap = URLDecoder.decode(urlMap, StandardCharsets.UTF_8);
					newPath = this.pickUrl(urlMap, fmt, jsUrl);
				}
				if(newPath == null) {
					String streamMap = getCaptureOrNull(FORMATS_URL_EXTRACT, line);
					if(streamMap != null) {
						logger.debug(this + ": found new-style parameters for youtube video stream, parsing...");
						streamMap = URLDecoder.decode(streamMap, StandardCharsets.UTF_8);
						streamMap = replaceUnicodeAmpersands(streamMap);
						newPath = this.pickStream(newPath, streamMap, jsUrl);
					}
				}
				if(newPath == null) {
					String hlsvp = getCaptureOrNull(HLS_MANIFEST_URL_EXTRACT, line);
					if(hlsvp != null) {
						hlsvp = URLDecoder.decode(hlsvp, StandardCharsets.UTF_8);
						newPath = hlsvp;
						logger.debug("QX found newPath via hlsvp, newPath: " + newPath);
					}
				}
				if(newPath == null && !URLUtils.getUrlParam(path, "el").equals("detailpage")) {
					String videoId = URLUtils.getUrlParam(path, "video_id");
					if(videoId != null) {
						newPath = parsedURL.getAccess() + "://www.youtube.com/get_video_info?video_id=" + videoId 
								+ "&el=detailpage" + URLUtils.copyUrlParam(path, "fmt") + URLUtils.copyUrlParam(path, "jsurl");
						logger.warn(this + ": couldn't extract video URL, retrying with alternate youtube API parameters");
					}
				}
				if(newPath == null) {
					this.logGeneralError();
					return null;
				}
				//see comment higher up about why we do another probe check here
				ParsedURL newParsedURL = new ParsedURL(newPath);
				if(this.probe(newParsedURL)) {
					logger.debug("QX passed probe check with new parsed url: " + newParsedURL);
					return this.parse(newParsedURL);
				} else {
					return newPath;
				}
			}
		} else {
			String videoId = getCaptureOrNull(VIDEO_ID_EXTRACT, path);
			if(videoId == null) {
				logger.error(this + ": couldn't extract youtube video URL");
				return null;
			}
			logger.debug("QX making another parse attempt with found video id: " + videoId);
			return parse(new ParsedURL(parsedURL.getAccess() + "://www.youtube.com/watch?v=" + videoId + URLUtils.copyUrlParam(path, "fmt")));
		}
	}
	
	public int getTimestamp(String youtubeLink) {
		try {
			Matcher matcher = TIMESTAMP_FORMATTED_EXTRACT.matcher(youtubeLink);
			if(matcher.find()) {
				int timestamp = 0;
				for(int i=1; i <= matcher.groupCount(); i++) {
					String result = matcher.group(i);
					if(result == null) continue;
					result = result.toLowerCase();
					/*
					 * each captured group is <numbers><'s', 'm', or 'h'>
					 * h indicates hours, so obtain the number and multiply by 3600 for seconds
					 * m indicates minutes, so obtain the number and multiply by 60 for minutes
					 * s indicates seconds, so obtain the number
					 */
					if(result.endsWith("h"))
						timestamp += Math.multiplyExact(Integer.parseInt(result.substring(0, result.length() - 1)), 3600);
					else if (result.endsWith("m"))
						timestamp += Math.multiplyExact(Integer.parseInt(result.substring(0, result.length() - 1)), 60);
					else if (result.endsWith("s"))
						timestamp += Integer.parseInt(result.substring(0, result.length() - 1));
					else 
						throw new AssertionError("parsed formatted timestamp without proper formatting? link: " + youtubeLink);
				}
				return timestamp;
			} else {
				String extract = getCaptureOrNull(TIMESTAMP_EXTRACT, youtubeLink);
				if(extract != null)
					return Integer.parseInt(extract);
				else
					 return -1;
			}
		} catch (NumberFormatException | ArithmeticException e) {
			//could happen with unrealistically large values as timestamp parameter
			//just pretend it doesn't exist
			return -1;
		}
	}
	
	/**
	 * matches the given Pattern against the given String. if at least one result is found, 
	 * then the capture from group 1 of the result is returned. if no match is found, then
	 * null is returned. if the given pattern doesn't have at least one capturing group, 
	 * an exception is thrown
	 * @param pattern the Pattern to check 
	 * @param input the String to check against the given Pattern
	 * @return if the given pattern has at least one match against the given string, the
	 * capture from group 1 of the first matchresult is returned. if no matches are
	 * found, null is returned
	 * @throws IndexOutOfBoundsException if the given pattern does not contain at least one 
	 * capturing group and a match is found
	 */
	private static String getCaptureOrNull(Pattern pattern, String input) throws IndexOutOfBoundsException {
		return pattern.matcher(input).results().map(result -> result.group(1)).findFirst().orElse(null);
	}
	
	/**
	 * replaces any occurrences of "\u0026" in the input string with "&"
	 * @param input the string to replace occurrences of "\u0026" in
	 * @return the input string with all instances of "\u0026" replaced
	 * with "&"
	 */
	private static String replaceUnicodeAmpersands(String input) {
		return UNICODE_AMPERSAND.matcher(input).replaceAll("&");
	}
	
	private void logGeneralError() {
		logger.error(this + ": couldn't process youtube video URL");
	}
	
	/*
	 * might need this for later idk
	 */
	@SuppressWarnings("unused")
	private static String URIDecode(String toDecode) throws URISyntaxException {
		return URLDecoder.decode(toDecode.replace("+", "%2B"), StandardCharsets.UTF_8);
	}
	
	@Override
	public String toString() {
		return "YoutubeParserImpl";
	}
}
