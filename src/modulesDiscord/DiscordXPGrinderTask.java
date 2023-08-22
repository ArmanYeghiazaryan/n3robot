package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class DiscordXPGrinderTask extends Thread {

	private String serverID;
	private String channelID;
	private int delayInSeconds;
	private String csvMessage;
	private String discordToken;
	private String proxy;
	private String ip;
	private int port;
	private String username;
	private String password;
	private String fingerPrint;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	List<Cookie> cookies = new ArrayList<Cookie>();
	DiscordUtilities util = new DiscordUtilities();
	private UnirestInstance unirest;
	private Config config;

	public DiscordXPGrinderTask(String serverID, String channelID, String csvMessage, String delayInSeconds,
			String proxy, String discordToken) {
		this.serverID = serverID;
		this.channelID = channelID;
		this.csvMessage = csvMessage;
		this.delayInSeconds = Integer.valueOf(delayInSeconds);
		this.proxy = proxy;
		this.discordToken = discordToken;
	}

	public void run() {
		this.config = new Config().connectTimeout(30_000);
		this.unirest = new UnirestInstance(config);
		try {
			prepareCookies();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - ERROR: " + e2.toString() + " - Retrying..." + ANSI_RESET);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
			run();
		}

		while (true) {
			try {
				setProxy();

				if (this.fingerPrint == null) {
					prepareFingerprint();
				}

				sendMessage();
				System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Sleeping " + delayInSeconds + "s..." + ANSI_RESET);
				Thread.sleep(delayInSeconds * 1000);
			} catch (Exception e) {
				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - ERROR: " + e.toString() + " - Retrying..." + ANSI_RESET);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	public String getLatestMessage() throws Exception {
		System.out.println(
				"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Getting Channel History...");

		HttpResponse<String> response = unirest
				.get("https://discord.com/api/v9/channels/" + channelID + "/messages?limit=50")
				.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
				.header("Accept-Language", "de").header("Authorization", discordToken)
				.header("Referer", "https://discord.com/channels/@me")
				.header("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
				.header("Sec-Ch-Ua-Mobile", "?0").header("Sec-Ch-Ua-Platform", "\"Windows\"")
				.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
				.header("Sec-Fetch-Site", "same-origin")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
				.header("X-Debug-Options", "bugReporterEnabled").header("X-Discord-Locale", "de")
				.header("X-Fingerprint", fingerPrint)
				.header("X-Super-Properties",
						"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
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
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - GET MESSAGE FAILED - " + response.getStatus() + error + ANSI_RESET);
			Thread.sleep(20000);
			getLatestMessage();

		}

		JSONArray a = new JSONArray(new JSONTokener(responseBody));
		JSONObject o = new JSONObject(a.get(0).toString());
		String latestMessage = o.getString("content");
		System.out.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
				+ "] - Scraped Latest Message: " + latestMessage);
		return latestMessage;

	}

	public void prepareCookies() throws Exception {

		unirest.config().addDefaultCookie(util.get__dcfduid().name(), util.get__dcfduid().value());
		unirest.config().addDefaultCookie(util.get__sdcfduid().name(), util.get__sdcfduid().value());
		unirest.config().addDefaultCookie(util.getLocaleCookie().name(), util.getLocaleCookie().value());
		unirest.config().addDefaultCookie(util.getConsentCookie().name(), util.getConsentCookie().value());
		unirest.config().addDefaultCookie(util.getCFBMCookie().name(), util.getCFBMCookie().value());

	}

	public void prepareFingerprint() throws Exception {
		System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
				+ "] - Getting Metadata..." + ANSI_RESET);

		HttpResponse<String> response = unirest.get("https://discord.com/api/v9/experiments")
				.header("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
				.header("Sec-Ch-Ua-Mobile", "?0").header("Sec-Ch-Ua-Platform", "\"Windows\"")
				.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
				.header("Sec-Fetch-Site", "same-origin")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
				.asString();

		String responseBody = response.getBody();

		if (response.getStatus() != 200) {

			setProxy();

			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - GET FINGERPRINT FAILED - " + response.getStatus() + " - Retrying..." + ANSI_RESET);
			Thread.sleep(5000);
			prepareFingerprint();

		}

		JSONObject a = new JSONObject(responseBody);
		this.fingerPrint = a.getString("fingerprint");
	}

	public String getAIResponse(String message) throws Exception {

		System.out
				.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Generating Answer...");
		String encodedMessage = URLEncoder.encode(message, "UTF-8");// changed

		Session session = Requests.session();
		RawResponse newSession = session
				.get("https://www.cleverbot.com/getreply?key=CC9e9kPowmA9dRxQ_o0pUotcKRQ&input=" + encodedMessage)
				.socksTimeout(120_000).connectTimeout(120_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - GENERATE ANSWER FAILED - " + newSession.statusCode() + " - " + response.strip()
					+ ANSI_RESET);
			Thread.sleep(3000);
			getAIResponse(message);

		}

		String answer = new JSONObject(new JSONTokener(response)).getString("output");

		return prepareAIResponse(answer);

	}

	public String prepareAIResponse(String answer) {
		// remove .
		if (answer.charAt(answer.length() - 1) == '.') {
			answer = answer.substring(0, answer.length() - 1);
		}

		return answer;
	}

	public void sendMessage() throws Exception {
		try {
			String answer = getAnswer(getLatestMessage());

			String body = "{\"content\":\"" + answer + "\",\"nonce\":\"" + getNonce() + "\",\"tts\":false}";

			System.out.println(ANSI_YELLOW + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Sending Answer: " + answer + ANSI_RESET);
			HttpResponse<String> response = unirest
					.post("https://discord.com/api/v9/channels/" + channelID + "/messages")
					.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
					.header("Accept-Language", "de").header("Authorization", discordToken)
					.header("Content-Type", "application/json").header("Referer", "https://discord.com/channels/@me")
					.header("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
					.header("Sec-Ch-Ua-Mobile", "?0").header("Sec-Ch-Ua-Platform", "\"Windows\"")
					.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
					.header("Sec-Fetch-Site", "same-origin")
					.header("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
					.header("X-Debug-Options", "bugReporterEnabled").header("X-Discord-Locale", "de")
					.header("X-Fingerprint", fingerPrint)
					.header("X-Super-Properties",
							"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
					.body(body).asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

				setProxy();

				String error = " - ";
				try {
					error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
				} catch (Exception e) {
					error = error + response;
				}
				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - POST MESSAGE FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(30000);
				sendMessage();

			}

			System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Answer Sent Successfully!" + ANSI_RESET);
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			sendMessage();
		}

	}

	public String getAnswer(String message) throws Exception {

		if (this.csvMessage.toLowerCase().equals("ai")) {
			String bannedWord = "bot";

			String response = getAIResponse(message);

			while (response.contains(bannedWord)) {
				response = getAIResponse(message);
			}

			return response;
		} else if (this.csvMessage.toLowerCase().equals("file")) {
			// get random string from file
			return getPresetMessage();
		} else {
			return this.csvMessage;
		}

	}

	public String getPresetMessage() throws Exception {
		List<String> messages = new ArrayList<String>();

		// GENERATE RANDOM PROXY
		File file = new File(System.getProperty("user.dir") + "\\tasks\\discord\\presetMessages.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				messages.add(line);
			}
		}

		int randomNum = ThreadLocalRandom.current().nextInt(0, messages.size());

		return messages.get(randomNum);

	}

	public String getNonce() {
		StringBuilder sb = new StringBuilder();
		Random ran = new Random();

		String example = "150900819294101621";
		for (int i = 0; i < example.length(); i++) {
			String x = String.valueOf(ran.nextInt(10 - 1 + 1) + 1);
			sb.append(x);
		}

		return sb.toString();
	}

	public void setProxy() throws Exception {
		if (this.proxy.toLowerCase().equals("random")) {
			String[] p = getProxy().split(":");
			this.ip = p[0];
			this.port = Integer.valueOf(p[1]);
			this.username = p[2];
			this.password = p[3];

		} else {
			String[] p = proxy.split(":");
			this.ip = p[0];
			this.port = Integer.valueOf(p[1]);

			this.username = p[2];
			this.password = p[3];

		}

		String oldCookies = config.getDefaultHeaders().get("Cookie").toString().replace("[", "").replace("]", "")
				.replace(",", ";");

		Config newConfig = new Config().addDefaultHeader("Cookie", oldCookies)
				.connectTimeout(config.getConnectionTimeout()).proxy(ip, port, username, password);

		this.unirest = new UnirestInstance(newConfig);
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
