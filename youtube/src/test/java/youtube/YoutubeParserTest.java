package youtube;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import my.cute.parser.youtube.YoutubeParser;

public class YoutubeParserTest {

	public static void main(String[] args) throws IOException {
		YoutubeParser parser = YoutubeParser.createDefault();
		//https://www.youtube.com/watch?v=jMV-aN634ZQ&ab_channel=MechaSoul
		String[] links = new String[] {
				"https://www.youtube.com/watch?v=IODxDxX7oi4&t=59s&ab_channel=Calisthenicmovement",
				"https://www.youtube.com/watch?v=87WMpOFbvBQ&ab_channel=AZKiChannel",
				"https://www.youtube.com/watch?v=26gr6waXm2E&ab_channel=ThePatMcAfeeShowThePatMcAfeeShowVerified",
				"https://www.youtube.com/watch?v=8ftMkhhp3_Y&ab_channel=MixiGaming",
				"https://music.youtube.com/watch?v=penvn9VL32Y&list=RDAMVMpenvn9VL32Y",
				"https://www.youtube.com/embed/6KEnzhHQhoo",
				"https://youtu.be/6KEnzhHQhoo?t=5328"
				
		};
		String[] timestampedLinks = new String[] {
				"https://youtu.be/6KEnzhHQhoo?t=5328",
				"https://youtu.be/6KEnzhHQhoo?t=1h50m342s",
				"https://www.youtube.com/watch?v=6KEnzhHQhoo&t=18100s&ab_channel=GameSpot",
				"https://www.youtube.com/watch?v=6KEnzhHQhoo&t=5h01m40s&ab_channel=GameSpot"
		};

//		testLinks(links, parser);
		String parsed = parser.parse("https://www.youtube.com/watch?v=jMV-aN634ZQ&ab_channel=MechaSoul");
		Files.writeString(Paths.get("./ytdirectvideo.txt"), parsed, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		
		for(String link: timestampedLinks) {
			if(parser.probe(link)) {
				System.out.println(parser.getTimestamp(link));
			} else {
				System.out.println("no link");
			}
		}
	}
	
	public static void testLinks(String[] links, YoutubeParser parser) {
		for(String link : links) {
			if(parser.probe(link)) {
				System.out.println(parser.parse(link));
			} else {
				System.out.println("no link");
			}
		}
	}

}
