package modulesOther;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

public class PremintTaskRequests extends Thread {
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

	public PremintTaskRequests(int taskId, String mode, String raffleURL, String customField, String transactionValue,
			String gasPrice, String privateKey, String proxy, HashSet<String> twoCaptchaKeys, int delay,
			int tasksAmount, String webhookURL) {
		this.taskId = taskId;
		this.mode = mode;
		this.raffleURL = raffleURL;
		this.customField = customField;
		this.transactionValue = transactionValue;
		this.gasPrice = gasPrice;
		this.privateKey = privateKey;
		this.proxy = proxy;
		this.twoCaptchaKeys = twoCaptchaKeys;
		this.delay = delay;
		this.tasksAmount = tasksAmount;
		this.webhookURL = webhookURL;

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

			if (this.mode.toLowerCase().equals("raffle")) {
				Session session = Requests.session();
				setProxy();
				prepareTask();
				getCsrfToken(session);
				postSignUp(session);
				getNonce(session);
				getSignature();
				login(session);
				getCsrfMiddleWareToken(session);
				submitEntry(session);
			}

			else if (this.mode.toLowerCase().equals("initializer")) {
				Session session = Requests.session();
				setProxy();
				prepareTask();
				getCsrfToken(session);
				postSignUp(session);
				getNonce(session);
				getSignature();
				login(session);
//				getProfilePage();
			}

			else {
				// checker
				Session session = Requests.session();
				setProxy();
				prepareTask();
				checkIfWinner();
			}

		} catch (Exception e) {
//			e.printStackTrace();

			// TODO Auto-generated catch block
			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Generous Error: " + parsePWError(e.getMessage()) + ANSI_RESET);

			this.errorCounter = 0;
			this.isFinished = true;
		}
	}

	public void prepareTask() {
		System.out.println("[PREMINT] - [" + dtf.format(now.now())
				+ "] - [------------------------------------------] - [" + taskId + "] - Preparing Entry...");

		// Get address
		Credentials credentials = Credentials.create(this.privateKey);
		this.userPublicAddress = credentials.getAddress();

		boolean headless = true;

		if (this.mode.toLowerCase().equals("initializer")) {
			headless = false;
		}

		if (this.raffleURL.charAt(this.raffleURL.length() - 1) != '/') {
			this.raffleURL = this.raffleURL + "/";
		}
//
//		if (!this.mode.toLowerCase().equals("checker")) {
//			this.playwright = Playwright.create();
//			this.browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(headless).setProxy(
//					new Proxy("http://" + ip + ":" + port + "").setUsername(this.username).setPassword(this.password))
//					.setTimeout(30_000));
//			this.context = browser.newContext(new Browser.NewContextOptions().setUserAgent(
//					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36"));
//			this.page = context.newPage();
//		}

	}

	public void getCsrfToken(Session session) throws Exception {
		try {

			System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Getting Metadata..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();

//			request.put("User-Agent",
//					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			RawResponse newSession = session.get("https://www.premint.xyz/login/").proxy(Proxies.httpProxy(ip, port))
					.headers(request).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - GET CSRF FAILED - " + newSession.statusCode() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);
				getCsrfToken(session);

			} else {

				for (Cookie cookie : newSession.cookies()) {
					if (cookie.name().equals("csrftoken")) {
						this.csrfToken = cookie.value();
					}
				}

				if (this.csrfToken == null) {
					throw new Exception("CSRF_TOKEN_MISSING");
				}

				Elements imageElement = Jsoup.parse(response).getElementsByClass("img-center bg-white border-white");

				for (Element el : imageElement) {
					this.imageUrl = el.absUrl("src");
				}

				this.forceCaptcha = true;

			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request GET_MD_1 Error Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			getCsrfToken(session);

		}
	}

	public void postSignUp(Session session) throws Exception {
		try {

			Map<String, Object> request = new HashMap<>();
			request.put("Accept", "*/*");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");
			request.put("Connection", "keep-alive");
			request.put("Content-Length", "51");
			request.put("Content-Type", "application/x-www-form-urlencoded");
			request.put("Host", "www.premint.xyz");
			request.put("Origin", "https://www.premint.xyz");
			request.put("Referer", "https://www.premint.xyz/");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\", \"Google Chrome\";v=\"100\"");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Sec-Fetch-Dest", "empty");
			request.put("Sec-Fetch-Mode", "cors");
			request.put("Sec-Fetch-Site", "same-origin");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");
			request.put("x-csrftoken", this.csrfToken);

			RawResponse newSession = session.post("https://www.premint.xyz/v1/signup_api/").headers(request)
					.body("username=" + this.userPublicAddress).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (!response.contains("success")) {

				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - POST SIGNUP FAILED - Retrying..." + ANSI_RESET);
				Thread.sleep(1000);
				postSignUp(session);

			} else {

//				System.out.println(response);
			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error GET_MD_2 Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			postSignUp(session);

		}
	}

	public void getNonce(Session session) throws Exception {
		try {

			Map<String, Object> request = new HashMap<>();

			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			RawResponse newSession = session.get("https://www.premint.xyz/v1/login_api/")
					.proxy(Proxies.httpProxy(ip, port)).headers(request).socksTimeout(30000).connectTimeout(30000)
					.send();

			String response = newSession.readToText();

			if (!response.contains("success\": true")) {

				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - GET NONCE FAILED - Retrying..." + ANSI_RESET);
				Thread.sleep(1000);
				getNonce(session);

			} else {
//				System.out.println(response);
				JSONObject o = new JSONObject(new JSONTokener(response));
				this.entryNonce = o.getString("data");
			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error GET_MD_3 Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			getNonce(session);

		}
	}

	public void getSignature() {

		Credentials credentials = Credentials.create(this.privateKey);

		String message = "Welcome to PREMINT!\n" + "\n" + "Signing is the only way we can truly know \n"
				+ "that you are the owner of the wallet you \n" + "are connecting. Signing is a safe, gas-less \n"
				+ "transaction that does not in any way give \n" + "PREMINT permission to perform any \n"
				+ "transactions with your wallet.\n" + "\n" + "Wallet address:\n" + "" + this.userPublicAddress + "\n"
				+ "\n" + "Nonce: " + this.entryNonce + "";

		Numeric.hexStringToByteArray(message);
		Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(), credentials.getEcKeyPair());

		String r = Numeric.toHexString(sig.getR());
		String s = Numeric.toHexString(sig.getS());
		String v = Numeric.toHexString(sig.getV());

		s = s.substring(2, s.length());
		v = v.substring(2, v.length());

		this.signature = r + s + v;

	}

	public void login(Session session) throws Exception {
		try {
			System.out.println("[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress + "] - ["
					+ taskId + "] - Connecting Wallet...");

//			String response = (String) page.evaluate("fetch(\"https://www.premint.xyz/v1/login_api/\", {\n"
//					+ "  \"headers\": {\n" + "    \"accept\": \"*/*\",\n"
//					+ "    \"accept-language\": \"de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5\",\n"
//					+ "    \"content-type\": \"application/x-www-form-urlencoded; charset=UTF-8\",\n"
//					+ "    \"sec-ch-ua\": \"\\\" Not A;Brand\\\";v=\\\"99\\\", \\\"Chromium\\\";v=\\\"100\\\", \\\"Google Chrome\\\";v=\\\"100\\\"\",\n"
//					+ "    \"sec-ch-ua-mobile\": \"?0\",\n" + "    \"sec-ch-ua-platform\": \"\\\"Windows\\\"\",\n"
//					+ "    \"sec-fetch-dest\": \"empty\",\n" + "    \"sec-fetch-mode\": \"cors\",\n"
//					+ "    \"sec-fetch-site\": \"same-origin\",\n" + "    \"x-csrftoken\": \"" + this.csrfToken + "\"\n"
//					+ "  },\n" + "  \"referrer\": \"https://www.premint.xyz/login/\",\n"
//					+ "  \"referrerPolicy\": \"same-origin\",\n" + "  \"body\": \"web3provider=metamask&address="
//					+ this.userPublicAddress + "&signature=" + this.signature + "\",\n" + "  \"method\": \"POST\",\n"
//					+ "  \"mode\": \"cors\",\n" + "  \"credentials\": \"include\"\n" + "})\n"
//					+ "    .then(response => {\n" + "        console.log(response.statusText);\n"
//					+ "        return response.text();\n" + "    });");

			Map<String, Object> request = new HashMap<>();
			request.put("Accept", "*/*");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");
			request.put("Connection", "keep-alive");
			request.put("Content-Length", "51");
			request.put("Content-Type", "application/x-www-form-urlencoded");
			request.put("Host", "www.premint.xyz");
			request.put("Origin", "https://www.premint.xyz");
			request.put("Referer", "https://www.premint.xyz/");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\", \"Google Chrome\";v=\"100\"");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Sec-Fetch-Dest", "empty");
			request.put("Sec-Fetch-Mode", "cors");
			request.put("Sec-Fetch-Site", "same-origin");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");
			request.put("x-csrftoken", this.csrfToken);

			RawResponse newSession = session.post("https://www.premint.xyz/v1/login_api/")
					.proxy(Proxies.httpProxy(ip, port))
					.body("web3provider=metamask&address=" + this.userPublicAddress + "&signature=" + this.signature)
					.headers(request).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (!response.contains("success\": true")) {

				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - WALLET CONNECTION FAILED - Retrying..." + ANSI_RESET);
				Thread.sleep(1000);
				login(session);

			} else {

				JSONObject o = new JSONObject(new JSONTokener(response));

				boolean success = o.getBoolean("success");

				if (!success) {
					System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Wallet Connection Failed: "
							+ parseError(response) + ANSI_RESET);
					Thread.sleep(2000);
					getCsrfToken(session);
					postSignUp(session);
					getNonce(session);
					getSignature();
					login(session);
				} else {

					System.out.println(ANSI_YELLOW + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Connected Wallet!" + ANSI_RESET);

//					if (this.mode.toLowerCase().equals("initializer")) {
//						page.navigate("https://www.premint.xyz/profile/");
//						System.out.println(
//								ANSI_GREEN + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
//										+ "] - [" + taskId + "] - Browser Is Operational!" + ANSI_RESET);
//						this.isFinished = true;
//						increaseTasksCounter();
//					}

				}
			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error Retrying: " + parsePWError(e.getMessage()) + ANSI_RESET);
			Thread.sleep(2000);
			login(session);

		}
	}

	public void submitEntry(Session session) throws Exception {

		try {

			if (!this.customField.equals("")) {
				customField = "custom_field=" + URLEncoder.encode(this.customField, StandardCharsets.UTF_8) + "&";
			}

			String siteKey = "6Lf9yOodAAAAADyXy9cQncsLqD9Gl4NCBx3JCR_x";

			String body = "csrfmiddlewaretoken=" + this.csrfmiddlewaretoken + "&" + customField
					+ "params_field=%7B%7D&g-recaptcha-response=" + getCaptcha(siteKey) + "&registration-form-submit=";

			System.out.println(ANSI_YELLOW + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Submitting Entry..." + ANSI_RESET);

//			String response = (String) page.evaluate("fetch(\"" + this.raffleURL + "\", {\n" + "  \"headers\": {\n"
//					+ "    \"accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\",\n"
//					+ "    \"accept-language\": \"de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5\",\n"
//					+ "    \"cache-control\": \"max-age=0\",\n"
//					+ "    \"content-type\": \"application/x-www-form-urlencoded\",\n"
//					+ "    \"sec-ch-ua\": \"\\\" Not A;Brand\\\";v=\\\"99\\\", \\\"Chromium\\\";v=\\\"100\\\", \\\"Google Chrome\\\";v=\\\"100\\\"\",\n"
//					+ "    \"sec-ch-ua-mobile\": \"?0\",\n" + "    \"sec-ch-ua-platform\": \"\\\"Windows\\\"\",\n"
//					+ "    \"sec-fetch-dest\": \"document\",\n" + "    \"sec-fetch-mode\": \"navigate\",\n"
//					+ "    \"sec-fetch-site\": \"same-origin\",\n" + "    \"sec-fetch-user\": \"?1\",\n"
//					+ "    \"upgrade-insecure-requests\": \"1\"\n" + "  },\n" + "  \"referrer\": \"" + this.raffleURL
//					+ "\",\n" + "  \"referrerPolicy\": \"same-origin\",\n" + "  \"body\": \"" + body + "\",\n"
//					+ "  \"method\": \"POST\",\n" + "  \"mode\": \"cors\",\n" + "  \"credentials\": \"include\"\n"
//					+ "})\n" + "    .then(response => {\n" + "        console.log(response.statusText);\n"
//					+ "        return response.text();\n" + "    });");

			Map<String, Object> request = new HashMap<>();
			request.put("Accept", "*/*");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");
			request.put("Connection", "keep-alive");
			request.put("Content-Length", "51");
			request.put("Content-Type", "application/x-www-form-urlencoded");
			request.put("Host", "www.premint.xyz");
			request.put("Origin", "https://www.premint.xyz");
			request.put("Referer", "https://www.premint.xyz/");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\", \"Google Chrome\";v=\"100\"");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Sec-Fetch-Dest", "empty");
			request.put("Sec-Fetch-Mode", "cors");
			request.put("Sec-Fetch-Site", "same-origin");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");
//			request.put("x-csrftoken", this.csrfToken);

			RawResponse newSession = session.post(this.raffleURL).proxy(Proxies.httpProxy(ip, port)).body(body)
					.headers(request).socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (!response.contains("Here are some answers to commonly asked questions")) {

				if (response.contains("This page is password protected")) {
					confirmEntry(session);
				} else {

					errorCounter++;
					setProxy();

					System.out.println(
							ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - SUBMIT ENTRY FAILED - Retrying..." + ANSI_RESET);
					if (errorCounter >= 3) {
						throw new Exception("TASK_STOPPED");
					} else {
						Thread.sleep(2000);
						submitEntry(session);
					}
				}
			} else {

				Document doc = Jsoup.parse(response);

				Elements elements = doc.getElementsByClass("alert alert-danger alert-dismissible fade show");

				for (Element element : elements) {
					System.out.println(
							ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - MISSING REQUIREMENT: " + element.ownText() + ANSI_RESET);

				}

				String header = doc.getElementsByClass("heading heading-3 mb-2 d-block").text();

				if (header.contains("Registered")) {

					System.out.println(
							ANSI_GREEN + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - Submitted Entry Successfully!" + ANSI_RESET);
					stopCaptchaHarvestor();
					addToResultsCSV("ENTERED");
					this.isFinished = true;
					BackendWebhook b = new BackendWebhook("Entered Premint Raffle", "**Project:** " + this.raffleURL,
							null,
							"https://discord.com/api/webhooks/961206828904087593/GoCjMo5r7RwICjbzeNPfdO0OSZcJwTY-pCeYpXYShZfvTlvnSxyNXA1lcU8YMKUoEsx2");
					System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Sleeping..." + ANSI_RESET);
					Thread.sleep(delay);

				} else {
					System.out.println(
							ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - Waiting 15s Before Confirmation..." + ANSI_RESET);
					Thread.sleep(15000);
					confirmEntry(session);

				}

			}

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Request Error SUBMIT_ENTRY Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(200);
			submitEntry(session);

		}
	}

	public void stopCaptchaHarvestor() {
		if (this.captchaThread != null) {
			this.captchaThread.stopHarvestor();

		}
	}

	synchronized void increaseTasksCounter() {
		this.finishedTasks++;

		cliTools.setTitle("N3RO BOT - Premint Entries - Status: " + this.finishedTasks + "/" + this.tasksAmount);

	}

	public void getCsrfMiddleWareToken(Session session) throws Exception {

		try {

			System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Getting Raffle..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();

			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			RawResponse newSession = session.get(this.raffleURL).proxy(Proxies.httpProxy(ip, port)).headers(request)
					.socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - GET CSRF2 FAILED - Retrying..." + ANSI_RESET);
				Thread.sleep(1000);
				getCsrfMiddleWareToken(session);

			} else {

				if (this.mode.equals("RAFFLE")) {

					if (response.contains("This page is password protected")) {
						System.out.println(
								ANSI_YELLOW + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
										+ "] - [" + taskId + "] - Detected Password. Bypassing..." + ANSI_RESET);
						this.hasPassword = true;
					} else if (response.contains("fas fa-check-circle text-success mr-2\"></i>Registered")) {
						this.isFinished = true;
						increaseTasksCounter();
						throw new Exception("WALLET_ALREADY_PARTICIPATING");
					} else if (response.contains("This list is no longer accepting entries.</div>")) {
						this.isFinished = true;
						increaseTasksCounter();
						throw new Exception("REGISTRATION_ALREADY_CLOSED");
					}

					String[] arr = response.split(Pattern.quote("csrfmiddlewaretoken\" value=\""));
					String temp = arr[1];
					String[] arr2 = temp.split(Pattern.quote("\""));
					this.csrfmiddlewaretoken = arr2[0];
					this.cookieslist = session.currentCookies();
				}

			}

		} catch (Exception e) {

			if (e.toString().contains("WALLET_ALREADY_PARTICIPATING")) {
				throw new Exception(e.getMessage());
			} else if (e.toString().contains("REGISTRATION_ALREADY_CLOSED")) {
				throw new Exception(e.getMessage());
			} else {

				if (e.getMessage().contains("Index 1 out of bounds for length 1")) {
					System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Request Error Retrying: "
							+ "CHECK RAFFLE REQUIREMENTS / SOCIAL ACCOUNTS" + ANSI_RESET);
					errorCounter++;
				} else {
					System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Request Error Retrying: "
							+ parsePWError(e.getMessage()) + ANSI_RESET);
				}

				if (errorCounter >= 3) {
					addToResultsCSV("FAILED");
					throw new Exception("TASK_STOPPED");
				} else {
					Thread.sleep(2000);
					getCsrfMiddleWareToken(session);
				}
			}

		}

	}

	public void confirmEntry(Session session) throws Exception {
		try {
			System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Confirming Entry..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();

			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

			RawResponse newSession = session.get(this.raffleURL).proxy(Proxies.httpProxy(ip, port)).headers(request)
					.socksTimeout(30000).connectTimeout(30000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {
				setProxy();

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - CONFIRM ENTRY FAILED - " + newSession.statusCode() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);
				confirmEntry(session);
			} else {
				if (response.contains("success mr-2\"></i>Registered")) {
					System.out.println(
							ANSI_GREEN + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - Submitted Entry Successfully!" + ANSI_RESET);
					stopCaptchaHarvestor();
					addToResultsCSV("ENTERED");
					this.isFinished = true;
					BackendWebhook b = new BackendWebhook("Entered Premint Raffle", "**Project:** " + this.raffleURL,
							null,
							"https://discord.com/api/webhooks/961206828904087593/GoCjMo5r7RwICjbzeNPfdO0OSZcJwTY-pCeYpXYShZfvTlvnSxyNXA1lcU8YMKUoEsx2");
					System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Sleeping..." + ANSI_RESET);
					Thread.sleep(delay);
				} else {
					System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - Submitting Entry Failed!" + ANSI_RESET);
					stopCaptchaHarvestor();
					addToResultsCSV("FAILED");
					this.isFinished = true;
				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Confirm Entry Error Retrying: " + parsePWError(e.getMessage())
					+ ANSI_RESET);
			Thread.sleep(500);
			confirmEntry(session);
		}
	}

	public void checkIfWinner() throws Exception {
		System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
				+ "] - [" + taskId + "] - Checking Status..." + ANSI_RESET);

		Config config = new Config().connectTimeout(30_000).proxy(ip, port, username, password);
		UnirestInstance unirest = new UnirestInstance(config);

		try {
			HttpResponse<String> response = unirest.get(this.raffleURL + "verify/?wallet=" + this.userPublicAddress)

					.header("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
					.asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

				setProxy();

				String error = " - ";
				try {
					error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
				} catch (Exception e) {
					error = error + response;
				}

				System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
						+ "] - [" + taskId + "] - Check Win Failed Retrying: " + response.getStatus() + ANSI_RESET);
				Thread.sleep(2000);

				checkIfWinner();
			} else {

				if (responseBody.contains(">You were selected!</div>")) {
					System.out.println(
							ANSI_GREEN + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
									+ "] - [" + taskId + "] - Win Detected. Congratulations!" + ANSI_RESET);
					addToResultsCSV("WINNER");
					Document doc = Jsoup.parse(responseBody);
					this.imageUrl = doc.getElementsByClass("bg-white border-white").attr("src").strip();

					this.isFinished = true;
					sendWebhook();
					BackendWebhook b = new BackendWebhook("Won Premint Raffle", "**Project:** " + this.raffleURL, null,
							"https://discord.com/api/webhooks/964462433072713749/RAGE87hC5Nm_HfONZ_FWCYjETYNklbGn-mJDrlkmuAT4eFoHVlOO67v_TXYUIVrokTYa");
				} else {
					System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - ["
							+ this.userPublicAddress + "] - [" + taskId + "] - No Win Detected." + ANSI_RESET);
					this.isFinished = true;
					increaseTasksCounter();
				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - Check Win Error Retrying: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			checkIfWinner();
		}

	}

	public void getProfilePage(Session session) throws Exception {
		System.out.println(ANSI_GREY + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
				+ "] - [" + taskId + "] - Getting Profile..." + ANSI_RESET);

		Map<String, Object> request = new HashMap<>();

		request.put("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36");

		RawResponse newSession = session.get("https://www.premint.xyz/profile/").proxy(Proxies.httpProxy(ip, port))
				.headers(request).socksTimeout(30000).connectTimeout(30000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			setProxy();

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
					+ "] - [" + taskId + "] - GET PROFILE FAILED - Retrying..." + ANSI_RESET);
			Thread.sleep(1000);
			getProfilePage(session);

		} else {
//			connectTwitter();
		}
	}

//	public void connectTwitter() throws Exception {
//
//		page.navigate("https://www.premint.xyz/accounts/twitter/login/?process=connect&next=%2Fprofile%2F");
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

	public List<Cookie> getCookies() {
		return this.cookieslist;
	}

	public String getCaptcha(String siteKey) throws Exception {

		if (this.forceCaptcha) {
			if (this.captchaThread == null) {
				this.captchaThread = new CaptchaThreadPremint(this.userPublicAddress, this.raffleURL, siteKey,
						twoCaptchaKeys);
				captchaThread.start();
				return this.captchaThread.getCaptchaToken();
			} else {
				return this.captchaThread.getCaptchaToken();

			}
		} else {
			return "";
		}

	}

	public String parseError(String response) {
		try {
			JSONObject o = new JSONObject(new JSONTokener(response));
			return o.getString("error");
		} catch (Exception e) {
			return response;
		}

	}

	synchronized void addToResultsCSV(String status) throws IOException {
		FileWriter writer = new FileWriter(
				System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntriesResults.csv", true);

		writer.write("\n" + dtf.format(now.now()) + "," + this.raffleURL + "," + this.userPublicAddress + ","
				+ this.privateKey + "," + status);
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

		String title = "Won Premint Raffle";

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
			System.out.println("[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress + "] - ["
					+ taskId + "] - Sent Webhook.");

		} catch (Exception e) {

			System.out.println(ANSI_RED + "[PREMINT] - [" + dtf.format(now.now()) + "] - [" + this.userPublicAddress
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
