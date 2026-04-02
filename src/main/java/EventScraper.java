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
  private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1483170528154878003/t2P-Cz9-tOrT3dlqv0TwlA-9G5ob6tnQ1m7SQkz-KxoVjq8O5aCEYYJzXyiTx50wya3q";

  public static void main(String[] args) {
    try {
      Set<String> sent = Files.exists(Paths.get(SENT_FILE))
          ? new HashSet<>(Files.readAllLines(Paths.get(SENT_FILE)))
          : new HashSet<>();

      Document doc = Jsoup.connect(WEVITY_URL)
          .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)")
          .get();

      Elements items = doc.select(".list li");
      if (items.isEmpty()) return;

      StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>");
      html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'><title>공모전 알림판</title>");
      html.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>");
      html.append("<style>");
      html.append("body{background:#f4f7f9; padding-top:40px; font-family:'Pretendard', sans-serif;}");
      html.append(".contest-card{background:white; border-radius:12px; border:1px solid #e1e4e8; padding:24px; margin-bottom:20px; transition:0.2s; height: 100%;}");
      html.append(".contest-card:hover{transform:translateY(-3px); box-shadow:0 8px 16px rgba(0,0,0,0.08); border-color:#0d6efd;}");
      html.append(".badge-date{background:#0d6efd; color:white; padding:4px 14px; border-radius:30px; font-size:0.85rem; font-weight:700; display:inline-block; margin-bottom:15px;}");
      html.append("h1{font-weight:800; text-align:center; margin-bottom:10px; color:#1a1a1a;}");
      html.append(".sub-title{text-align:center; color:#666; margin-bottom:40px;}");
      html.append(".search-box{max-width:600px; margin: 0 auto 50px;}");
      html.append("</style></head><body><div class='container'>");

      html.append("<h1> 공모전 </h1>");
      html.append("<p class='sub-title'>최신 공모전 정보를 한눈에 확인하세요</p>");

      // 🔍 검색창 코드 (오류 수정됨)
      html.append("<div class='search-box'>");
      html.append("<input type='text' id='searchInput' class='form-control form-control-lg shadow-sm' placeholder='키워드로 공모전 찾기...'>");
      html.append("</div>");

      html.append("<div class='row' id='contestList'>");

      int newCount = 0;
      for (int i = 0; i < Math.min(items.size(), 20); i++) {
        Element item = items.get(i);
        Element el = item.selectFirst(".tit a");
        if (el == null) continue;

        String title = el.text();
        String organ = item.select(".organ").text();
        String day = item.select(".day").text();
        String link = "https://www.wevity.com/" + el.attr("href");

        html.append("<div class='col-md-6 col-lg-4 mb-4 contest-item'>");
        html.append("<div class='contest-card'>");
        html.append("<span class='badge-date'>").append(day).append("</span>");
        html.append("<h5 class='fw-bold mb-3' style='line-height:1.5; height:3rem; overflow:hidden;'>").append(title).append("</h5>");
        html.append("<p class='text-muted small mb-4'> ").append(organ).append("</p>");
        html.append("<a href='").append(link).append("' target='_blank' class='btn btn-primary w-100 fw-bold'>상세 정보 보기</a>");
        html.append("</div></div>");

        if (sent.add(title)) {
          sendDiscordAlert(DISCORD_WEBHOOK_URL, "공모전**\n " + title + "\n 주최: " + organ + "\n 마감: " + day + "\n " + link);
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
      html.append("<footer class='text-center mt-5 mb-5 text-muted'><hr><small>최종 업데이트: ").append(time).append("</small></footer></div></body></html>");

      Files.write(Paths.get(SENT_FILE), new ArrayList<>(sent));
      Files.write(Paths.get("index.html"), html.toString().getBytes(StandardCharsets.UTF_8));

      System.out.println("완료! 웹 페이지가 정상적으로 갱신되었습니다.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendDiscordAlert(String url, String msg) throws Exception {
    if (url == null || url.contains("여기에")) return;
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);
    String json = "{\"content\":\"" + msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
    try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
    conn.getResponseCode();
    conn.disconnect();
  }
}