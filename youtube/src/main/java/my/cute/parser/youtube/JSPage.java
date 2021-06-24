package my.cute.parser.youtube;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

interface JSPage extends AutoCloseable {

	/**
	 * attempts to match the given pattern against the source js of this JSPage. if a match is found, 
	 * the first capturing group in the pattern provided is extracted and returned. if no match is 
	 * found, null is returned
	 * @param pattern the Pattern to search for in this page's js. the first capturing group in the 
	 * pattern will be returned by this method. if no capturing group is present, an
	 * IllegalArgumentException will be thrown
	 * @return the first capture in the resulting match from matching the given pattern against this
	 * page's js. if no match was found, null is returned
	 * @throws IOException if an IOException occurred when reading data from this page
	 * @throws SocketTimeoutException if a timeout occurred when reading data from this page
	 * @throws IllegalArgumentException if the provided pattern did not contain a capturing group
	 */
	String extract(Pattern pattern) throws IOException, SocketTimeoutException, IllegalArgumentException;
	
	/**
	 * compiles the given pattern into a Pattern object and then calls {@link #extract(Pattern)}. see 
	 * {@link #extract(Pattern)} for details
	 * @param pattern
	 * @return
	 * @throws IOException
	 * @throws SocketTimeoutException
	 * @throws IllegalArgumentException
	 * @throws PatternSyntaxException if the given pattern is not a valid Pattern
	 */
	String extract(String pattern) throws IOException, SocketTimeoutException, IllegalArgumentException, PatternSyntaxException;
	
	@Override
	public void close() throws IOException;
}
