package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import main.BackendWebhook;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import net.dongliu.requests.utils.Cookies;

public class DiscordJoinerTask extends Thread {

	private int taskID;
	private String invite;
	private String preHarvestCaptcha;
	private HashSet<String> twoCaptchaKeys;
	private String discordToken;
	private String webhookUrl;
	private String siteKey = "4c672d35-0701-42b2-88c3-78380b0db560";
	private String captchaData;
	private String captchaToken = "";

	private String serverID;
	private String serverName;
	private String welcomeChannelID;
	private String welcomeChannelType;
	private String proxy;
	private String ip;
	private int port;
	private String username;
	private String password;
	List<Cookie> cookies = new ArrayList<Cookie>();
	DiscordUtilities util = new DiscordUtilities();
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	CaptchaThreadDiscord captchaThread;
	// twitter
	private String twitterID;
	private HashSet<String> tweetsSet = new HashSet<String>();
	private TwitterMonitorTask twitterMonitorTask;
	private UnirestInstance unirest;
	private Config config;

	public DiscordJoinerTask(int taskID, String invite, String proxy, String preHarvestCaptcha,
			HashSet<String> twoCaptchaKeys, String discordToken, String webhookUrl,
			TwitterMonitorTask twitterMonitorTask) {
		this.taskID = taskID;
		this.invite = invite;
		this.proxy = proxy;
		this.preHarvestCaptcha = preHarvestCaptcha;
		this.twoCaptchaKeys = twoCaptchaKeys;
		this.discordToken = discordToken;
		this.webhookUrl = webhookUrl;
		this.twitterMonitorTask = twitterMonitorTask;
	}

	public void run() {
		System.err.close();

		try {
			this.config = new Config().connectTimeout(30_000);
			this.unirest = new UnirestInstance(config);
			initializeCaptchaThread();
			initializeMonitor();
			setProxy();
			prepareCookies();
			getServerData();
			getXContextProperties();
			joinServer();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void initializeCaptchaThread() {

		String url = "";
		if (invite == null) {
			url = "https://discord.com/api/v9/invites/";
		} else {
			url = "https://discord.com/api/v9/invites/" + invite;
		}
		if (this.preHarvestCaptcha.toLowerCase().equals("true") && this.captchaThread == null) {
			this.captchaThread = new CaptchaThreadDiscord(discordToken, url, this.siteKey, this.captchaData,
					twoCaptchaKeys);
			captchaThread.start();
		}
	}

	public void initializeMonitor() throws Exception {
		if (this.invite.toLowerCase().equals("wait")) {
			monitorInviteViaWait();
		} else if (this.invite.charAt(0) == '@') {
			// start twitter monitor
			while (twitterMonitorTask.getInvite() == null) {
				Thread.sleep(1000);
			}
			this.invite = twitterMonitorTask.getInvite();

		}
	}

	public void monitorInviteViaWait() throws Exception {

		String inviteInput = getXRecord(taskID).get("server_invite [INVITE / WAIT]");
		if (!inviteInput.toLowerCase().equals("wait")) {
			this.invite = inviteInput;
		} else {
			System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Waiting For Invite..." + ANSI_RESET);
			Thread.sleep(1500);
			monitorInviteViaWait();
		}

	}

	public CSVRecord getXRecord(int recordID) throws Exception {
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerJoiner.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		int id = 0;
		for (CSVRecord record : records) {
			if (id == recordID) {
				in.close();
				return record;
			} else {
				id++;
			}
		}

		in.close();
		return null;
	}

	public void getServerData() throws Exception {
		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Getting Server: " + invite);
			HttpResponse<String> response = unirest.get("https://ptb.discord.com/api/v9/invites/" + invite)
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
					.header("X-Super-Properties",
							"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
					.asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200 || !responseBody.contains("guild\":")) {

				setProxy();

				String error = " - ";
				try {
					error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
				} catch (Exception e) {
					error = error + response;
				}
				System.out.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - GET SERVER FAILED - " + response.getStatus() + error);
				Thread.sleep(5000);

				getServerData();
			} else {
				JSONObject o = new JSONObject(new JSONTokener(responseBody));
				this.serverID = o.getJSONObject("guild").getString("id");
				this.serverName = o.getJSONObject("guild").getString("name");
				this.welcomeChannelID = o.getJSONObject("channel").getString("id");
				this.welcomeChannelType = String.valueOf(o.getJSONObject("channel").getInt("type"));

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getServerData();
		}
	}

	public void joinServer() throws Exception {
		try {
			System.out.println(ANSI_YELLOW + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Joining Server: " + serverName + ANSI_RESET);

			String body = "{}";

			if (preHarvestCaptcha.toLowerCase().equals("false")) {
				body = "{}";
			} else {
				body = "{\"captcha_key\": \"" + captchaThread.getCaptchaToken() + "\", \"captcha_rqtoken\": \""
						+ this.captchaToken + "\"}";
//				System.out.println(body);
			}

			this.invite = invite.replace("https://discord.gg/", "");

			HttpResponse<String> response = unirest.post("https://discord.com/api/v9/invites/" + invite)
					.header("Host", "discord.com")
					.header("User-Agent",
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) discord/0.0.61 Chrome/91.0.4472.164 Electron/13.6.6 Safari/537.36")
					.header("Accept", "*/*").header("Accept-Language", "en-US,en;q=0.5")
					.header("Content-Type", "application/json").header("X-Context-Properties", getXContextProperties())
					.header("Authorization", discordToken)
					.header("X-Super-Properties",
							"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
					.header("X-Discord-Locale", "en-US").header("X-Debug-Options", "bugReporterEnabled")
					.header("Origin", "https://discord.com").header("Referer", "https://discord.com/channels/@me")
					.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
					.header("Sec-Fetch-Site", "same-origin")

					.body(body).asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

				if (responseBody.contains("captcha_service\": \"hcaptcha")) {
					System.out.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
							+ "] - Captcha Detected. Retrying...");

					JSONObject o = new JSONObject(responseBody);
					this.siteKey = o.getString("captcha_sitekey");
					this.captchaToken = o.getString("captcha_rqtoken");

					try {
						this.captchaData = o.getString("captcha_rqdata");

					} catch (Exception e) {

					}

					// Activate CaptchaThread
					this.preHarvestCaptcha = "TRUE";
					initializeCaptchaThread();
				} else {

					setProxy();

					String error = " - ";
					try {
						error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
					} catch (Exception e) {
						error = error + response;
					}
					System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
							+ "] - JOIN SERVER FAILED - " + response.getStatus() + error + ANSI_RESET);
					Thread.sleep(5000);
				}

				joinServer();

			} else {

				System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - SUCCESSFULLY JOINED: " + serverName + ANSI_RESET);

				if (this.captchaThread != null) {
					this.captchaThread.stopHarvestor();
				}
				BackendWebhook b = new BackendWebhook("Joined Discord Server", "**Name:** " + serverName, null,
						"https://discord.com/api/webhooks/961206828904087593/GoCjMo5r7RwICjbzeNPfdO0OSZcJwTY-pCeYpXYShZfvTlvnSxyNXA1lcU8YMKUoEsx2");

				if (responseBody.contains("MEMBER_VERIFICATION_GATE_ENABLED")) {
					getVerificationForm();
				}

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			joinServer();
		}
	}

	public void getVerificationForm() throws Exception {
		try {

			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Getting Verification...");

			HttpResponse<String> response = unirest
					.get("https://discord.com/api/v9/guilds/" + serverID
							+ "/member-verification?with_guild=false&invite_code=" + invite)
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
				System.out.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - GET VERIFICATION FAILED - " + response.getStatus() + error);
				Thread.sleep(2000);

				getVerificationForm();
			} else {
				JSONObject o = new JSONObject(new JSONTokener(responseBody));
				String version = o.getString("version");
				String field_type = o.getJSONArray("form_fields").getJSONObject(0).getString("field_type");
				String label = o.getJSONArray("form_fields").getJSONObject(0).getString("label");
				Object description = o.getJSONArray("form_fields").getJSONObject(0).get("description");
				Object automations = o.getJSONArray("form_fields").getJSONObject(0).get("automations");
				Object required = o.getJSONArray("form_fields").getJSONObject(0).get("required");

				JSONArray values = o.getJSONArray("form_fields").getJSONObject(0).getJSONArray("values");

				JSONObject body = new JSONObject();
				body.put("version", version);
				JSONArray formFieldsArray = new JSONArray();
				formFieldsArray.put(new JSONObject().put("field_type", field_type).put("label", label)
						.put("description", description).put("automations", automations).put("required", required)
						.put("values", values).put("response", true));
				body.put("form_fields", formFieldsArray);

				postVerificationForm(body.toString());
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getVerificationForm();
		}
	}

	public void postVerificationForm(String body) throws Exception {
		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Sending Verification...");

			HttpResponse<String> response = unirest
					.put("https://discord.com/api/v9/guilds/" + serverID + "/requests/@me")
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
					.header("X-Super-Properties",
							"eyJvcyI6IldpbmRvd3MiLCJicm93c2VyIjoiQ2hyb21lIiwiZGV2aWNlIjoiIiwic3lzdGVtX2xvY2FsZSI6ImRlIiwiYnJvd3Nlcl91c2VyX2FnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzk4LjAuNDc1OC4xMDIgU2FmYXJpLzUzNy4zNiIsImJyb3dzZXJfdmVyc2lvbiI6Ijk4LjAuNDc1OC4xMDIiLCJvc192ZXJzaW9uIjoiMTAiLCJyZWZlcnJlciI6IiIsInJlZmVycmluZ19kb21haW4iOiIiLCJyZWZlcnJlcl9jdXJyZW50IjoiIiwicmVmZXJyaW5nX2RvbWFpbl9jdXJyZW50IjoiIiwicmVsZWFzZV9jaGFubmVsIjoic3RhYmxlIiwiY2xpZW50X2J1aWxkX251bWJlciI6MTE4MjA1LCJjbGllbnRfZXZlbnRfc291cmNlIjpudWxsfQ==")
					.body(body).asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 201) {

				setProxy();

				String error = " - ";
				try {
					error = error + new JSONObject(new JSONTokener(responseBody)).getString("message");
				} catch (Exception e) {
					error = error + response;
				}
				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - SEND VERIFICATION FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(2000);

			} else {
				System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Verified Form Successfully!" + ANSI_RESET);

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			postVerificationForm(body);
		}
	}

	public String getXContextProperties() {
		String properties = "{\"location\":\"Join Guild\",\"location_guild_id\":\"" + serverID
				+ "\",\"location_channel_id\":\"" + welcomeChannelID + "\",\"location_channel_type\":"
				+ welcomeChannelType + "";

		String encodedString = Base64.getEncoder().encodeToString(properties.getBytes());

		return encodedString;
	}

	public void prepareCookies() throws Exception {

		Cookie dcfduid = util.get__dcfduid();
		Cookie sdcfduid = util.get__sdcfduid();

		unirest.config().addDefaultCookie(dcfduid.name(), dcfduid.value());
		unirest.config().addDefaultCookie(sdcfduid.name(), sdcfduid.value());
		unirest.config().addDefaultCookie(util.getLocaleCookie().name(), util.getLocaleCookie().value());
		unirest.config().addDefaultCookie(util.getConsentCookie().name(), util.getConsentCookie().value());
		unirest.config().addDefaultCookie(util.getCFBMCookie().name(), util.getCFBMCookie().value());

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