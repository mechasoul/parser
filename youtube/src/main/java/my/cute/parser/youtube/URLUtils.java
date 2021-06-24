package my.cute.parser.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {

	/**
	 * attempts to extract a url parameter value from a given url. ie, given a parameter
	 * <code>param</code>, searches for the first instance of 
	 * <pre>{any character in the set [&|?]}param=({any character that isn't &, as many times as possible})</pre>
	 * extracting and returning the group value. if no match is found, returns null
	 * @param url the url to search for the parameter in
	 * @param param the name of the parameter whose value should be extracted
	 * @return the value of the given parameter in the given url, or null if the given parameter couldn't be 
	 * found in the given url
	 */
	public static String getUrlParam(String url, String param) {
		Matcher matcher = Pattern.compile("(?:&|\\?)" + param + "=([^&]*)").matcher(url);
		if(matcher.find()) 
			return matcher.group(1);
		else
			return null;
	}
	
	/**
	 * helper method for appending url parameters from one url to another. given a url and
	 * a parameter name, this will return a string representing the given parameter and its value,
	 * ready to be appended to the end of a url. specifically, returns the string
	 * <pre>&name=value</pre>
	 * where <code>name</code> is the name of the parameter to be copied (as given by <code>String 
	 * param</code>) and <code>value</code> is the value of that parameter in the given url
	 * (as obtained by {@link #getUrlParam(String, String)})
	 * <p>
	 * if the given parameter name is missing from the given url, this returns the empty string
	 * @param url the url to copy a parameter from
	 * @param param the name of the parameter to copy from the given url
	 * @return the string <code>&name=value</code> where <code>name</code> is given by the 
	 * parameter <code>param</code> for this method and <code>value</code> is obtained from
	 * {@link #getUrlParam(String, String)}, or the empty string if the parameter <code>param</code>
	 * couldn't be found in the given url (ie, {@link #getUrlParam(String, String)} returned null)
	 */
	public static String copyUrlParam(String url, String param) {
		String value = getUrlParam(url, param);
		if(value == null)
			return "";
		else
			return "&" + param + "=" + value;
	}
	
}
