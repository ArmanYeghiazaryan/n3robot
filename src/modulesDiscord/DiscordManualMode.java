package modulesDiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import org.web3j.crypto.Credentials;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;

import kong.unirest.Config;
import kong.unirest.UnirestInstance;

public class DiscordManualMode extends Thread {

	private String discordToken;
	private String username;
	private String password;
	private String proxy;
	private String ip;
	private int port;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private Playwright playwright;
	private Page page;
	private boolean isFinished;

	public DiscordManualMode(String discordToken, String proxy) {
		this.discordToken = discordToken;
		this.proxy = proxy;
	}

	public void run() {

		try {
			setProxy();
			prepareTask();
			getLogin();
			executeLogin();
		} catch (Exception e) {

		}

	}

	public void prepareTask() {
		System.out.println(
				"[DISCORD] - [" + dtf.format(now.now()) + "] - [" + this.discordToken + "] - Preparing Browser...");

		this.playwright = Playwright.create();
		Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false).setProxy(
				new Proxy("http://" + ip + ":" + port + "").setUsername(this.username).setPassword(this.password))
				.setTimeout(60_000));
		BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36"));
		this.page = context.newPage();
	}

	public void getLogin() throws Exception {

		int status = page.navigate("https://discord.com/login").status();

		String response = page.content();

		if (status != 200) {

			setProxy();

			System.out.println(ANSI_RED + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + this.discordToken
					+ "] - GET LOGIN FAILED - " + status + " - Retrying..." + ANSI_RESET);
			Thread.sleep(1000);
			getLogin();

		}
	}

	public void executeLogin() throws Exception {
		System.out.println(ANSI_YELLOW + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + this.discordToken
				+ "] - Logging In..." + ANSI_RESET);

		page.evaluate("let token = \"" + this.discordToken + "\";\n" + "\n" + "function login(token) {\n"
				+ "    setInterval(() => {\n"
				+ "      document.body.appendChild(document.createElement `iframe`).contentWindow.localStorage.token = `\"${token}\"`\n"
				+ "    }, 50);\n" + "    setTimeout(() => {\n" + "      location.reload();\n" + "    }, 2500);\n"
				+ "  }\n" + "\n" + "login(token);");

		System.out.println(ANSI_GREEN + "[DISCORD] - [" + dtf.format(now.now()) + "] - [" + this.discordToken
				+ "] - Executed Login Fetch!" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Type 'r' to retry or hit enter to launch next task: " + ANSI_RESET);

		Scanner inScanner = new Scanner(System.in);
		String input = inScanner.nextLine();

		if (input.toLowerCase().equals("r")) {
			executeLogin();
		} else {
			this.isFinished = true;
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
