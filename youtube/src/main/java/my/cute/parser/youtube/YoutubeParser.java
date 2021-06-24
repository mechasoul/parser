package my.cute.parser.youtube;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * parses youtube pages into direct video source links (ie googlevideo hosts)
 * TODO youtu.be links?
 * TODO can obs play .m3u8 link? (result of parsing livestream. rly not necessary and
 * 		probably shouldnt even allow it in the first place, but)
 */
public interface YoutubeParser {
	
	/**
	 * checks if the given String represents a valid youtube video. returns true if
	 * it does, and false otherwise
	 * @param potentialLink a String that may be a link to a youtube video
	 * @return true if the given string is a valid youtube video link, false otherwise
	 * @throws MalformedURLException 
	 */
	public boolean probe(String potentialLink);

	/**
	 * parses a given string as a youtube link. if the given string is a valid youtube link,
	 * this will return a direct video link to the video in question
	 * @param youtubeLink the youtube link to parse
	 * @return a string representing a direct link to the source video if the given string is
	 * a valid youtube link. if the string is not a valid youtube link (or something goes 
	 * wrong), returns null
	 */
	public String parse(String youtubeLink);
	
	/**
	 * given a youtube link, extracts the url-embedded timestamp in the given link,
	 * returning the corresponding time in seconds. works on both raw seconds timestamps
	 * (eg <code>&t=60</code>) as well as formatted hours/minutes/seconds timestamps 
	 * (eg <code>&t=1h30m45s</code>)<p>
	 * note no checks are made to see if the given link is actually a valid youtube link
	 * or not; this method really just checks for a "<code>&t=</code>" url parameter and
	 * makes any necessary conversion if it's formatted. consequently this method will
	 * return some "valid" (ie, not -1) value on an input such as 
	 * <code>www.butts.com/butts?t=346</code>. if this is undesirable, make sure to check
	 * the link first with {@link #probe(String)}
	 * @param youtubeLink the link to extract a timestamp from
	 * @return the number of seconds corresponding to the timestamp in the given link,
	 * or -1 if no timestamp could be found
	 */
	public int getTimestamp(String youtubeLink);
	
	/*
	 * ok so the bulk of what the parser does is just string pattern matching in order to extract youtube
	 * page information, url parameters, etc. consequently there are several dozen patterns being used
	 * here and i'm not even sure what most of the original lua script was doing AND lua doesn't even
	 * use typical regex it does its own thing so some of this regex stuff i'm not 100% sure on
	 * 
	 * ive made a best effort to try to explain what the different patterns are matching. in docs for the
	 * patterns i list the actual pattern and then my attempt to explain what it's actually matching. i 
	 * use < > to indicate special characters or character sets or whatever and i escape characters that would
	 * need to normally be escaped in a regex string. this may not be 100% consistent so i hope you understand
	 * the gist. goodluck
	 */
	
	public static YoutubeParser createDefault() {
		return new YoutubeParserImpl();
	}

	/**
	 * (\\d+)\\/\\d+x(\\d+)[^,]*<br>
	 * (&lt;any number at least once&gt;)\/&lt;any number at least once&gt;x(&lt;any number at least once&gt;)&lt;any character that isnt ',' 0+ times as many as possible&gt;
	 */
	static final Pattern GET_FMT = Pattern.compile("(\\d+)\\/\\d+x(\\d+)[^,]*");

	/**
	 * [=\\(,&\\|](..)\\(decodeURIComponent\\(.\\.s\\)\\)<br>
	 * &lt;any character '=', '(', ',', '&', '|'&gt;(&lt;any 2 characters&gt;)\(decodeURIComponent\(&lt;any character&gt;\.s\)\)
	 */
	static final Pattern DESCRAMBLER = Pattern.compile("[=\\(,&\\|](..)\\(decodeURIComponent\\(.\\.s\\)\\)");
	/**
	 * ;(..)\\...\\(<br>
	 * ;(&lt;any 2 characters&gt;)\.&lt;any 2 characters&gt;\(
	 */
	static final Pattern DESCRAMBLE_HELPER = Pattern.compile(";(..)\\...\\(");
	/**
	 * (..):function\\([^)]*\\)\\{([^}]*)\\}<br>
	 * (&lt;any 2 characters&gt;):function\(&lt;any character that isnt ')', 0+ times as many as possible&gt;\)\{(&lt;any character that isnt '}', 0+ times as many as possible&gt;)\}
	 */
	static final Pattern DESCRAMBLE_TRANS = Pattern.compile("(..):function\\([^)]*\\)\\{([^}]*)\\}");
	/**
	 * ..\\.(..)\\([^,]+,(\\d+)\\)<br>
	 * &lt;any 2 characters&gt;\.(&lt;any 2 characters&gt;)\(&lt;any character that isnt ',' 1 or more times&gt;,(&lt;any numbers 1 or more times&gt;)\)
	 */
	static final Pattern DESCRAMBLE_INDEX = Pattern.compile("..\\.(..)\\([^,]+,(\\d+)\\)");

	/**
	 * url=([^&]+)<br>
	 * url=(&lt;any character that isnt &, 1 or more times&gt;)
	 */
	static final Pattern URL_EXTRACT = Pattern.compile("url=([^&]+)");
	/**
	 * s=([^&]+)<br>
	 * s=(&lt;any character that isnt &, 1 or more times&gt;)
	 */
	static final Pattern S_EXTRACT = Pattern.compile("s=([^&]+)");
	/**
	 * sp=([^&]+)<br>
	 * sp=(&lt;any character that isnt &, 1 or more times&gt;)
	 */
	static final Pattern SP_EXTRACT = Pattern.compile("sp=([^&]+)");

	/**
	 * itag=(\\d+)<br>
	 * itag=(&lt;numbers, 1+ times&gt;)
	 */
	static final Pattern ITAG_EXTRACT = Pattern.compile("itag=(\\d+)");
	/**
	 * \\{(.*?)\\}<br>
	 * \{&lt;any characters 0+ times, as few as possible&gt;\}
	 */
	static final Pattern STREAM_MAP = Pattern.compile("\\{(.*?)\\}");
	/**
	 * \"itag\":(\\d+)<br>
	 * \"itag\":(&lt;numbers at least once&gt;)
	 */
	static final Pattern STREAM_ITAG = Pattern.compile("\"itag\":(\\d+)");
	/**
	 * \"height\":(\\d+)<br>
	 * \"height\":(&lt;numbers at least once&gt;)
	 */
	static final Pattern HEIGHT_EXTRACT = Pattern.compile("\"height\":(\\d+)");
	/**
	 * \"signatureCipher\":\"(.*?)\"<br>
	 * \"signatureCipher\":\"(&lt;0+ characters as few as possible&gt;)\"
	 */
	static final Pattern SIG_CIPHER = Pattern.compile("\"signatureCipher\":\"(.*?)\"");
	/**
	 * \"[a-zA-Z]*[Cc]ipher\":\"(.*?)\"<br>
	 * \"&lt;letters as many as possible&gt;&lt;C or c&gt;ipher\":\"(&lt;0+ characters as few as possible&gt;)\"
	 */
	static final Pattern SIG_CIPHER_BACKUP = Pattern.compile("\"[a-zA-Z]*[Cc]ipher\":\"(.*?)\"");
	/**
	 * \"url\":\"(.*?)\"<br>
	 * \"url\":\"(&lt;0+ characters as few as possible&gt;)\"
	 */
	static final Pattern STREAM_URL_EXTRACT = Pattern.compile("\"url\":\"(.*?)\"");

	/**
	 * ^(?:www|music|gaming)\\.youtube\\.com<br>
	 * &lt;start&gt;&lt;"www" or "music" or "gaming"&gt;\.youtube\.com
	 */
	static final Pattern PROBE_START = Pattern.compile("^(?:www|music|gaming)\\.youtube\\.com");
	/*
	 * below could probably just be a single regex eg \\/(?:watch\\?|live$|live\\?) or something?
	 */
	/**
	 * unlike other patterns, these are constantly lumped together. use with 
	 * <pre>WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matches(input).find)</pre>
	 * denotes a youtube video watch page. not thread safe?? (??)<p>
	 * \\/watch\\?<br>
	 * \\/live(?:$|\\?)
	 * <p>
	 * \/watch\?<br>
	 * \/live&lt;end of line or '?'&gt;
	 */
	static final List<Pattern> WATCH_PATTERNS = Collections.unmodifiableList(
			List.of(Pattern.compile("\\/watch\\?"), Pattern.compile("\\/live(?:$|\\?)")
					)
			);
	/**
	 * ^consent\\.youtube\\.com\\/<br>
	 * &lt;start&gt;consent\.youtube\.com\/
	 */
	static final Pattern CONSENT = Pattern.compile("^consent\\.youtube\\.com\\/");

	/**
	 * ^www\\.youtube\\.com\\/<br>
	 * &lt;start&gt;www\.youtube\.com\/
	 */
	static final Pattern STANDARD_YOUTUBE = Pattern.compile("^www\\.youtube\\.com\\/");
	/**
	 * ^[^\\/]*\\/<br>
	 * &lt;start&gt;(&lt;any character that isnt '/' 0+ times, as many as possible&gt;)\/
	 */
	static final Pattern PATH_START = Pattern.compile("^[^\\/]*\\/");
	/**
	 * ^ *&lt;div id=\"player-api\"&gt;<br>
	 * &lt;start&gt;&lt;whitespace 0+ times, as many as possible&gt;\&lt;div id=\"player-api\"\&gt;
	 */
	static final Pattern PLAYER_API = Pattern.compile("^ *<div id=\"player-api\">");
	/**
	 * \"jsUrl\":\"(.*?)\"<br>
	 * \"jsUrl\":\"(&lt;any character 0+ times, as few as possible&gt;)\"
	 */
	static final Pattern JS_URL_EXTRACT = Pattern.compile("\"jsUrl\":\"(.*?)\"");
	/**
	 * \"js\": *\"(.*?)\"<br>
	 * \"js\":&lt;whitespace 0+ times, as many as possible&gt;\"(&lt;any character 0+ times, as few as possible&gt;)\"
	 */
	static final Pattern JS_EXTRACT = Pattern.compile("\"js\": *\"(.*?)\"");
	/**
	 * \\\\/<br>
	 * \/<br>
	 * note this pattern is intended to replace instances of "\/" with "/"
	 */
	static final Pattern UNESCAPE = Pattern.compile("\\\\/");
	/**
	 * ^\\/[^\\/]<br>
	 * &lt;start&gt;/&lt;any character that isnt '/'&gt;<br>
	 * note this pattern is used to test if a string represents a local path (eg starts with "/&lt;path&gt;")
	 */
	static final Pattern JS_LOCAL_PATH = Pattern.compile("^\\/[^\\/]");
	/**
	 * ^\\/\\/<br>
	 * &lt;start&gt;\/\/
	 */
	static final Pattern JS_PLACEHOLDER_PATH_START = Pattern.compile("^\\/\\/");
	/**
	 * \"fmt_list\": *\"(.*?)\"<br>
	 * \"fmt_list\":&lt;whitespace 0+ times, as many as possible&gt;\"(&lt;any character 0+ times, as few as possible&gt;)\"
	 */
	static final Pattern FMT_LIST_EXTRACT = Pattern.compile("\"fmt_list\": *\"(.*?)\"");
	/**
	 * \"url_encoded_fmt_stream_map\": *\"(.*?)\"<br>
	 * \"url_encoded_fmt_stream_map\":&lt;whitespace 0+ times, as many as possible&gt;\"(&lt;any character 0+ times, as few as possible&gt;)\"
	 */
	static final Pattern URL_ENCODED_FMT_MAP_EXTRACT = Pattern.compile("\"url_encoded_fmt_stream_map\": *\"(.*?)\"");
	/**
	 * \\\\u0026<br>
	 * \u0026<br>
	 * note this pattern is intended to replace instances of "\u0026" with "&"
	 */
	static final Pattern UNICODE_AMPERSAND = Pattern.compile("\\\\u0026");
	/**
	 * \"formats\":\\[(.*?)\\]<br>
	 * \"formats\":\[(&lt;any character 0+ times, as few as possible&gt;)\]
	 */
	static final Pattern FORMATS_EXTRACT = Pattern.compile("\"formats\":\\[(.*?)\\]");
	/**
	 * \\\\\"formats\\\\\":\\[(.*?)\\]<br>
	 * \\\"formats\\\":\[(&lt;any character 0+ times, as few as possible&gt;)\]
	 */
	static final Pattern FORMATS_ESCAPED_EXTRACT = Pattern.compile("\\\\\"formats\\\\\":\\[(.*?)\\]");
	/**
	 * \\\\([\"\\\\\\/])<br>
	 * \\(&lt;any character '\"', '\\', '/'&gt;)<br>
	 * note this pattern is intended to replace instances of '\"', '\\', '\/' with '"', '\', '/' respectively. 
	 * can do this via <pre>REDUNDANT_ESCAPE.matcher(input).replaceAll("$1")</pre>
	 * jesus christ
	 */
	static final Pattern REDUNDANT_ESCAPE = Pattern.compile("\\\\([\"\\\\\\/])");
	/**
	 * \"hlsManifestUrl\":\"(.*?)\"<br>
	 * \"hlsManifestUrl\":\"(&lt;any character 0+ times, as few as possible&gt;)\"
	 */
	static final Pattern HLS_MANIFEST_EXTRACT = Pattern.compile("\"hlsManifestUrl\":\"(.*?)\"");
	/**
	 * \\\\\"hlsManifestUrl\\\\\": *\\\\\"(.*?)\\\\\"<br>
	 * \\\"hlsManifestUrl\\\":&lt;whitespace 0+ times, as many as possible&gt;\\\"(&lt;any character 0+ times, as few as possible&gt;)\\\"
	 */
	static final Pattern HLS_MANIFEST_ESCAPED_EXTRACT = Pattern.compile("\\\\\"hlsManifestUrl\\\\\": *\\\\\"(.*?)\\\\\"");
	/**
	 * &fmt_list=([^&]*)<br>
	 * &fmt_list=(&lt;any character that isnt '&' 0+ times, as many as possible&gt;)
	 */
	static final Pattern FMT_LIST_URL_EXTRACT = Pattern.compile("&fmt_list=([^&]*)");
	/**
	 * &url_encoded_fmt_stream_map=([^&]*)<br>
	 * &url_encoded_fmt_stream_map=(&lt;any character that isnt '&' 0+ times, as many as possible&gt;)
	 */
	static final Pattern URL_ENCODED_FMT_MAP_URL_EXTRACT = Pattern.compile("&url_encoded_fmt_stream_map=([^&]*)");
	/**
	 * %22formats%22%3A%5B(.*?)%5D<br>
	 * %22formats%22%3A%5B(&lt;any character 0+ times, as few as possible&gt;)%5D
	 */
	static final Pattern FORMATS_URL_EXTRACT = Pattern.compile("%22formats%22%3A%5B(.*?)%5D");
	/**
	 * %22hlsManifestUrl%22%3A%22(.*?)%22<br>
	 * %22hlsManifestUrl%22%3A%22(&lt;any character 0+ times, as few as possible&gt;)%22
	 */
	static final Pattern HLS_MANIFEST_URL_EXTRACT = Pattern.compile("%22hlsManifestUrl%22%3A%22(.*?)%22");
	/**
	 * \\/[^\\/]+\\/([^?]*)<br>
	 * \/&lt;any character that isnt '/' 1 or more times&gt;\/(&lt;any character that isnt '?' 0+ times, as many as possible&gt;)
	 */
	static final Pattern VIDEO_ID_EXTRACT = Pattern.compile("\\/[^\\/]+\\/([^?]*)");
	/**
	 * not from vlc script. attempts to match youtube shortened links against path<p>
	 * ^youtu\\.be\\/<br>
	 * &lt;start&gt;youtu\.be\/
	 */
	static final Pattern YOUTUBE_SHORTENED = Pattern.compile("^youtu\\.be\\/");
	/**
	 * not from vlc script. attempts to extract video id from youtube shortened link. could omit starting youtu.be part
	 * if we assume a check has been made that the path is on youtu.be? also maybe the any character 0+ times should be
	 * alphanumeric 1+ times?<p>
	 * ^youtu\\.be\\/(?:watch\\?v=)?(.*?)(?:$|\\?|&)<br>
	 * &lt;start&gt;youtu.be\/&lt;optional watch\?v=&gt;(&lt;any character 0+ times as few as possible&gt;)&lt;any of end of line, '&', '?'&gt;
	 */
	static final Pattern YOUTUBE_SHORTENED_EXTRACT = Pattern.compile("^youtu\\.be\\/(?:watch\\?v=)?(.*?)(?:$|\\?|&)");
	/**
	 * not from vlc script. checks for a timestamp present in the link<p>
	 * [?&]t=(\\d+)<br>
	 * &lt;either '?' or '&'&gt;t=&lt;(numbers 1+ times)&gt;
	 */
	static final Pattern TIMESTAMP_EXTRACT = Pattern.compile("[?&]t=(\\d+)");
	/**
	 * not from vlc script. checks for a timestamp in the link, in XXhYYmZZs format<p>
	 * [?&]t=(\\d+[hms])(\\d+[hms])?(\\d+[hms])?(?:$|&|\\||\\?)<br>
	 * &lt;either '?' or '&'&gt;t=(&lt;numbers 1+ times&gt;&lt;'h', 'm', or 's'&gt)&lt;repeat previous capture up to two more times&gt;
	 * &lt;end of line or '&', '|', '?'&gt;
	 */
	static final Pattern TIMESTAMP_FORMATTED_EXTRACT = Pattern.compile("[?&]t=(\\d+[hms])(\\d+[hms])?(\\d+[hms])?(?:$|&|\\||\\?)", Pattern.CASE_INSENSITIVE);
}
