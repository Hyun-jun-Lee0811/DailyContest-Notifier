import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;

public class EventScraper {

  private static final String SENT_FILE = "sent.txt";

  public static void main(String[] args) {
    try {
      Yaml yaml = new Yaml();
      Map<String, Object> config = yaml.load(new FileInputStream("config.yml"));
      Map<String, Object> apiConfig = (Map<String, Object>) ((Map<String, Object>) config.get("api")).get("worldjob");
      Map<String, Object> discordConfig = (Map<String, Object>) config.get("discord");

      String serviceKey = (String) apiConfig.get("key");
      String discordWebhook = (String) discordConfig.get("webhook");

      String encodedKey = URLEncoder.encode(serviceKey, "UTF-8");

      int currentYear = java.time.LocalDate.now().getYear();
      String urlStr = "http://apis.data.go.kr/B490007/worldjob10/openApi10"
          + "?serviceKey=" + encodedKey
          + "&searchYear=" + currentYear
          + "&numOfRows=50"
          + "&pageNo=1";

      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/xml");

      int responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        System.out.println("Response Code : " + responseCode);
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        String line;
        while ((line = br.readLine()) != null) System.out.println(line);
        br.close();
        return;
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) sb.append(line);
      br.close();
      conn.disconnect();

      Document doc = Jsoup.parse(sb.toString(), "", Parser.xmlParser());
      Elements items = doc.select("ITEM");

      if (items.isEmpty()) {
        System.out.println("현재 선택한 연도에는 공모전 데이터가 없습니다.");
        return;
      }

      Set<String> sent = new HashSet<>();
      if (Files.exists(Paths.get(SENT_FILE))) {
        sent.addAll(Files.readAllLines(Paths.get(SENT_FILE)));
      }

      for (var item : items) {
        String title = item.select("ctstSj").text();
        String start = item.select("ctstBgnDt").text();
        String end = item.select("ctstEndDt").text();
        String year = item.select("ctstYear").text();

        // 중복 체크
        if (sent.contains(title)) continue;

        String message = "새 공모전 [" + year + "]: " + title
            + "\n 기간: " + start + " ~ " + end;

        System.out.println(message);

        sendDiscordAlert(discordWebhook, message);

        sent.add(title);
      }

      Files.write(Paths.get(SENT_FILE), sent);

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
      byte[] input = jsonPayload.getBytes("UTF-8");
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != 204) {
      System.out.println("Discord 알림 전송 실패: " + responseCode);
    }
    conn.disconnect();
  }
}