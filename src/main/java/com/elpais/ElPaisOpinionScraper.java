package com.elpais;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ElPaisOpinionScraper {

    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "C:\\\\Selenium\\\\chromedriver-win64\\\\chromedriver-win64\\\\chromedriver.exe");

        // Headless Chrome setup
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);

        // CSV Writers
        FileWriter titleCsv = new FileWriter("titles.csv");
        titleCsv.append("Spanish Title,Translated Title\n");

        FileWriter wordCsv = new FileWriter("repeated_words.csv");
        wordCsv.append("Word,Count\n");

        try {
            driver.get("https://elpais.com/");

            // Accept cookies once
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[aria-label='Aceptar']")));
                acceptBtn.click();
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("No cookie banner found.");
            }

            // Click on Opinión
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[href='https://elpais.com/opinion/']")));
                opinionLink.click();
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Failed to click Opinión link: " + e.getMessage());
            }

            // Fetch first 5 articles
            List<WebElement> articles = driver.findElements(By.cssSelector("article a[href^='https://elpais.com/opinion/']"));
            List<String> links = articles.stream()
                    .map(e -> e.getAttribute("href"))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());

            List<String> translatedTitles = new ArrayList<>();

            for (String link : links) {
                driver.get(link);
                Thread.sleep(2000); // Allow page load

                Document doc = Jsoup.parse(driver.getPageSource());

                String title = doc.select("h1").text();
                String content = doc.select("p").stream()
                        .map(org.jsoup.nodes.Element::text)
                        .collect(Collectors.joining(" "));

                System.out.println("\n📰 Title (Spanish): " + title);
                System.out.println("📄 Content: " + content.substring(0, Math.min(300, content.length())) + "...");

                // Save image
                Elements imgEl = doc.select("figure img");
                if (!imgEl.isEmpty()) {
                    String imgUrl = imgEl.first().absUrl("src");
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        try (InputStream in = new URL(imgUrl).openStream()) {
                            String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
                            String name = safeTitle.substring(0, Math.min(20, safeTitle.length())) + ".jpg";
                            Files.copy(in, Paths.get(name), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("🖼️ Image saved as: " + name);
                        } catch (Exception e) {
                            System.out.println("⚠️ Failed to download image: " + e.getMessage());
                        }
                    }
                }

                // Translate title
                String translated = translateText(title);
                translatedTitles.add(translated);

                // Write to title CSV
                titleCsv.append("\"" + title.replace("\"", "\"\"") + "\",\"" + translated.replace("\"", "\"\"") + "\"\n");

                System.out.println("🌍 Translated Title: " + translated);
            }

            // Analyze repeated words
            Map<String, Long> wordCounts = translatedTitles.stream()
                    .flatMap(t -> Arrays.stream(t.toLowerCase().split("\\W+")))
                    .filter(w -> w.length() > 2)
                    .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

            Map<String, Long> repeatedWords = wordCounts.entrySet().stream()
                    .filter(e -> e.getValue() > 2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            System.out.println("\n🔁 Repeated Words (appearing > 2 times):");

            if (repeatedWords.isEmpty()) {
                wordCsv.append("Not available,0\n");
                System.out.println("Not available");
            } else {
                for (Map.Entry<String, Long> entry : repeatedWords.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                    wordCsv.append(entry.getKey()).append(",").append(String.valueOf(entry.getValue())).append("\n");
                }
            }

        } finally {
            // Close resources
            titleCsv.flush();
            titleCsv.close();
            wordCsv.flush();
            wordCsv.close();
            driver.quit();
        }
    }

    private static String translateText(String input) throws IOException {
        String apiUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=es&tl=en&dt=t&q=" +
                URLEncoder.encode(input, "UTF-8");
        InputStream response = URI.create(apiUrl).toURL().openStream();
        String json = new String(response.readAllBytes());
        response.close();

        // Parse translated text
        if (json.startsWith("[[[")) {
            int firstQuote = json.indexOf('"');
            int secondQuote = json.indexOf('"', firstQuote + 1);
            return json.substring(firstQuote + 1, secondQuote);
        }
        return input;
    }
}
