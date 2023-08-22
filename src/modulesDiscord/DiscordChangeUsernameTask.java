package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class DiscordChangeUsernameTask extends Thread {

	private String discordToken;
	private String userName;
	private String password;
	private String proxy;
	private String ip;
	private int port;
	private String username;
	private String discordPassword;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	DiscordUtilities util = new DiscordUtilities();
	private UnirestInstance unirest;
	private Config config;

	public DiscordChangeUsernameTask(String userName, String password, String proxy, String discordToken) {
		this.userName = userName;
		this.discordPassword = password;
		this.proxy = proxy;
		this.discordToken = discordToken;
	}

	public void run() {
		try {
			this.config = new Config().connectTimeout(30_000);
			this.unirest = new UnirestInstance(config);
			setProxy();
			prepareCookies();
			changeUserName();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void prepareCookies() throws Exception {

		unirest.config().addDefaultCookie(util.get__dcfduid().name(), util.get__dcfduid().value());
		unirest.config().addDefaultCookie(util.get__sdcfduid().name(), util.get__sdcfduid().value());
		unirest.config().addDefaultCookie(util.getConsentCookie().name(), util.getConsentCookie().value());
		unirest.config().addDefaultCookie(util.getCFBMCookie().name(), util.getCFBMCookie().value());
		unirest.config().addDefaultCookie(util.getLocaleCookie().name(), util.getLocaleCookie().value());

	}

	public String getRandomImage() throws Exception {
		Random rand = new Random();
		File[] files = new File(System.getProperty("user.dir") + "\\tasks\\discord\\images").listFiles();
		File randomPic = files[rand.nextInt(files.length)];

		byte[] fileContent = FileUtils.readFileToByteArray(randomPic);
		String encodedString = Base64.getEncoder().encodeToString(fileContent);
		return encodedString;
	}

	public void changeUserName() throws Exception {

		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Changing Username...");

			String body = "{\"username\":\"" + userName + "\",\"password\":\"" + discordPassword + "\"}";

			HttpResponse<String> response = unirest.patch("https://discord.com/api/v9/users/@me")

					.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
					.header("Accept-Language", "de").header("Authorization", discordToken)
					.header("Content-Type", "application/json").header("Origin", "https://discord.com")
					.header("Referer", "https://discord.com/channels/@me")
					.header("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
					.header("Sec-Ch-Ua-Mobile", "?0").header("Sec-Ch-Ua-Platform", "\"Windows\"")
					.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
					.header("Sec-Fetch-Site", "same-origin")
					.header("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
					.header("X-Debug-Options", "bugReporterEnabled").header("X-Discord-Locale", "de")
					.header("X-Super-Properties",
							"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
					.body(body).asString();

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

				if (responseBody.contains("PASSWORD_DOES_NOT_MATCH")) {
					error = " - PASSWORD_DOES_NOT_MATCH";
				}

				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - CHANGE USERNAME FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(2000);

				// Only loop if unknown codes!
				if (response.getStatus() != 404) {
					changeUserName();
				}
			} else {
				System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Succssfully Changed Username!" + ANSI_RESET);
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			changeUserName();
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
