package youtube;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class InputStreamTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		Path path = Paths.get("./input.txt");
		try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path, StandardOpenOption.READ), StandardCharsets.UTF_8)) {
			char[] buffer = new char[1050000];
			reader.read(buffer, 0, 1048576);
			StringBuilder sb = new StringBuilder();
			sb.append(buffer);
			String str = sb.toString().trim();
			System.out.println(str);
			System.out.println(str.contains("hello"));
		}
		
	}
}
