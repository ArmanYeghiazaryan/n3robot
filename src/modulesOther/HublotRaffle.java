package modulesOther;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import javax.sound.sampled.LineUnavailableException;

import org.apache.http.client.protocol.ResponseAuthCache;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Proxy;
import com.opencsv.CSVWriter;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import main.BackendWebhook;
import main.CLITools;
import main.DiscordWebhook;
import main.Main;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class HublotRaffle extends Thread {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	private String raffleURL;
	private String customField;
	private String transactionValue;
	private String gasPrice;
	private String privateKey;
	private String userPublicAddress;
	private HashSet<String> twoCaptchaKeys;
	private String csrfToken;
	private String csrfmiddlewaretoken;
	private String entryNonce;
	private String signature;
	private String imageUrl;
	private boolean isFinished;
	private boolean hasPassword;
	private boolean twitterCredentials = true;

	private CaptchaThreadPremint captchaThread;
	private static int finishedTasks;
	CLITools cliTools = new CLITools(false);
	private String mode;
	private int tasksAmount;
	private int delay;
	private String proxy;
	private String ip;
	private String username;
	private String password;
	private int port;
	List<Cookie> cookieslist;
	private String webhookURL;
	private int errorCounter = 0;
	private int taskId;
	private boolean forceCaptcha;
	private String email;
	private String name;
	private String lastName;
	private String phoneCountry;
	private String phonePrefix;
	private String phoneNr;
	private String formKey;

	public HublotRaffle(int taskId, String proxy, String email, String name, String lastName, String phoneCountry,
			String phonePrefix, String phoneNr) {
		this.taskId = taskId;
		this.proxy = proxy;
		this.email = email;
		this.name = name;
		this.lastName = lastName;
		this.phoneCountry = phoneCountry;
		this.phoneNr = phoneNr;
		this.phonePrefix = phonePrefix;

	}

	public String getCustomField() {
		return customField;
	}

	public HashSet<String> getTwoCaptchaKeys() {
		return twoCaptchaKeys;
	}

	public int getTasksAmount() {
		return tasksAmount;
	}

	public int getDelay() {
		return delay;
	}

	public String getWebhookURL() {
		return webhookURL;
	}

	public void run() {

		try {

			Session session = Requests.session();
			setProxy();
			prepareTask();
			getRaffle(session);
			postEmail(session);
			postSignUp(session);

		} catch (Exception e) {
//			e.printStackTrace();

			// TODO Auto-generated catch block
			System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
					+ taskId + "] - Generous Error: " + parsePWError(e.getMessage()) + ANSI_RESET);

			this.errorCounter = 0;
			this.isFinished = true;
		}
	}

	public void prepareTask() throws Exception {
		System.out.println("[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - [" + taskId
				+ "] - Preparing Entry...");

		if (name.toLowerCase().contains("random"))
			name = name.toLowerCase().replace("random", generateName());

		if (lastName.toLowerCase().contains("random"))
			lastName = lastName.toLowerCase().replace("random", generateName());

		if (email.toLowerCase().contains("random"))
			email = email.toLowerCase().replace("random", generateName().toLowerCase());

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < this.phoneNr.length(); i++) {
			if (phoneNr.toLowerCase().charAt(i) == 'x') {
				int random_int = (int) Math.floor(Math.random() * (9 - 0 + 1) + 0);
				sb.append(String.valueOf(random_int));

			} else {
				sb.append(String.valueOf(phoneNr.charAt(i)));
			}
		}

		this.phoneNr = sb.toString();

	}

	public String generateName() throws IOException {
		// GENERATE RANDOM NAME
		File file = new File(System.getProperty("user.dir") + "\\target\\names.txt");
		final RandomAccessFile f = new RandomAccessFile(file, "r");
		final long randomLocation = (long) (Math.random() * f.length());
		f.seek(randomLocation);
		f.readLine();
		String name = f.readLine();
		f.close();

		return name;

	}

	public void getRaffle(Session session) throws Exception {
		try {

			System.out.println(ANSI_GREY + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
					+ taskId + "] - Getting Raffle..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();

			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			RawResponse newSession = session.get("https://boutique.hublot.com/eur_en/nftclaim/form/luckydraw")
					.proxy(Proxies.httpProxy(ip, port)).headers(request).socksTimeout(30000).connectTimeout(30000)
					.send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				setProxy();

				System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - GET RAFFLE FAILED - " + newSession.statusCode() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);
				getRaffle(session);

			} else {

				String arr[] = response.split(Pattern.quote("name=\"form_key\" type=\"hidden\" value=\""));
				String temp = arr[1];
				String arr2[] = temp.split(Pattern.quote("\""));
				this.formKey = arr2[0];

			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request RAFFLE Error Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			getRaffle(session);

		}
	}

	public void postEmail(Session session) throws Exception {
		try {

			System.out.println(ANSI_YELLOW + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
					+ taskId + "] - Submitting Entry... [1]" + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();
//			request.put("Authority", "boutique.hublot.com");
			request.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");
			request.put("Cache-Control", "max-age=0");
			request.put("Content-Type", "application/json; charset=UTF-8");
			request.put("Origin", "https://boutique.hublot.com");
			request.put("Referer", "https://boutique.hublot.com/eur_en/nftclaim/form/luckydraw");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"101\", \"Google Chrome\";v=\"101\"");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Sec-Fetch-Dest", "document");
			request.put("Sec-Fetch-Mode", "navigate");
			request.put("Sec-Fetch-Site", "same-origin");
			request.put("Sec-Fetch-User", "?1");
			request.put("Upgrade-Insecure-Requests", "1");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			String body = "{\"customerEmail\":\"" + this.email + "\"}";

			RawResponse newSession = session
					.post("https://boutique.hublot.com/eur_en//rest/V1/customers/isEmailAvailable").headers(request)
					.body(body).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				setProxy();

				System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
						+ taskId + "] - POST EMAIL FAILED - Retrying: " + newSession.statusCode() + ANSI_RESET);
				System.out.println(response);
				Thread.sleep(1000);
				postEmail(session);

			} else {

			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error EMAIL Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			postEmail(session);

		}
	}

	public void postSignUp(Session session) throws Exception {
		try {

			System.out.println(ANSI_YELLOW + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
					+ taskId + "] - Submitting Entry... [2]" + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();
//			request.put("Authority", "boutique.hublot.com");
			request.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");
			request.put("Cache-Control", "max-age=0");
			request.put("Content-Type", "application/x-www-form-urlencoded");
			request.put("Origin", "https://boutique.hublot.com");
			request.put("Referer", "https://boutique.hublot.com/eur_en/nftclaim/form/luckydraw");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"101\", \"Google Chrome\";v=\"101\"");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Sec-Fetch-Dest", "document");
			request.put("Sec-Fetch-Mode", "navigate");
			request.put("Sec-Fetch-Site", "same-origin");
			request.put("Sec-Fetch-User", "?1");
			request.put("Upgrade-Insecure-Requests", "1");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			String body = "form_key=" + this.formKey + "&email=" + this.email.replace("@", "%40")
					+ "&title=Mr.&firstname=" + this.name + "&lastname=" + this.lastName + "&phone_prefix="
					+ this.phoneCountry + "&phone=" + this.phoneNr + "&phone_formated="
					+ this.phonePrefix.replace("+", "%2B") + this.phoneNr + "&optin_tc=on";

			RawResponse newSession = session.post("https://boutique.hublot.com/eur_en/nftclaim/form/luckydrawsuccess/")
					.headers(request).body(body).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (!response.contains("THANK YOU FOR YOUR PARTICIPATION IN THE HUBLOT NFT DRAW!")) {

				setProxy();

				System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
						+ taskId + "] - POST ENTRY FAILED - Retrying: " + newSession.statusCode() + ANSI_RESET);
				Thread.sleep(1000);
				postSignUp(session);

			} else {

				System.out.println(ANSI_GREEN + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.email + "] - ["
						+ taskId + "] - Entry Succeeded!" + ANSI_RESET);
				addToResultsCSV();
			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error POST_ENTRY Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			postSignUp(session);

		}
	}

	synchronized void increaseTasksCounter() {
		this.finishedTasks++;

		cliTools.setTitle("N3RO BOT - HUBLOT Entries - Status: " + this.finishedTasks + "/" + this.tasksAmount);

	}

//	public void connectTwitter() throws Exception {
//
//		page.navigate("https://www.HUBLOT.xyz/accounts/twitter/login/?process=connect&next=%2Fprofile%2F");
//
//		page.type("//*[@id=\"username_or_email\"]", "alna.krasivaya.89@bk.ru");
//		Thread.sleep(2000);
//		String currentUrl = page.url();
//		page.type("//*[@id=\"password\"]", "jXmvGZutlj");
//		Thread.sleep(2000);
//		page.click("//*[@id=\"allow\"]");
//
//		while (page.url().equals(currentUrl)) {
//			Thread.sleep(1000);
//		}
//
//		page.waitForLoadState();
//
//		if (page.url().contains("https://twitter.com/account/login_challenge")) {
//			System.out.println("Phone Nr. detected!");
//			page.type("//*[@id=\"challenge_response\"]", "79346695012");
//			page.click("//*[@id=\"email_challenge_submit\"]");
//		}
//
//		page.click("//*[@id=\"allow\"]");
//
//	}

	synchronized void addToResultsCSV() throws IOException {
		FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\tasks\\other\\hublotEntriesResults.csv",
				true);

		writer.write("\n" + dtf.format(now.now()) + "," + this.email + "," + this.name + "," + this.lastName + ","
				+ this.phonePrefix + this.phoneNr + "," + "ENTERED");
		writer.close();
		increaseTasksCounter();
	}

	public int getAmountFinishedTasks() {
		return this.finishedTasks;
	}

	public boolean getIsFinished() {
		return this.isFinished;
	}

	public void setProxy() throws Exception {
		if (this.proxy.toLowerCase().equals("random")) {
			String[] p = getProxy().split(":");
			this.ip = p[0];
			this.port = Integer.valueOf(p[1]);
//			this.username = p[2];
//			this.password = p[3];

		} else {
			String[] p = proxy.split(":");
			this.ip = p[0];
			this.port = Integer.valueOf(p[1]);
//			this.username = p[2];
//			this.password = p[3];
		}
	}

	public String getProxy() throws Exception {
		List<String> proxies = new ArrayList<String>();

		// GENERATE RANDOM PROXY
		File file = new File(System.getProperty("user.dir") + "\\tasks\\proxies.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				proxies.add(line);
			}
		}

		int randomNum = ThreadLocalRandom.current().nextInt(0, proxies.size());

		return proxies.get(randomNum);

	}

	public String getPrivateKey() {
		return this.privateKey;
	}

	public String getRaffleURL() {
		return raffleURL;
	}

	public String getTransactionValue() {
		return this.transactionValue;
	}

	public String getGasPrice() {
		return this.gasPrice;
	}

	public String parsePWError(String err) {

		try {
			if (err.toLowerCase().contains("networkerror when attempting to fetch resource")) {
				err = "Network issue for resource.";
			} else {

				String[] arr = err.split("message='");
				String temp = arr[1];
				String arr2[] = temp.split("==");

				String error = arr2[0].strip();
				err = error;
			}
		} catch (Exception e) {
		}

		return err;

	}

	public void setRaffleURL(String raffleURL) {
		this.raffleURL = raffleURL;
	}

	public void sendWebhook() throws IOException, LineUnavailableException, InterruptedException {

		Color color = Color.green;

		String title = "Won HUBLOT Raffle";

		DiscordWebhook webhook = new DiscordWebhook(this.webhookURL);
		webhook.setUsername("N3RO BOT");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
		webhook.setTts(false);
		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title).setColor(color).setUrl(this.raffleURL)
				.addField("Raffle", this.raffleURL, false)
				.addField("Wallet", "||" + new Main().getEtherCredentialFromWalletsFile("NAME", this.privateKey) + "||",
						false)

				.setFooter(dtf.format(now.now()) + " | @n3robot", null).setThumbnail(this.imageUrl));

		try {
			webhook.execute();
			System.out.println("[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress + "] - ["
					+ taskId + "] - Sent Webhook.");

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[HUBLOT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Send Webhook Failed." + ANSI_RESET);
//			Thread.sleep(10000);
//			sendWebhook();
		}
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREY = "\u001b[30;1m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
}
