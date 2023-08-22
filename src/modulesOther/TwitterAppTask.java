package modulesOther;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.utils.URLEncodedUtils;
import org.asynchttpclient.uri.Uri;
import org.bouncycastle.jcajce.provider.symmetric.AES;

import main.CLITools;
import okhttp3.HttpUrl;
import twitter4j.Twitter;

public class TwitterAppTask extends Thread {

	private int amount;
	private String consumer;
	private String consumerSecret;
	private String screenName;
	private String accessToken;
	private String accessTokenSecret;
	private String userToFollow;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	CLITools cliTools = new CLITools(false);
	private String proxy = "random";
	private String ip;
	private int port;
	private String username;
	private String password;
	private boolean isFinished;
	private int attempts = 0;

	public TwitterAppTask(String consumer, String consumerSecret, String screenName, String accessToken,
			String accessTokenSecret, String userToFollow) {
		this.consumer = consumer;
		this.consumerSecret = consumerSecret;
		this.screenName = screenName;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
		this.userToFollow = userToFollow;
	}

	public TwitterAppTask(String consumer, String consumerSecret) {
		this.consumer = consumer;
		this.consumerSecret = consumerSecret;
	}

	public void run() {

		if (this.userToFollow != null) {
			try {
				setProxy();
				follow();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(ANSI_YELLOW + "[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName
						+ "] - Generous Error: " + e.getMessage() + ANSI_RESET);
			}
		}
	}

	public void setAmount() throws Exception {
		try {
			System.out.print(ANSI_YELLOW + "\n>>> Set Amount Of Accounts: " + ANSI_RESET);
			Scanner userInput = new Scanner(System.in);
			String input = userInput.nextLine();
			this.amount = Integer.valueOf(input);
			cliTools.setTitle("N3RO BOT - Twitter Initializer - Status: " + 0 + "/" + amount);

			System.out.println();

		} catch (Exception e) {
			System.out.println(ANSI_RED + "Invalid Input!" + ANSI_RESET);
			Thread.sleep(1100);
			setAmount();
		}
	}

	public void follow() throws Exception {

		try {
			System.out
					.println("[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName + "] - Preparing...");
//			ConfigurationBuilder cb = new ConfigurationBuilder();
//
//			cb.setOAuthConsumerKey(consumer).setOAuthConsumerSecret(consumerSecret);
//
//			TwitterFactory factory = new TwitterFactory(cb.build());
//
//			Twitter twitter = factory.getInstance();
//
//			AccessToken accessTokenObject = new AccessToken(accessToken, accessTokenSecret);
//
//			twitter.setOAuthAccessToken(accessTokenObject);
//
//			System.out.println(ANSI_YELLOW + "[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName
//					+ "] - Following '@" + this.userToFollow + "'..." + ANSI_RESET);
//
//			User user = twitter.createFriendship(this.userToFollow);
//
//			System.out.println(ANSI_GREEN + "[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName + "] "
//					+ "- Followed '@" + user.getScreenName() + "' Successfully!" + ANSI_RESET);
//			this.isFinished = true;

		} catch (Exception e) {
			setProxy();
			System.out.println(ANSI_RED + "[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName
					+ "] - Error Retrying: " + e.getMessage().strip() + ANSI_RESET);
			Thread.sleep(3000);
			attempts++;

			if (attempts < 3) {
				follow();

			} else {
				System.out.println(ANSI_RED + "[TWITTER] - [" + dtf.format(now.now()) + "] - [@" + this.screenName
						+ "] - Task Stopped: MAX_ATTEMPTS_EXCEEDED" + ANSI_RESET);
			}
		}

	}

	public void initializeAccounts() throws Exception {
		setAmount();

		for (int i = 1; i <= amount; i++) {
			try {
				System.out.println("[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i + "] - Preparing...");
//				ConfigurationBuilder cb = new ConfigurationBuilder();
//				cb.setDebugEnabled(true).setOAuthConsumerKey(consumer).setOAuthConsumerSecret(consumerSecret);
//				TwitterFactory tf = new TwitterFactory(cb.build());
//				Twitter twitter = tf.getInstance();
//				RequestToken requestToken = null;
//				requestToken = twitter.getOAuthRequestToken();

				System.out.print(ANSI_GREY + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i
						+ "] - Login via following link and hit enter..." + ANSI_RESET);

//				String authorizationURL = requestToken.getAuthorizationURL();

				System.out.println(
						ANSI_YELLOW + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i + "] - " + "" + ANSI_RESET);

				Scanner userInput = new Scanner(System.in);
				userInput.nextLine();

				System.out.print(ANSI_GREY + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i
						+ "] - Provide pin-code or hit enter..." + ANSI_RESET);

				userInput = new Scanner(System.in);

				String pinCode = userInput.nextLine();

//				AccessToken token = null;

				if (pinCode.length() == 0) {
					System.out.print(ANSI_GREY + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i
							+ "] - Provide redirect-url from browser..." + ANSI_RESET);

					userInput = new Scanner(System.in);

					String link = userInput.nextLine();
					HttpUrl url = HttpUrl.parse(link);
					String cookie = url.queryParameter("oauth_verifier");
//					token = twitter.getOAuthAccessToken(cookie);

//				} else {
//					token = twitter.getOAuthAccessToken(requestToken, pinCode);
//				}
//
//				twitter.setOAuthAccessToken(token);
//				String username = token.getScreenName();
//				String accessToken = token.getToken();
//				String accessTokenSecret = token.getTokenSecret();

					addToCSV(username, accessToken, accessTokenSecret);
					System.out.println(ANSI_GREEN + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i
							+ "] - Initialized Account Successfully!" + ANSI_RESET);
					cliTools.setTitle("N3RO BOT - Twitter Initializer - Status: " + i + "/" + amount);

					// encrypt tokens, add to csv, proxy support

				}
			} catch (Exception e) {
				System.out.println(ANSI_RED + "[TWITTER] - [" + dtf.format(now.now()) + "] - [" + i + "] - Error: "
						+ e.getMessage() + ANSI_RESET);
			}
		}
	}

	public void addToCSV(String username, String accessToken, String accessTokenSecret) throws Exception {
		String path = System.getProperty("user.dir") + "\\target\\twitterTokens.csv";
		File file = new File(path);

		if (!file.exists()) {
			FileWriter writer = new FileWriter(path);
			writer.write("USERNAME,TOKEN,TOKEN_SECRET");
			writer.close();
			addToCSV(username, accessToken, accessTokenSecret);
		} else {
			FileWriter writer = new FileWriter(path, true);
			writer.write("\n" + username + "," + accessToken + "," + accessTokenSecret);
			writer.close();

		}
	}

	public void setProxy() throws Exception {
		String[] p = getProxy().split(":");
		this.ip = p[0];
		this.port = Integer.valueOf(p[1]);
		this.username = p[2];
		this.password = p[3];

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

	public boolean isFinished() {
		return this.isFinished;
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
