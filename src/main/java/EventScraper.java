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
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
          .get();

      Elements items = doc.select(".list li");
      if (items.isEmpty()) {
        return;
      }

      StringBuilder html = new StringBuilder(
          "<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>");
      html.append(
          "<meta name='viewport' content='width=device-width, initial-scale=1.0'><title>공모전 알림판</title>");
      html.append(
          "<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>");
      html.append("<style>");
      html.append(
          "body{background:#f8f9fa; padding-top:30px; font-family:'Pretendard', sans-serif;}");
      html.append(
          ".contest-card{background:white; border-radius:15px; overflow:hidden; margin-bottom:20px; box-shadow:0 4px 6px rgba(0,0,0,0.05); transition:0.3s; height: 100%; display: flex; flex-direction: column;}");
      html.append(
          ".contest-card:hover{transform:translateY(-5px); box-shadow:0 8px 15px rgba(0,0,0,0.1);}");
      html.append(
          ".img-container{width:100%; height:200px; overflow:hidden; background:#dee2e6; display: flex; align-items: center; justify-content: center;}");
      html.append(
          ".img-container img{width:100%; height:100%; object-fit: contain; padding: 10px;}");
      html.append(".card-body{padding:20px; flex-grow: 1;}");
      html.append(
          ".badge-date{background:#0d6efd; color:white; padding:4px 12px; border-radius:20px; font-size:0.8rem; font-weight:bold;}");
      html.append("h1{font-weight:800; text-align:center; margin-bottom:30px; color: #212529;}");
      html.append(".search-box{max-width:500px; margin: 0 auto 40px;}");
      html.append("</style></head><body><div class='container'>");

      html.append("<h1>공모전</h1>");
      html.append(
          "<div class='search-box'><input type='text' id='searchInput' class='form-control form-control-lg shadow-sm' placeholder='공모전 제목이나 주최측 검색...'></div>");
      html.append("<div class='row' id='contestList'>");

      int newCount = 0;
      for (int i = 0; i < Math.min(items.size(), 20); i++) {
        Element item = items.get(i);
        Element el = item.selectFirst(".tit a");
        if (el == null) {
          continue;
        }

        String title = el.text();
        String organ = item.select(".organ").text();
        String day = item.select(".day").text();
        String link = "https://www.wevity.com/" + el.attr("href");

        Element imgEl = item.selectFirst("img");
        String imgUrl = "https://via.placeholder.com/300x200?text=No+Image";
        if (imgEl != null) {
          String src = imgEl.attr("src");
          if (!src.isEmpty()) {
            imgUrl = src.startsWith("http") ? src : "https://www.wevity.com" + src;
          }
        }

        html.append("<div class='col-md-6 col-lg-4 mb-4 contest-item'>");
        html.append("<div class='contest-card'>");
        html.append("<div class='img-container'><img src='").append(imgUrl).append(
            "' onerror=\"this.src='https://via.placeholder.com/300x200?text=Poster'\" alt='공모전 포스터'></div>");
        html.append("<div class='card-body'>");
        html.append("<span class='badge-date'>").append(day).append("</span>");
        html.append("<h5 class='mt-3 mb-2 fw-bold text-truncate' title='").append(title)
            .append("'>").append(title).append("</h5>");
        html.append("<p class='text-muted small mb-3'> 주최: ").append(organ).append("</p>");
        html.append("<a href='").append(link)
            .append("' target='_blank' class='btn btn-outline-primary btn-sm w-100'>상세보기 →</a>");
        html.append("</div></div></div>");

        if (sent.add(title)) {
          sendDiscordAlert(DISCORD_WEBHOOK_URL,
              "공모전\n 제목: " + title + "\n 주최: " + organ + "\n 마감: " + day + "\n 링크: " + link);
          newCount++;
        }
      }

      html.append("</div>");
      html.append("<script>");
      html.append("document.getElementById('searchInput').addEventListener('keyup', function() {");
      html.append("  let val = this.value.toLowerCase();");
      html.append("  document.querySelectorAll('.contest-item').forEach(item => {");
      html.append("    let text = item.innerText.toLowerCase();");
      html.append("    item.style.display = text.includes(val) ? '' : 'none';");
      html.append("  });");
      html.append("});");
      html.append("</script>");

      String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      html.append("<footer class='text-center mt-5 mb-5 text-muted'><small>마지막 동기화: ").append(time)
          .append("</small></footer></div></body></html>");

      Files.write(Paths.get(SENT_FILE), new ArrayList<>(sent));
      Files.writeString(Paths.get("index.html"), html.toString());

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