package modulesDiscord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class TwitterMonitorTask extends Thread {

	private String tag;
	private String invite;
	private HashSet<String> twitterTokens;
	private HashSet<String> tweetsSet = new HashSet<String>();
	private String twitterID;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public TwitterMonitorTask(String tag, HashSet<String> twitterTokens) {
		this.tag = tag;
		this.twitterTokens = twitterTokens;
	}

	public void run() {

		try {
			getTwitterHandleID();
			monitorTweets(true);

			while (this.invite == null) {
				monitorTweets(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getTwitterHandleID() throws Exception {
		try {

			System.out.println("[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag + "] - Getting Twitter...");

			Map<String, Object> request = new HashMap<>();
			request.put("Authorization", "Bearer " + getRandomTwitterToken());

			Session session = Requests.session();
			RawResponse newSession = session
					.get("https://api.twitter.com/2/users/by/username/" + this.tag.replace("@", "")).headers(request)
					.socksTimeout(60_000).connectTimeout(60_000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
						+ "] - FAILED TO GET TWITTER HANDLE - " + newSession.statusCode() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);

				getTwitterHandleID();

			} else {
				this.twitterID = new JSONObject(new JSONTokener(response)).getJSONObject("data").getString("id");
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			getTwitterHandleID();
		}
	}

	public void monitorTweets(boolean firstRun) throws Exception {
		try {
			System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
					+ "] - Monitoring Tweets..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();
			request.put("Authorization", "Bearer " + getRandomTwitterToken());

			Session session = Requests.session();
			RawResponse newSession = session
					.get("https://api.twitter.com/1.1/statuses/user_timeline.json?user_id=" + twitterID)
					.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
						+ "] - FAILED TO GET TWEETS - " + newSession.statusCode() + " - Retrying..." + ANSI_RESET);

				throw new Exception("GET_TWEETS_FAILED");

			} else {

				JSONArray tweets = new JSONArray(new JSONTokener(response));

				for (int i = 0; i < tweets.length(); i++) {
					JSONObject o = new JSONObject(new JSONTokener(tweets.get(i).toString()));
					String tweetID = o.getString("id_str");
					if (firstRun) {
						tweetsSet.add(tweetID);
					} else {
						if (!tweetsSet.contains(tweetID)) {
							if (lookUpTweet(tweetID)) {
								return;
							}
							tweetsSet.add(tweetID);
						}
					}

				}

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
					+ "] - Request Error: " + e.getMessage() + ANSI_RESET);
		}

		Thread.sleep(2000); // monitor delay for tweets

	}

	public boolean lookUpTweet(String tweetID) throws Exception {

		System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag + "] - Getting Tweet: "
				+ tweetID + ANSI_RESET);

		Map<String, Object> request = new HashMap<>();
		request.put("Authorization", "Bearer " + getRandomTwitterToken());

		Session session = Requests.session();
		RawResponse newSession = session
				.get("https://api.twitter.com/2/tweets?ids=" + tweetID + "&expansions=&tweet.fields=entities")
				.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
					+ "] - FAILED TO GET TWEET - " + newSession.statusCode() + " - Retrying..." + ANSI_RESET);
			Thread.sleep(500);

			lookUpTweet(tweetID);

		} else {

			try {
				JSONObject o = new JSONObject(new JSONTokener(response));
				JSONArray a = o.getJSONArray("data").getJSONObject(0).getJSONObject("entities").getJSONArray("urls");

				for (int i = 0; i < a.length(); i++) {
					o = new JSONObject(new JSONTokener(a.get(i).toString()));
					String url = o.getString("expanded_url");
					if (url.contains("discord.gg")) {
						String[] arr = url.split(Pattern.quote("discord.gg/"));

						this.invite = arr[1].replace("/", "");
						System.out.println(ANSI_YELLOW + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
								+ "] - Discord Invite Found: " + invite + ANSI_RESET);
						return true;
					}

				}
			} catch (Exception e) {
				if (e.toString().contains("JSONObject[\"entities\"] not found")) {
					System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
							+ "] - Tweet Contains No Links..." + ANSI_RESET);
				} else {
					System.out.println(ANSI_GREY + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + tag
							+ "] - Tweet Contains No Links: " + e.toString() + ANSI_RESET);
				}
			}

		}
		return false;

	}

	public String getRandomTwitterToken() {
		List<String> asList = new ArrayList<String>(twitterTokens);
		Collections.shuffle(asList);
		return String.valueOf(asList.get(0));
	}

	public String getInvite() {
		return this.invite;
	}

	public String getTag() {
		return this.tag;
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
