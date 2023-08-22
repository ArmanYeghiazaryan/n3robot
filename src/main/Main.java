package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.transaction.type.LegacyTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import com.google.common.hash.Hashing;

import engines.KeyEngine;
import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.Proxy;
import kong.unirest.UnirestInstance;
import modulesEth.EthTools;

import org.json.*;

import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import net.dongliu.requests.utils.Cookies;

public class Main {

	// new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();

	static String botVersion = "0.2.6.4.1";
	static boolean downloadInstaller = false;
	private String botVersionWebhook = "";
	private static CLITools cliTools = new CLITools(false);

	private static String licenseKey;
	static String discordUsername;
	public static String alchemyKeyUrl;
	public static String etherscanApiKey;
	public static String webhookUrl;
	public static int delayInMs;
	public static HashSet<String> twitterTokens = new HashSet<String>();
	public static HashSet<String> twoCaptchaKeys = new HashSet<String>();
	private static Worker worker;

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREY = "\u001b[30;1m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static final String ANSI_BRIGHT_CYAN = "\u001b[36;1m";

	public static void main(String[] args) throws Exception {

		worker = new Worker();
		worker.start();
		MainMenu newSession = new MainMenu(true, worker);
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public String getAlchemyKeyUrl() {
		return alchemyKeyUrl;
	}

	public String getWebhookUrl() {
		return webhookUrl;
	}

	public String getEtherscanApiKey() {
		return etherscanApiKey;
	}

	public int getDelayInMs() {
		return delayInMs;
	}

	public HashSet<String> getTwitterTokens() {
		return this.twitterTokens;
	}

	public HashSet<String> get2CaptchaKeys() {
		return this.twoCaptchaKeys;

	}

	public void checkBotVersion() {
		Session session = Requests.session();
		RawResponse newSession = session.post(botVersionWebhook).socksTimeout(60_000).connectTimeout(60_000).body("")
				.send();

		if (newSession.statusCode() != 400) {
			System.exit(0);
		}

	}

	public void initializeConfig() throws Exception {

		checkBotVersion();
		this.licenseKey = getValueFromConfig("License-Key");
		validateKey();

		if (Files.readString(Path.of(System.getProperty("user.dir") + "\\config.json"), StandardCharsets.UTF_8)
				.contains("2Captcha-Keys")) {
			String content = Files.readString(Path.of(System.getProperty("user.dir") + "\\config.json"),
					StandardCharsets.UTF_8);
			FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\config.json");
			writer.write(content.replace("2Captcha-Keys", "CapMonster-Keys"));
			writer.close();
		}

		this.alchemyKeyUrl = getValueFromConfig("ETH-Node URL");

		if (this.alchemyKeyUrl == null || this.alchemyKeyUrl.equals("")) {
			this.alchemyKeyUrl = "http://localhost:8400";

		}

		this.etherscanApiKey = getValueFromConfig("Etherscan API-Key");
		this.webhookUrl = getValueFromConfig("Webhook-URL");
		this.delayInMs = Double.valueOf(Double.valueOf(getValueFromConfig("Delay In Seconds")) * 1000).intValue();

		setTwitterTokens();
		setTwoCaptchaKeys();
		checkIfFilesExist();

	}

	public void disableMainHeader() throws Exception {
		worker.disableMainHeader();
		Thread.sleep(1000);
	}

	public void checkIfFilesExist() throws Exception {

		// TARGET FOLDER
		// --------------------
		File f = new File(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json");

		if (!f.exists()) {

			createNewPendingTransactionsFile();

		}

		// --------------------
		f = new File(System.getProperty("user.dir") + "\\target\\twitterTokens.csv");

		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\target\\twitterTokens.csv");
			x.write("USERNAME,TOKEN,TOKEN_SECRET\n");
			x.close();
		}

		// --------------------
		if (downloadInstaller) {
			f = new File(System.getProperty("user.dir") + "\\target\\preparer.exe");
			if (!f.exists()) {
				try {
					String path = System.getProperty("user.dir") + "\\target\\" + "preparer.exe";

					System.out.println(ANSI_YELLOW + "\n Downloading Installer. Please wait..." + ANSI_RESET);

					URL ipURL = new URL(
							"https://onedrive.live.com/download?cid=05A7FDEF5B0CC5EB&resid=5A7FDEF5B0CC5EB%21112&authkey=ADQ2fC-diIcMeYk");
					File opFile = new File(path);
					FileUtils.copyURLToFile(ipURL, opFile);
					System.out.println(
							ANSI_YELLOW + " Download finished." + ANSI_RESET + ANSI_GREEN + " Thanks!" + ANSI_RESET);
					Thread.sleep(2500);
					new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
				} catch (Exception e) {
					System.out.println(ANSI_RED + "Retrying - Download Failed: " + e.getMessage() + ANSI_RESET);
					Thread.sleep(2000);
					new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
					checkIfFilesExist();
				}

			}
		}

		// Node

		f = new File(System.getProperty("user.dir") + "\\target\\node.exe");
		if (!f.exists()) {
			try {
				String path = System.getProperty("user.dir") + "\\target\\" + "node.exe";

				System.out.println(ANSI_YELLOW + "\n Downloading Node. Please wait..." + ANSI_RESET);

				URL ipURL = new URL(
						"https://onedrive.live.com/download?cid=05A7FDEF5B0CC5EB&resid=5A7FDEF5B0CC5EB%21126&authkey=AKV6ACGbTBGggi8");
				File opFile = new File(path);
				FileUtils.copyURLToFile(ipURL, opFile);
				System.out.println(
						ANSI_YELLOW + " Download finished." + ANSI_RESET + ANSI_GREEN + " Thanks!" + ANSI_RESET);
				Thread.sleep(2500);
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} catch (Exception e) {
				System.out.println(ANSI_RED + "Retrying - Download Failed: " + e.getMessage() + ANSI_RESET);
				Thread.sleep(2000);
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
				checkIfFilesExist();
			}

		}

		// WALLETS FOLDER
		f = new File(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
			x.write("NAME,ADDRESS,PRIVATE_KEY\n");
			x.close();
		}

		// TASKS FOLDER

		f = new File(System.getProperty("user.dir") + "\\tasks\\proxies.txt");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\proxies.txt");
			x.write("IP:PORT:USER:PASSWORD");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\eth\\proxiesOpenSea.txt");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\eth\\proxiesOpenSea.txt");
			x.write("IP:PORT:USER:PASSWORD");
			x.close();
		}

		// TASKS OTHER FOLDER
		f = new File(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintChecker.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintChecker.csv");
			x.write("RAFFLE_URL,PRIVATE_KEY,PROXY [RANDOM / IP:PORT]\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntries.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntries.csv");
			x.write("RAFFLE_URL,CUSTOM_FIELD,PRIVATE_KEY,PROXY [RANDOM / IP:PORT]\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntriesResults.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntriesResults.csv");
			x.write("TIMESTAMP,RAFFLE_URL,ADDRESS,PRIVATE_KEY,STATUS\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintInitializer.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintInitializer.csv");
			x.write("PRIVATE_KEY,PROXY [RANDOM / IP:PORT]\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintTrain.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintTrain.csv");
			x.write("RAFFLE_URL,CUSTOM_FIELD,TRANSACTION_VALUE [X / ALL],GAS_PRICE [RAPID / RAPID + X / X],PRIVATE_KEY,PROXY [RANDOM / IP:PORT]\n");
			x.close();
		}

		// ETH FOLDER
		f = new File(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
			x.write("SAFE_MODE [TRUE / FALSE],CONTRACT_ADDRESS,FUNCTION,FUNCTION_PARAMETERS,TRANSACTION_VALUE,GAS_PRICE [RAPID / RAPID + X / X],GAS_LIMIT [AUTO / CUSTOM],PRIVATE_KEY,TIMER [dd-MM-yyyy HH:mm:ss],TRANSACTION_DATA\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthWalletTopup.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthWalletTopup.csv");
			x.write("FROM_WALLET,TO_WALLET,TRANSACTION_VALUE [ALL / X ETH]\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\eth\\tasksOpenSeaSniping.csv");
		if (!f.exists()) {
			FileWriter x = new FileWriter(System.getProperty("user.dir") + "\\tasks\\eth\\tasksOpenSeaSniping.csv");
			x.write("SAFE_MODE [TRUE / FALSE],COLLECTION_NAME,MAX_PRICE [FLOOR / X ETH / X%],TRAITS [TRAIT1=EXP;...],MAX_AMOUNT,GAS_PRICE [RAPID / RAPID + X / X],GAS_LIMIT,PRIVATE_KEY,PROXY [TRUE / FALSE]\n");
			x.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\eth\\customSignatures.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\tasks\\eth\\customSignatures.csv");
			writer.write("PROJECT,WALLET,SIGNATURE\n" + ",,");
			writer.close();
		}

		// --------------------
		f = new File(System.getProperty("user.dir") + "\\N3RORaffles.exe");
		if (!f.exists()) {
			try {
				String path = System.getProperty("user.dir") + "\\N3RORaffles.exe";

				System.out.println(ANSI_YELLOW + "\n Downloading Raffle Engine. Please wait..." + ANSI_RESET);

				URL ipURL = new URL(
						"https://onedrive.live.com/download?cid=05A7FDEF5B0CC5EB&resid=5A7FDEF5B0CC5EB%21114&authkey=ABRPj9Qia9YrFTE");
				File opFile = new File(path);
				FileUtils.copyURLToFile(ipURL, opFile);
				System.out.println(
						ANSI_YELLOW + " Download finished." + ANSI_RESET + ANSI_GREEN + " Thanks!" + ANSI_RESET);
				Thread.sleep(2500);
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} catch (Exception e) {
				System.out.println(ANSI_RED + "Retrying - Download Failed: " + e.getMessage() + ANSI_RESET);
				Thread.sleep(2000);
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
				checkIfFilesExist();
			}

		}

		// DISCORD FOLDER
		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\tasksManualMode.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\discord\\tasksManualMode.csv");
			writer.write("DISCORD_TOKEN,PROXY [RANDOM / IP:PORT]\n,,");
			writer.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\accounts.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\tasks\\discord\\accounts.csv");
			writer.write("NAME,DISCORD_TOKEN\n");
			writer.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\tasksAccountChecker.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\discord\\tasksAccountChecker.csv");
			writer.write("CHECK_SERVER_ROLES [TRUE / FALSE],PROXY [RANDOM / IP:PORT],DISCORD_TOKEN\n");
			writer.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\tasksReactionAdder.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\discord\\tasksReactionAdder.csv");
			writer.write(
					"CHANNEL_ID,MESSAGE_ID,REACTION [EMOJI_NAME / EMOTE_NAME],WAIT_FOR_NEW_ROLE [TRUE / FALSE],PROXY [RANDOM / IP:PORT],DISCORD_TOKEN\n");
			writer.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerJoiner.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerJoiner.csv");
			writer.write(
					"SERVER_INVITE [INVITE / WAIT / @TWITTER],PROXY [RANDOM / IP:PORT],PRE_HARVEST_CAPTCHA [TRUE / FALSE],DISCORD_TOKEN\n");
			writer.close();
		}

		f = new File(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerLeaver.csv");
		if (!f.exists()) {

			FileWriter writer = new FileWriter(
					System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerLeaver.csv");
			writer.write("SERVER_ID [ALL / SERVERID],PROXY [RANDOM / IP:PORT],DISCORD_TOKEN\n");
			writer.close();
		}

		// --------------------

	}

	public void createNewPendingTransactionsFile() throws Exception {
		FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json");
		writer.write("[]");
		writer.close();
	}

	public void validateKey() throws Exception {
		String hwid = getMacAddress();
		String hardwareName = InetAddress.getLocalHost().getHostName();

		String body = "{\"metadata\":{\n" + "    \"hwid\":\"" + hwid + "\",\n" + "    \"hardwareName\":\""
				+ hardwareName + "\"\n" + "  }}";

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("Content-Type", "application/json");
		request.put("Authorization", new KeyEngine().getKey());

		RawResponse newSession = session.post("https://api.whop.com/api/v1/licenses/" + this.licenseKey + "/validate")
				.headers(request).body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		try {
			JSONObject o = new JSONObject(new JSONTokener(response));

			if (response.contains("Please reset your key to use on a new machine")) {
				throw new Exception("KEY_RESET_REQUIRED");

			}

			if (!o.getBoolean("valid")) {
				throw new Exception("LICENSE_INVALID");
			}

			if (!o.getString("key_status").equals("approved")) {
				throw new Exception("LICENSE_NOT_APPROVED");
			}

			if (!o.getString("subscription_status").equals("completed")
					&& !o.getString("subscription_status").equals("active")) {
				throw new Exception("SUBSCRIPTION_STATUS_NOT_COMPLETED");
			}

			this.discordUsername = o.getJSONObject("discord").getString("username");

			BackendWebhook b = new BackendWebhook("Key Validated",
					"**User:** " + this.discordUsername + "\\n**Machine:** " + hardwareName + "\\n**Key: **"
							+ this.licenseKey,
					null,
					"https://discord.com/api/webhooks/951520188732411914/g8MpBzoyW61YTRPxV7wstVzM8DULSldqpRplobn3g6QjavfCxFe5HynoacNaUxo6uHEP");

		} catch (Exception e) {
			String error = "";

			if (e.toString().contains("KEY_RESET_REQUIRED")) {
				error = "KEY_RESET_REQUIRED";
			} else {
				error = "ACCESS DENIED";
			}
			System.out.println(ANSI_RED + error + ANSI_RESET);
			Thread.sleep(2000);
			System.exit(0);
		}

	}

	public String getMacAddress() throws Exception {
		InetAddress localHost = InetAddress.getLocalHost();
		NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
		byte[] hardwareAddress = ni.getHardwareAddress();

		String[] hexadecimal = new String[hardwareAddress.length];
		for (int i = 0; i < hardwareAddress.length; i++) {
			hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
		}
		String macAddress = String.join("-", hexadecimal);

		return macAddress;
	}

	public String getValueFromConfig(String key) throws IOException, InterruptedException {
		String value = "";
		try {

			Path path = Paths.get(System.getProperty("user.dir") + "\\config.json");

			String content = Files.readString(path, StandardCharsets.US_ASCII);

			JSONTokener tokener = new JSONTokener(content);
			JSONObject object = new JSONObject(tokener);
			value = object.getString(key).replace("\"", "");

			if (value.equals("") && !key.equals("ETH-Node URL"))
				throw new Exception("VALUE_IS_EMPTY");
		} catch (Exception e) {
			System.out.println("COULD NOT GET '" + key + "' FROM 'config.json' - PLEASE CHECK...");
			Thread.sleep(5000);
			getValueFromConfig(key);
		}
		return value;

	}

	public void setTwitterTokens() throws Exception {

		try {

			Path path = Paths.get(System.getProperty("user.dir") + "\\config.json");

			String content = Files.readString(path, StandardCharsets.US_ASCII);

			JSONTokener tokener = new JSONTokener(content);
			JSONObject object = new JSONObject(tokener);
			JSONArray a = object.getJSONArray("Twitter-Bearer-Tokens");

			for (int i = 0; i < a.length(); i++) {
				twitterTokens.add(a.getString(i));
			}

		} catch (Exception e) {
			System.out.println("COULD NOT GET 'Twitter-Bearer-Tokens' FROM 'config.json' - PLEASE CHECK...");
			Thread.sleep(5000);
			setTwitterTokens();
		}
	}

	public void setTwoCaptchaKeys() throws Exception {

		try {

			Path path = Paths.get(System.getProperty("user.dir") + "\\config.json");

			String content = Files.readString(path, StandardCharsets.US_ASCII);

			JSONTokener tokener = new JSONTokener(content);
			JSONObject object = new JSONObject(tokener);
			JSONArray a = object.getJSONArray("CapMonster-Keys");

			for (int i = 0; i < a.length(); i++) {
				this.twoCaptchaKeys.add(a.getString(i));
			}

		} catch (Exception e) {
			System.out.println("COULD NOT GET 'CapMonster-Keys' FROM 'config.json' - PLEASE CHECK...");
			Thread.sleep(5000);
			setTwitterTokens();
		}
	}

	public String getEtherCredentialFromWalletsFile(String neededCredential, String providedCredential)
			throws IOException, InterruptedException {
		Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		String value = "";

		for (CSVRecord record : records) {
			String name = record.get("NAME");
			String address = record.get("ADDRESS");
			String privateKey = record.get("PRIVATE_KEY");

			if (name.equals(providedCredential) || address.equals(providedCredential)
					|| privateKey.equals(providedCredential)) {

				if (neededCredential.toLowerCase().equals("name") && name.equals("")) {
					return providedCredential;
				}

				value = record.get(neededCredential);
				break;
			}
		}

		if (value.equals("")) {
			System.out.println("'" + providedCredential + "' Missing In 'wallets/etherWallets.csv'. Please Add It...");
			Thread.sleep(2500);
			getEtherCredentialFromWalletsFile(neededCredential, providedCredential);
		}

		return value;
	}

	public String getSolanaCredentialFromWalletsFile(String neededCredential, String providedCredential)
			throws IOException, InterruptedException {
		Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\solanaWallets.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\wallets\\solanaWallets.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		String value = "";

		for (CSVRecord record : records) {
			String name = record.get("NAME");
			String address = record.get("ADDRESS");
			String privateKey = record.get("PRIVATE_KEY");

			if (name.equals(providedCredential) || address.equals(providedCredential)
					|| privateKey.equals(providedCredential)) {
				value = record.get(neededCredential);
				break;
			}
		}

		if (value.equals("")) {
			System.out.println("'" + providedCredential + "' Missing In 'wallets/solanaWallets.csv'. Please Add It...");
			Thread.sleep(2500);
			getSolanaCredentialFromWalletsFile(neededCredential, providedCredential);
		}

		return value;
	}

	public String getDiscordTokenFromProfilesFile(String providedCredential) throws Exception {
		if (providedCredential.length() > 35) {
			return providedCredential;
		} else {

			Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\accounts.csv");
			Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\accounts.csv");
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

			for (CSVRecord record : records) {
				String name = record.get("NAME");
				String discordToken = record.get("DISCORD_TOKEN");

				if (name.equals(providedCredential)) {
					return discordToken;
				}
			}

			System.out.println("'" + providedCredential + "' Missing In 'discord/accounts.csv'. Please Add It...");
			Thread.sleep(2500);
			getDiscordTokenFromProfilesFile(providedCredential);
		}

		return "";
	}

	public String getBotVersion() throws Exception {

		return this.botVersion;
	}

	public String getDiscordUsername() {
		return this.discordUsername;
	}

	public void printLogo() throws Exception {

		System.out.println(ANSI_GREY + "\n    _   _______ ____  ____     ____  ____  ______\n"
				+ "   / | / /__  // __ \\/ __ \\   / __ )/ __ \\/_  __/\n"
				+ "  /  |/ / /_ </ /_/ / / / /  / __  / / / / / /   \n"
				+ " / /|  /___/ / _, _/ /_/ /  / /_/ / /_/ / / /    \n"
				+ "/_/ |_//____/_/ |_|\\____/  /_____/\\____/ /_/     \n"
				+ "                                                 " + ANSI_RESET);
		System.out.println(ANSI_GREEN + "Version " + getBotVersion() + ANSI_RESET + ANSI_GREY + " - " + ANSI_RESET
				+ ANSI_YELLOW + getDiscordUsername() + ANSI_RESET);
	}

	public void removeEntryFromPendingTransactions(String transactionHash) throws Exception {
		String content = Files.readString(
				Path.of(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json"),
				Charset.defaultCharset());
		JSONArray array = new JSONArray(new JSONTokener(content));

		for (int i = 0; i < array.length(); i++) {
			JSONObject o = new JSONObject(new JSONTokener(array.get(i).toString()));
			if (o.getString("transactionHash").equals(transactionHash)) {
				array.remove(i);
				break;
			}
		}

		FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json");
		writer.write(array.toString());
		writer.close();
	}

	public String getLatestBotVersion() throws Exception {

		Session session = Requests.session();

		RawResponse newSession = session.get(
				"https://onedrive.live.com/download?cid=05A7FDEF5B0CC5EB&resid=5A7FDEF5B0CC5EB%21105&authkey=AENuIJsU2UHxCu0")
				.socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "GET_LATEST_VERSION_FAILED" + ANSI_RESET);
			Thread.sleep(2000);
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			getLatestBotVersion();
		}

		return response.strip();
	}

	public void downloadLatestVersion() throws Exception {

		System.out.println(ANSI_CYAN + "\n Update detected!" + ANSI_RESET);

		Thread.sleep(2000);

		Process proc = Runtime.getRuntime().exec("cmd /c start preparer.exe", null,
				new File(System.getProperty("user.dir") + "\\target"));

		System.exit(0);

	}
}
