package youtube;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static my.cute.parser.youtube.YoutubeParser.*;

import org.junit.jupiter.api.Test;

class ParserPatternTest {

	@Test
	void testGetFmt() {
		String input = "4736/222x987jjhg5674m/8(),nvt546x35";
		testPatternOnInputWithMultipleCaptures(GET_FMT, input, List.of("4736", "987"));
	}
	
	@Test
	void testDescrambler() {
		String input = "(bc(decodeURIComponent(F.s))";
		testPatternOnInputWithCapture(DESCRAMBLER, input, "bc");
	}
	
	@Test
	void testDescrambleHelper() {
		String input = "dstrtr454dsde.fg;ab.cd(hkogkj;op.lk";
		testPatternOnInputWithCapture(DESCRAMBLE_HELPER, input, "ab");
	}
	
	@Test
	void testDescrambleTrans() {
		String input = "feergfab:function(hfhh jshnfjgfyy){cd:function(f)}dfgffd";
		testPatternOnInputWithMultipleCaptures(DESCRAMBLE_TRANS, input, List.of("ab", "cd:function(f)"));
	}
	
	@Test
	void testDescrambleIndex() {
		String input = "garbageab.cd(cjhdyt8h gg,678)435gjfijgarbage";
		testPatternOnInputWithMultipleCaptures(DESCRAMBLE_INDEX, input, List.of("cd", "678"));
	}
	
	@Test
	void testUrlExtract() {
		String input = "garbageurl=&bjdbhdgurl=abcd&glgjbgb";
		testPatternOnInputWithCapture(URL_EXTRACT, input, "abcd");
	}
	
	@Test
	void testSExtract() {
		String input = "garbages=&fndhuuds=jkl&gpko";
		testPatternOnInputWithCapture(S_EXTRACT, input, "jkl");
	}
	
	@Test
	void testSPExtract() {
		String input = "garbagefkndbjsp=&vnudsp=yuiop1&kgofj";
		testPatternOnInputWithCapture(SP_EXTRACT, input, "yuiop1");
	}
	
	@Test
	void testItagExtract() {
		String input = "gabrbgitag=hello123$^&*bahitag=7891fd&gfd";
		testPatternOnInputWithCapture(ITAG_EXTRACT, input, "7891");
	}
	
	@Test
	void testStreamMap() {
		String input = "bab() uhfsgscb.ds({hello yes{(abc)};dgfe}jsi123)";
		testPatternOnInputWithCapture(STREAM_MAP, input, "hello yes{(abc)");
	}
	
	@Test
	void testStreamItag() {
		String input = "dbsshj itag:457 gndfbh \"itag\":1234d fdsds";
		testPatternOnInputWithCapture(STREAM_ITAG, input, "1234");
	}
	
	@Test
	void testHeightExtract() {
		String input = "dbsshj \"itag\":457 gndfbh height=6555 sdsa \"height\":1234d fdsds";
		testPatternOnInputWithCapture(HEIGHT_EXTRACT, input, "1234");
	}
	
	@Test
	void testSigCipher() {
		String input = "fnsdg signatureCipher:345 snjn \"signatureCipher\":\"123hello\" abcdk ndjbs";
		testPatternOnInputWithCapture(SIG_CIPHER, input, "123hello");
	}
	
	@Test
	void testSigCipherBackup() {
		String input = "abcd gfgcipher:dns cmshsj \"hellocipher\":\"help\"dffds fdfd";
		testPatternOnInputWithCapture(SIG_CIPHER_BACKUP, input, "help");
	}
	
	@Test
	void testStreamUrlExtract() {
		String input = "abdh url:123 fddfss \"url\":\"bdcf12 \" fdshsk";
		testPatternOnInputWithCapture(STREAM_URL_EXTRACT, input, "bdcf12 ");
	}
	
	@Test
	void testProbeStart() {
		testPatternOnInput(PROBE_START, "www.youtube.com fdddf");
		testPatternOnInput(PROBE_START, "gaming.youtube.com ddd");
		testPatternOnInput(PROBE_START, "music.youtube.com fdddf");
		assertFalse(PROBE_START.matcher("hello www.youtube.com").find());
	}
	
	@Test
	void testWatchPatterns() {
		assertTrue(WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher("dbdhdb /watch? fds").find()));
		assertTrue(WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher("dbdhdb /live? fds").find()));
		assertTrue(WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher("dbdhdb /live").find()));
		assertFalse(WATCH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher("dbdhdb /live fds").find()));
	}
	
	@Test
	void testConsent() {
		testPatternOnInput(CONSENT, "consent.youtube.com/fdfdg");
		assertFalse(CONSENT.matcher("consent.youtube.com").find());
		assertFalse(CONSENT.matcher("dddd consent.youtube.com/").find());
	}
	
	@Test
	void testStandardYoutube() {
		testPatternOnInput(STANDARD_YOUTUBE, "www.youtube.com/ abdhadja");
		assertFalse(STANDARD_YOUTUBE.matcher("www.youtube.com").find());
		assertFalse(STANDARD_YOUTUBE.matcher(" dada www.youtube.com/").find());
	}
	
	@Test
	void testPathStart() {
		String input = "abcd geywww.path ddd123.cc/dfsaa";
		testPatternOnInput(PATH_START, input);
	}
	
	@Test
	void testPlayerApi() {
		String input = "   <div id=\"player-api\"> adddffa";
		testPatternOnInput(PLAYER_API, input);
	}
	
	@Test
	void testJSUrlExtract() {
		String input = "abc jsUrl:123 fnaajbn \"jsUrl\":\"hello8765 \" dfsdf";
		testPatternOnInputWithCapture(JS_URL_EXTRACT, input, "hello8765 ");
	}
	
	@Test
	void testJSExtract() {
		String input = "abcav js: ddasf fgsds \"js\":   \"hello432\" dsadfa";
		testPatternOnInputWithCapture(JS_EXTRACT, input, "hello432");
	}
	
	@Test
	void testUnescape() {
		String input = "hello\\/how\\/are\\/you tod\\/ay";
		assertEquals("hello/how/are/you tod/ay", UNESCAPE.matcher(input).replaceAll("/"));
	}
	
	@Test
	void testJSLocalPath() {
		String input = "/fdsa/fsbbdd gdjis172 f& ";
		testPatternOnInput(JS_LOCAL_PATH, input);
		assertFalse(JS_LOCAL_PATH.matcher("//fgdhs gek").find());
		assertFalse(JS_LOCAL_PATH.matcher("a/ dasdfsdf f/f ss").find());
	}
	
	@Test
	void testJSPlaceholderPathStart() {
		String input = "//bdgsww.mfidjfi8dm1.rfjdi";
		testPatternOnInput(JS_PLACEHOLDER_PATH_START, input);
		assertFalse(JS_PLACEHOLDER_PATH_START.matcher("/fdww/ s a/ weee87.fdmci$").find());
		assertFalse(JS_PLACEHOLDER_PATH_START.matcher(" //fddsgt445sfd,98.d").find());
	}
	
	@Test
	void testFmtListExtract() {
		String input = "adnasv jfmt_list:123 damklnjk&jnhv \"fmt_list\":    \"helloyes7 &\" fs\"faf";
		testPatternOnInputWithCapture(FMT_LIST_EXTRACT, input, "helloyes7 &");
	}
	
	@Test
	void testUrlEncodedFmtMapExtract() {
		String input = "adnasv jurl_encoded_fmt_stream_map:123 damklnjk&jnhv \"url_encoded_fmt_stream_map\":    \"helloyes7 &\" fs\"faf";
		testPatternOnInputWithCapture(URL_ENCODED_FMT_MAP_EXTRACT, input, "helloyes7 &");
	}
	
	@Test
	void testUnicodeAmpersand() {
		String input = "abca\\u0026 dasfs\\u0026 &&af() daf\\u002 \\u0026";
		assertEquals("abca& dasfs& &&af() daf\\u002 &", UNICODE_AMPERSAND.matcher(input).replaceAll("&"));
	}
	
	@Test
	void testFormatsExtract() {
		String input = "bdahvd formats:[fa] dadf \"formats\":[defgs&123]\"dasd";
		testPatternOnInputWithCapture(FORMATS_EXTRACT, input, "defgs&123");
	}
	
	@Test
	void testFormatsEscapedExtract() {
		String input = "bdahvd \\formats\\:[fa] dadf \\\"formats\\\":[defgs&123]\\\"dasd";
		testPatternOnInputWithCapture(FORMATS_ESCAPED_EXTRACT, input, "defgs&123");
	}
	
	@Test
	void testRedundantEscape() {
		String input = "hello\\\"how\\/are\\\\you to\\\"day";
		assertEquals("hello\"how/are\\you to\"day", REDUNDANT_ESCAPE.matcher(input).replaceAll("$1"));
	}
	
	@Test
	void testHlsManifestExtract() {
		String input = "gbshjg hlsManifestUrl:dsad fnj7&() rrt \"hlsManifestUrl\":\"abcd1234 &hello()\" ghgfsd \" dadff";
		testPatternOnInputWithCapture(HLS_MANIFEST_EXTRACT, input, "abcd1234 &hello()");
	}
	
	@Test
	void testHlsManifestEscapedExtract() {
		String input = "gbshjg hlsManifestUrl:dsad fnj7&() rrt \\\"hlsManifestUrl\\\":   \\\"abcd1234 &hello()\\\" ghgfsd \\\" dadff";
		testPatternOnInputWithCapture(HLS_MANIFEST_ESCAPED_EXTRACT, input, "abcd1234 &hello()");
	}
	
	@Test
	void testFmtListUrlExtract() {
		String input = "sbdhvawww.wahetvr.com/dsfha &fmtlist= dmsan &fmt_list=stuffhere123&fsf";
		testPatternOnInputWithCapture(FMT_LIST_URL_EXTRACT, input, "stuffhere123");
	}
	
	@Test
	void testUrlEncodedFmtMapUrlExtract() {
		String input = "sbdhvawww.wahetvr.com/dsfha &url_encoded_fmt_stream_map= dmsan &url_encoded_fmt_stream_map=stuffhere123&fsf";
		testPatternOnInputWithCapture(URL_ENCODED_FMT_MAP_URL_EXTRACT, input, " dmsan ");
	}
	
	@Test
	void testFormatsUrlExtract() {
		String input = "dabhjd fj%22fnfjs %22formats%22%3A%5Bhelloyes&123%5Dabc%5D dsad";
		testPatternOnInputWithCapture(FORMATS_URL_EXTRACT, input, "helloyes&123");
	}
	
	@Test
	void testHlsManifestUrlExtract() {
		String input = "dabhjd fj%22fnfjs ddd%22hlsManifestUrl%22%3A%22hellofriend555&1%22abc%22 dsad";
		testPatternOnInputWithCapture(HLS_MANIFEST_URL_EXTRACT, input, "hellofriend555&1");
	}
	
	@Test
	void testVideoIdExtract() {
		String input = "dfs/abcfhjs12&832?gd/abhcvh274616b?fgds";
		testPatternOnInputWithCapture(VIDEO_ID_EXTRACT, input, "abhcvh274616b");
	}
	
	@Test
	void testYoutubeShortened() {
		String input = "youtu.be/TZPH9tGjchI";
		testPatternOnInput(YOUTUBE_SHORTENED, input);
	}
	
	@Test
	void testYoutubeShortenedExtract() {
		testPatternOnInputWithCapture(YOUTUBE_SHORTENED_EXTRACT, "youtu.be/TZPH9tGjchI", "TZPH9tGjchI");
		testPatternOnInputWithCapture(YOUTUBE_SHORTENED_EXTRACT, "youtu.be/watch?v=TZPH9tGjchI", "TZPH9tGjchI");
		testPatternOnInputWithCapture(YOUTUBE_SHORTENED_EXTRACT, "youtu.be/TZPH9tGjchI&t=60s", "TZPH9tGjchI");
		testPatternOnInputWithCapture(YOUTUBE_SHORTENED_EXTRACT, "youtu.be/watch?v=TZPH9tGjchI&t=60s", "TZPH9tGjchI");
	}
	
	@Test
	void testTimestampExtract() {
		String input = "sfgd dgfsfa&t=4552265 gddd";
		testPatternOnInputWithCapture(TIMESTAMP_EXTRACT, input, "4552265");
		testPatternOnInputWithCapture(TIMESTAMP_EXTRACT, "sfgd dgfsfa&t=2244? gddd", "2244");
	}
	
	@Test
	void testTimestampFormattedExtract() {
		testPatternOnInputWithMultiplePossiblyMissingCaptures(TIMESTAMP_FORMATTED_EXTRACT, "youtu.be/6KEnzhHQhoo?t=1h23240m311111s&fmt=fdsas",
				List.of("1h", "23240m", "311111s"));
		testPatternOnInputWithMultiplePossiblyMissingCaptures(TIMESTAMP_FORMATTED_EXTRACT, "adfdsf?t=1000m20s&daffw", List.of("1000m", "20s"));
		testPatternOnInputWithMultiplePossiblyMissingCaptures(TIMESTAMP_FORMATTED_EXTRACT, "asdfewafa?t=8765s", List.of("8765s"));
		testPatternOnInputWithMultiplePossiblyMissingCaptures(TIMESTAMP_FORMATTED_EXTRACT, "fsadsafewq&t=6h43m260s|ghgee", 
				List.of("6h", "43m", "260s"));
		testPatternOnInputWithMultiplePossiblyMissingCaptures(TIMESTAMP_FORMATTED_EXTRACT, "adfdsf&t=1000m20s?aaa=bbb", List.of("1000m", "20s"));
		assertFalse(TIMESTAMP_FORMATTED_EXTRACT.matcher("asdfewafa&t=8765s7464gd").find());
		assertFalse(TIMESTAMP_FORMATTED_EXTRACT.matcher("asdfewafa&t=87657464").find());
	}
	
	private void testPatternOnInput(Pattern pattern, String input) {
		Matcher matcher = pattern.matcher(input);
		assertTrue(matcher.find());
	}
	
	private void testPatternOnInputWithCapture(Pattern pattern, String input, String expectedCapture) {
		Matcher matcher = pattern.matcher(input);
		assertTrue(matcher.find());
		assertEquals(1, matcher.groupCount());
		assertNotNull(matcher.group(1));
		assertEquals(expectedCapture, matcher.group(1));
	}
	
	private void testPatternOnInputWithMultipleCaptures(Pattern pattern, String input, List<String> expectedCaptures) {
		Matcher matcher = pattern.matcher(input);
		assertTrue(matcher.find());
		assertEquals(expectedCaptures.size(), matcher.groupCount());
		for(int i=1; i <= expectedCaptures.size(); i++) {
			assertNotNull(matcher.group(i));
			assertEquals(expectedCaptures.get(i-1), matcher.group(i));
		}
	}
	
	/**
	 * ok this is probably too complex for a test method but basically we have a pattern and an input 
	 * and a list of expected capture results, just like {@link #testPatternOnInputWithMultipleCaptures(Pattern, String, List)},
	 * except we allow for some of the results of the match to be missing, ie some of the captures to be null. 
	 * this test should pass as long as the match results <b>with nulls omitted</b> are a perfect match with the given
	 * expected captures. eg expected captures {1, 2, 3} passes with results 
	 * <pre>{1, 2, 3}<br>
	 * {1, null, 2, null, 3, null}<br>
	 * {null, null, 1, 2, 3}<br>
	 * {1, 2, 3, null}<br>
	 * {1, 2, null, 3}<br><pre>
	 * etc. test will fail with any results that aren't found in expectedCaptures, or if the results are found
	 * out of order, or etc
	 * @param pattern pattern under test
	 * @param input test input
	 * @param expectedCaptures the nonnull results that should be found from the match. order matters
 	 */
	private void testPatternOnInputWithMultiplePossiblyMissingCaptures(Pattern pattern, String input, List<String> expectedCaptures) {
		Matcher matcher = pattern.matcher(input);
		assertTrue(matcher.find());
		MatchResult result = matcher.toMatchResult();
		assertTrue(matcher.groupCount() >= expectedCaptures.size());
		int groupIndex = 1;
		int listIndex = 0;
		while(listIndex < expectedCaptures.size()) {
			if(result.group(groupIndex) != null) {
				assertEquals(expectedCaptures.get(listIndex), result.group(groupIndex));
				listIndex++;
			}
			groupIndex++;
		}
		while(groupIndex <= matcher.groupCount()) {
			assertNull(matcher.group(groupIndex));
			groupIndex++;
		}
	}

}
