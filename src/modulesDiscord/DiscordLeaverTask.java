package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public class DiscordLeaverTask extends Thread {

	private String providedServerID;
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
	HashSet<String> serverSet = new HashSet<String>();
	private UnirestInstance unirest;
	private Config config;

	public DiscordLeaverTask(String providedServerID, String proxy, String discordToken) {
		this.providedServerID = providedServerID;
		this.proxy = proxy;
		this.discordToken = discordToken;
	}

	public void run() {
		try {

			this.config = new Config().connectTimeout(30_000);
			this.unirest = new UnirestInstance(config);
			setProxy();
			prepareCookies();
			prepareFingerprint();
			prepareServerSet();
			leaveServers();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void prepareCookies() throws Exception {

		unirest.config().addDefaultCookie(util.get__dcfduid().name(), util.get__dcfduid().value());
		unirest.config().addDefaultCookie(util.get__sdcfduid().name(), util.get__sdcfduid().value());
		unirest.config().addDefaultCookie(util.getLocaleCookie().name(), util.getLocaleCookie().value());
		unirest.config().addDefaultCookie(util.getConsentCookie().name(), util.getConsentCookie().value());
		unirest.config().addDefaultCookie(util.getCFBMCookie().name(), util.getCFBMCookie().value());

	}

	public void prepareServerSet() throws Exception {
		if (this.providedServerID.toLowerCase().equals("all")) {
			getUserGuilds();
		} else {
			this.serverSet.add(providedServerID);
		}
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

	public void getUserGuilds() throws Exception {
		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Getting Joined Servers...");

			HttpResponse<String> response = unirest.get("https://discord.com/api/v9/users/@me/guilds")
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

				if (response.getStatus() == 401) {
					throw new Exception("ACCOUNT_CLIPPED");
				}

				setProxy();

				String error = " - ";
				try {
					error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
				} catch (Exception e) {
					error = error + response;
				}
				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - GET USER SERVERS FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(5000);

				getUserGuilds();
			} else {
				JSONArray a = new JSONArray(new JSONTokener(responseBody));

				System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Amount Joined Servers: " + a.length() + ANSI_RESET);
				for (int i = 0; i < a.length(); i++) {
					String entry = a.get(i).toString();
					JSONObject o = new JSONObject(new JSONTokener(entry));
					String id = o.getString("id");

					this.serverSet.add(id);

				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getUserGuilds();
		}
	}

	public void leaveServers() throws Exception {
		try {

			for (String currentServer : serverSet) {
				System.out.println(
						"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Leaving Server...");
				String body = "{\"lurking\":false}";

				HttpResponse<String> response = unirest
						.delete("https://discord.com/api/v9/users/@me/guilds/" + currentServer)
						.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
						.header("Accept-Language", "de").header("Authorization", discordToken)
//						.header("Content-Length", "17")
						.header("Content-Type", "application/json").header("Origin", "https://discord.com")
						.header("Referer", "https://discord.com/channels/@me")// Possible
						// ban
						.header("Sec-Ch-Ua",
								"\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
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

				if (response.getStatus() != 204) {

					if (response.getStatus() == 401) {
						throw new Exception("ACCOUNT_CLIPPED");
					}

					setProxy();

					String error = " - ";
					try {
						error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
					} catch (Exception e) {
						error = error + response;
					}
					System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
							+ "] - LEAVE SERVER FAILED - " + response.getStatus() + error + ANSI_RESET);
					Thread.sleep(2000);

					// Only loop if unknown codes!
					if (response.getStatus() != 404) {
						leaveServers();
					}
				} else {
					System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
							+ "] - Succssfully Left Server '" + currentServer + "'!" + ANSI_RESET);
					Thread.sleep(2000);
				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			leaveServers();
		}
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
