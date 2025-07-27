# 📰 El País Opinion Scraper

## 📖 About

**El País Opinion Scraper** is a Java-based automation tool that:

- Scrapes the top 5 articles from the **"Opinión"** section of [elpais.com](https://elpais.com)
- Translates the article titles from **Spanish to English** using Google Translate’s free API
- Downloads and saves the main image from each article
- Analyzes and counts **repeated words** across all translated titles
- Outputs results into clean, readable **CSV files** for easy analysis

---

## ▶️ How It Works

1. **Launches a headless browser** using Selenium WebDriver and Chrome
2. Navigates to [elpais.com](https://elpais.com), handles the cookie banner, and clicks on the "Opinión" link
3. Extracts the top 5 article links, then:
   - Scrapes the Spanish title and preview content
   - Translates the title to English
   - Downloads the article image (if available)
   - Appends both original and translated titles to `titles.csv`
4. After all titles are processed, it counts and logs any **repeated words** (appearing more than 2 times) to `repeated_words.csv`

---

## 📂 Output Files

- `titles.csv` – Stores each article’s Spanish and translated title
- `repeated_words.csv` – Words repeated across translated titles (count > 2)
- `[image].jpg` – Article cover images saved locally (named after the title)

---

## 🚀 How to Run

### ✅ Prerequisites

- Java 17+
- Maven installed
- Google Chrome browser
- ChromeDriver installed (and path updated in your `ElPaisOpinionScraper.java`, e.g., `System.setProperty(...)`)

### ▶️ Run it

```bash
# Inside the project folder
mvn clean compile exec:java
