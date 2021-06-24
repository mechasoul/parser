package youtube;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

	public static void main(String[] args) throws MalformedURLException {
		
		String str = "https://www.youtube.com/watch?v=jMV-aN634ZQ&ab_channel=MechaSoul";
		URL url = new URL(str);
		String access = url.getProtocol();
		StringBuilder pathBuilder = new StringBuilder(url.getAuthority());
		pathBuilder.append(url.getPath());
		if(url.getQuery() != null) {
			pathBuilder.append("?");
			pathBuilder.append(url.getQuery());
		}
		
		Pattern unescapePattern = Pattern.compile("\\\\/");
		String test = "yes\\/what is\\/this";
		String test2 = "\\\" ddasd \\\\ dasda a\\/d ada ";
		Pattern pattern2 = Pattern.compile("\\\\([\"\\\\\\/])");
		
		Pattern height = Pattern.compile("\"height\":(\\d+)");
		String heightTest = "\"height\":56483";
		
		Pattern bug = Pattern.compile("(..):function\\([^)]*\\)\\{([^}]*)\\}");
		
		System.out.println(test);
		System.out.println(unescapePattern.matcher(test).replaceAll("/"));
		
		System.out.println(test2);
		pattern2.matcher(test2).results().forEach(result -> System.out.println(result.group(1)));
		System.out.println(pattern2.matcher(test2).replaceAll("b"));
		System.out.println(pattern2.matcher(test2).replaceAll("$1"));
		System.out.println(heightTest);
		height.matcher(heightTest).results().forEach(result -> System.out.println(result.group(1)));
	}

}
