import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EventScraper {

  private static final String SENT_FILE = "sent.txt";
  private static final String WEVITY_URL = "https://www.wevity.com/index.php?c=find&s=1&gub=1";

  private static final String DISCORD_WEBHOOK_URL = "여기에_웹훅_주소를_복사해서_넣으세요";

  public static void main(String[] args) {
    try {
      Set<String> sent = Files.exists(Paths.get(SENT_FILE))
          ? new HashSet<>(Files.readAllLines(Paths.get(SENT_FILE)))
          : new HashSet<>();

      Document doc = Jsoup.connect(WEVITY_URL)
          .userAgent(
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)")
          .get();

      Elements items = doc.select(".list li");
      if (items.isEmpty()) {
        System.out.println("데이터를 찾을 수 없습니다.");
        return;
      }

      StringBuilder html = new StringBuilder(
          "<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>");
      html.append(
          "<meta name='viewport' content='width=device-width, initial-scale=1.0'><title>공모전 알림판</title>");
      html.append(
          "<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>");
      html.append(
          "<style>body{background:#f8f9fa;padding-top:50px;}.contest-card{background:white;border-radius:12px;padding:20px;margin-bottom:15px;box-shadow:0 2px 4px rgba(0,0,0,0.05);transition:0.2s;}.contest-card:hover{transform:translateY(-3px);}.badge-date{background:#0d6efd;color:white;padding:4px 10px;border-radius:20px;font-size:0.75rem;}h1{font-weight:800;text-align:center;margin-bottom:40px;}a{text-decoration:none;font-weight:600;}</style></head><body><div class='container'><h1>🏆 실시간 공모전 리포트</h1>");

      int newCount = 0;
      for (int i = 0; i < Math.min(items.size(), 15); i++) {
        Element item = items.get(i);
        Element el = item.selectFirst(".tit a");
        if (el == null) {
          continue;
        }

        String title = el.text(), organ = item.select(".organ").text(), day = item.select(".day")
            .text();
        String link = "https://www.wevity.com/" + el.attr("href");

        html.append("<div class='contest-card'><span class='badge-date'>").append(day)
            .append("</span>")
            .append("<h4 class='mt-2' style='font-size:1.15rem;'>").append(title).append("</h4>")
            .append("<p class='text-muted mb-2' style='font-size:0.9rem;'> 주최: ").append(organ)
            .append("</p>")
            .append("<a href='").append(link).append("' target='_blank'>자세히 보기 →</a></div>");

        if (sent.add(title)) {
          sendDiscordAlert(DISCORD_WEBHOOK_URL,
              "새 공모전**\n " + title + "\n 주최: " + organ + "\n 마감: " + day + "\n " + link);
          newCount++;
        }
      }

      Files.write(Paths.get(SENT_FILE), new ArrayList<>(sent));
      String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      html.append("<footer class='text-center mt-5 mb-5 text-muted'><small>마지막 동기화: ").append(time)
          .append("</small></footer></div></body></html>");
      Files.write(Paths.get("index.html"), html.toString().getBytes(StandardCharsets.UTF_8));

      System.out.println("완료! 새 알림: " + newCount + "건");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendDiscordAlert(String url, String msg) throws Exception {
    if (url == null || url.contains("여기에")) {
      return;
    }

    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);

    String json =
        "{\"content\":\"" + msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            + "\"}";
    try (OutputStream os = conn.getOutputStream()) {
      os.write(json.getBytes(StandardCharsets.UTF_8));
    }
    conn.getResponseCode();
    conn.disconnect();
  }
}