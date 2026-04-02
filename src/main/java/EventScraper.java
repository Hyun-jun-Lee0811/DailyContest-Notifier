import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;

public class EventScraper {

  private static final String SENT_FILE = "sent.txt";
  private static final String WEVITY_URL = "https://www.wevity.com/index.php?c=find&s=1&gub=1";

  public static void main(String[] args) {
    try {
      Yaml yaml = new Yaml();
      Set<String> sent = new HashSet<>();
      Map<String, Object> config = yaml.load(new FileInputStream("config.yml"));
      Map<String, Object> discordConfig = (Map<String, Object>) config.get("discord");
      String discordWebhook = (String) discordConfig.get("webhook");
      int count = 0;

      Document doc = Jsoup.connect(WEVITY_URL)
          .userAgent(
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
          .get();

      Elements items = doc.select(".list li");

      if (items.isEmpty()) {
        System.out.println("공모전 데이터를 찾을 수 없습니다.");
        return;
      }

      if (Files.exists(Paths.get(SENT_FILE))) {
        sent.addAll(Files.readAllLines(Paths.get(SENT_FILE)));
      }

      for (Element item : items) {
        Element element = item.selectFirst(".tit a");
        if (element == null) {
          continue;
        }

        String title = element.text();
        String organizer = item.select(".organ").text();
        String deadline = item.select(".day").text();

        if (sent.contains(title)) {
          continue;
        }

        String message = "<새 공모전 업데이트>\n"
            + "제목: " + title + "\n"
            + "주최: " + organizer + "\n"
            + "마감: " + deadline + "\n"
            + "링크: " + "https://www.wevity.com/" + element.attr("href");

        System.out.println("전송 중: " + title);
        sendDiscordAlert(discordWebhook, message);

        sent.add(title);
        count++;

        if (count >= 15) {
          break;
        }
      }

      Files.write(Paths.get(SENT_FILE), sent);
      System.out.println("업데이트 완료! 새 알림 " + count + "건 전송됨.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendDiscordAlert(String webhookUrl, String message) throws Exception {
    URL url = new URL(webhookUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);

    message = message.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n");

    String jsonPayload = "{\"content\":\"" + message + "\"}";

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != 204) {
      System.out.println("Discord 알림 전송 실패: " + responseCode);
    }
    conn.disconnect();
  }
}