package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

import emoji4j.EmojiUtils;
import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class DiscordReactionAdder extends Thread {

	private String serverID;
	private String channelID;
	private String messageID;
	private String providedReaction;
	private String waitForRole;
	private String discordToken;
	private String userID;
	private String proxy;
	private String ip;
	private int port;
	private String username;
	private String password;
	List<Cookie> cookies = new ArrayList<Cookie>();
	DiscordUtilities util = new DiscordUtilities();
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private String fingerPrint;
	HashSet<String> rolesSet = new HashSet<String>();
	private String urlEncodedReaction;
	private String urlDecodedReaction;
	private UnirestInstance unirest;
	private Config config;

	public DiscordReactionAdder(String channelID, String messageID, String reaction, String waitForRole,
			String discordToken, String proxy) {
		System.err.close();

		this.channelID = channelID;
		this.messageID = messageID;
		this.providedReaction = reaction;
		this.waitForRole = waitForRole;
		this.discordToken = discordToken;
		this.proxy = proxy;
	}

	public void run() {
		try {

			this.config = new Config().connectTimeout(30_000);
			this.unirest = new UnirestInstance(config);
			setProxy();
			prepareCookies();
			getUserInfo();
			prepareEmoji();
			getServer();
			initializeWaitingForRole();
			putReaction();
			checkUserHasNewRoles();
			// monitor roles
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getUserInfo() throws Exception {

		try {
			HttpResponse<String> response = unirest.get("https://discord.com/api/v9/users/@me")
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
						+ "] - GET USER FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(5000);

				getUserInfo();
			} else {

				// set data
				JSONObject o = new JSONObject(new JSONTokener(responseBody));
				this.userID = o.getString("id");

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getUserInfo();
		}

	}

	public void getServer() throws Exception {
		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Getting Server...");

			HttpResponse<String> response = unirest.get("https://discord.com/api/v9/channels/" + channelID)
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
						+ "] - GET MESSAGE SERVER FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(5000);

				getServer();
			} else {
				JSONObject o = new JSONObject(new JSONTokener(responseBody));

				this.serverID = o.getString("guild_id");

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getServer();
		}
	}

	public void prepareEmoji() throws Exception {

		if (util.getEmoji(providedReaction) != null) {
			this.urlEncodedReaction = util.getEmoji(providedReaction);
			this.urlDecodedReaction = URLDecoder.decode(util.getEmoji(providedReaction), Charset.defaultCharset());

		} else {
			System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Emoji Not Found. Checking Message Emotes..." + ANSI_RESET);
			getEmoteFromMessage();
		}

	}

	public void getEmoteFromMessage() throws Exception {
		try {
			HttpResponse<String> response = unirest
					.get("https://discord.com/api/v9/channels/" + channelID + "/messages")
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
						+ "] - GET EMOTE FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(5000);

				getEmoteFromMessage();
			} else {
				// ...
				JSONArray arr = new JSONArray(new JSONTokener(responseBody));

				for (int i = 0; i < arr.length(); i++) {
					JSONObject o = new JSONObject(new JSONTokener(arr.get(i).toString()));

					if (o.get("id").equals(messageID)) {

						JSONArray reactions = o.getJSONArray("reactions");

						for (int j = 0; j < reactions.length(); j++) {
							String currentReaction = reactions.get(j).toString();
							JSONObject currentReactionObject = new JSONObject(new JSONTokener(currentReaction));

							String reactionName = currentReactionObject.getJSONObject("emoji").getString("name");
							if (reactionName.equals(this.providedReaction)) {
								String reactionID = currentReactionObject.getJSONObject("emoji").getString("id");
								if (!reactionID.equals(null)) {
									this.urlEncodedReaction = URLEncoder.encode(reactionName + ":" + reactionID,
											StandardCharsets.UTF_8);
									return;
								}
							}
						}

					}

				}

				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - MESSAGE DOES NOT CONTAIN EMOTE - Retrying..." + ANSI_RESET);
				Thread.sleep(1500);

				getEmoteFromMessage();
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getEmoteFromMessage();
		}
	}

	public void initializeWaitingForRole() throws Exception {
		try {
			if (this.waitForRole.toLowerCase().equals("true")) {

				HttpResponse<String> response = unirest
						.get("https://discord.com/api/v9/guilds/" + serverID + "/members/" + userID)
						.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
						.header("Accept-Language", "de").header("Authorization", discordToken)
						.header("Referer", "https://discord.com/channels/@me")
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
							+ "] - PREFILL USER ROLES FAILED - " + response.getStatus() + error + ANSI_RESET);
					Thread.sleep(5000);

					initializeWaitingForRole();
				} else {

					JSONArray a = new JSONObject(new JSONTokener(responseBody)).getJSONArray("roles");
					for (int i = 0; i < a.length(); i++) {
						String roleID = a.getString(i);

						rolesSet.add(roleID);

					}

				}

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			initializeWaitingForRole();
		}
	}

	public void putReaction() throws Exception {
		try {
			System.out.println(
					"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken + "] - Adding Reaction...");

			HttpResponse<String> response = unirest
					.put("https://discord.com/api/v9/channels/" + channelID + "/messages/" + messageID + "/reactions/"
							+ urlEncodedReaction + "/%40me")
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
						+ "] - PUT REACTION FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(2000);

				// Only loop if unknown codes!
				if (response.getStatus() != 404) {
					putReaction();
				}
			} else {
				System.out.println(ANSI_YELLOW + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Added Reaction! Reconfirming In 5 Seconds..." + ANSI_RESET);
				Thread.sleep(5_000);
				checkIfReactionIsSet();

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			putReaction();
		}
	}

	public void checkIfReactionIsSet() throws Exception {
		try {
			System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Getting Reactions..." + ANSI_RESET);

			HttpResponse<String> response = unirest
					.get("https://discord.com/api/v9/channels/" + channelID + "/messages")
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
						+ "] - GET EMOTE FAILED - " + response.getStatus() + error + ANSI_RESET);
				Thread.sleep(1500);

				checkIfReactionIsSet();
			} else {
				// ...
				JSONArray arr = new JSONArray(new JSONTokener(responseBody));

				for (int i = 0; i < arr.length(); i++) {
					JSONObject currentMessageObject = new JSONObject(new JSONTokener(arr.get(i).toString()));

					if (currentMessageObject.get("id").equals(messageID)) {

						JSONArray reactions = currentMessageObject.getJSONArray("reactions");

						boolean foundReaction = false;
						for (int j = 0; j < reactions.length(); j++) {
							String currentReaction = reactions.get(j).toString();
							JSONObject currentReactionObject = new JSONObject(new JSONTokener(currentReaction));

							String reactionName = currentReactionObject.getJSONObject("emoji").getString("name");

							boolean isSet = currentReactionObject.getBoolean("me");

							if (isSet) {

								foundReaction = true;
								System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - ["
										+ discordToken + "] - Reaction Confirmed!" + ANSI_RESET);
								return;
							}

						}
						if (!foundReaction) {
							System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - ["
									+ discordToken + "] - Reaction Not Found! Adding Again..." + ANSI_RESET);

							putReaction();
						}

					}

				}

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			checkIfReactionIsSet();
		}

	}

	public void checkUserHasNewRoles() throws Exception {
		try {
			if (waitForRole.toLowerCase().equals("true")) {

				System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
						+ "] - Checking Roles..." + ANSI_RESET);

				HttpResponse<String> response = unirest
						.get("https://discord.com/api/v9/guilds/" + serverID + "/members/" + userID)
						.header("Authority", "discord.com").header("Accept", "*/*").header("Accept-Encoding", "gzip")
						.header("Accept-Language", "de").header("Authorization", discordToken)
						.header("Referer", "https://discord.com/channels/@me")
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
							+ "] - GET USER ROLES FAILED - " + response.getStatus() + error + ANSI_RESET);
					Thread.sleep(5000);

					checkUserHasNewRoles();
				} else {
					JSONArray a = new JSONObject(new JSONTokener(responseBody)).getJSONArray("roles");
					for (int i = 0; i < a.length(); i++) {
						String roleID = a.getString(i);

						if (!rolesSet.contains(roleID)) {
							System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - ["
									+ discordToken + "] - Role '" + roleID + "' Assigned To User!" + ANSI_RESET);
							return;
						}

					}

					// role not found!
					System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
							+ "] - New Roles Missing. Monitoring..." + ANSI_RESET);
					Thread.sleep(3000);
					checkIfReactionIsSet();
					Thread.sleep(3000);
					checkUserHasNewRoles();
				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + discordToken
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			checkUserHasNewRoles();
		}
	}

	public void prepareCookies() throws Exception {

		unirest.config().addDefaultCookie(util.get__dcfduid().name(), util.get__dcfduid().value());
		unirest.config().addDefaultCookie(util.get__sdcfduid().name(), util.get__sdcfduid().value());
		unirest.config().addDefaultCookie(util.getLocaleCookie().name(), util.getLocaleCookie().value());
		unirest.config().addDefaultCookie(util.getConsentCookie().name(), util.getConsentCookie().value());
		unirest.config().addDefaultCookie(util.getCFBMCookie().name(), util.getCFBMCookie().value());

		prepareFingerprint();

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
