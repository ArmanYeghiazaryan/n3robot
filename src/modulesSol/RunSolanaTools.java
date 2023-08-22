package modulesSol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.sound.sampled.LineUnavailableException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONObject;
import org.json.JSONTokener;

import main.Main;
import modulesEth.EthGenerateWalletTask;
import modulesEth.EthTopupWalletTask;

public class RunSolanaTools extends Main {

	private String mode;

	public RunSolanaTools(String mode) throws Exception {
		this.mode = mode;
		runTasks();
	}

	public void runTasks() throws Exception {

		SolanaTools tools = new SolanaTools(getWebhookUrl());

		if (mode.equals("WALLET_GEN")) {
			System.out.println(ANSI_CYAN + "\n>>> Type Wallet Amount..." + ANSI_RESET);

			Scanner in = new Scanner(System.in);
			String input = in.next();

			try {
				int amount = Integer.valueOf(input);

				for (int i = 0; i < amount; i++) {
					tools.generateSolanaWallet();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(ANSI_CYAN + "Invalid Input!\n\n" + ANSI_RESET);
				Thread.sleep(1000);
				runTasks();
			}
		} else if (mode.equals("WALLET_TOPUP")) {
			Path path = Paths.get(
					System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaWalletTopup.csv");
			Reader in = new FileReader(
					System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaWalletTopup.csv");
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

			System.out.println(ANSI_CYAN + "Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
					+ " task(s)...\n" + ANSI_RESET);
			Thread.sleep(1000);

			for (CSVRecord record : records) {

				// Get fromWallet. Check if it is in file or not.
				String fromWallet = "";
				String[] fromWalletArray = record.get("FROM_WALLET").split(" ");

				if (fromWalletArray.length < 50) {
					fromWallet = getSolanaCredentialFromWalletsFile("PRIVATE_KEY", record.get("FROM_WALLET"));
				} else {
					fromWallet = record.get("FROM_WALLET");
				}

				// Get toWallet. Check if is in file or not.
				String toWallet = "";
				if (record.get("TO_WALLET").length() >= 32) {
					toWallet = record.get("TO_WALLET");
				} else {
					toWallet = getSolanaCredentialFromWalletsFile("ADDRESS", record.get("TO_WALLET"));
				}

				String transactionValue = record.get("TRANSACTION_VALUE");

				tools.transferSol(fromWallet, toWallet, transactionValue);

			}
		} else if (mode.equals("BALANCE_CHECK")) {
			System.out.println(ANSI_CYAN + "\n> 1. Check Single Wallet");
			System.out.println("> 2. Check All Wallets");
			System.out.println("\n>>> Choose..." + ANSI_RESET);

			Scanner in = new Scanner(System.in);
			String input = in.next();

			switch (input) {
			case "1":
				System.out.println(ANSI_CYAN + "\n>>> Type Wallet Address..." + ANSI_RESET);

				Scanner in2 = new Scanner(System.in);
				String input2 = in2.next();
				tools.getSolBalance(input2);
				runTasks();
				break;
			case "2":
				Path path = Paths
						.get(System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\wallets\\solanaWallets.csv");
				Reader inReader = new FileReader(
						System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\wallets\\solanaWallets.csv");
				Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(inReader);
				for (CSVRecord record : records) {
					try {
						tools.getSolBalance(record.get("address"));
					} catch (Exception e) {

					}
				}
				break;
			default:
				System.out.println(ANSI_CYAN + "... Invalid Input!\n\n" + ANSI_RESET);
				Thread.sleep(1100);
				runTasks();
				break;
			}
		} else if (mode.equals("MINTING")) {
			System.out.println(ANSI_CYAN + "\n> 1. Scrape Candy Machines By URL");
			System.out.println("> 2. Use Custom Candy Machine Key");
			System.out.println("\n>>> Choose..." + ANSI_RESET);

			Scanner in = new Scanner(System.in);
			String input = in.next();

			switch (input) {
			case "1":
				// scrape candy machines
				getCandyMachinesFromWebsite();
				break;
			case "2":

				useCustomCandyMachine();

				break;
			default:
				System.out.println(ANSI_CYAN + "... Invalid Input!\n\n" + ANSI_RESET);
				Thread.sleep(1100);
				runTasks();
				break;
			}
		}

		else if (mode.equals("TRANSFER_ALL_NFTS")) {

			Path path = Paths.get(
					System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaNftTransfer.csv");
			Reader inReader = new FileReader(
					System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaNftTransfer.csv");
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(inReader);
			for (CSVRecord record : records) {
				String privateKey = getSolanaCredentialFromWalletsFile("PRIVATE_KEY", record.get("FROM_WALLET"));
				String publicAddress = getSolanaCredentialFromWalletsFile("ADDRESS", record.get("FROM_WALLET"));
				String receiverPublic = getSolanaCredentialFromWalletsFile("ADDRESS", record.get("TO_WALLET"));

				HashSet<String> set = tools.getWalletNFTs(publicAddress);

				for (String nft : set) {
					tools.transferNFT(privateKey, receiverPublic, nft);
				}

			}

		}
	}

	public void getCandyMachinesFromWebsite() throws Exception {
		System.out.println(ANSI_CYAN + "\n>>> Paste Minting URL..." + ANSI_RESET);
		// Use custom CMID
		Scanner inScanner = new Scanner(System.in);
		String url = inScanner.next();

		System.out.println(ANSI_CYAN + "Scraping Data..." + ANSI_RESET);

		// Generate random file name
		Random r = new Random();
		char c = (char) (r.nextInt(26) + 'a');
		String folderName = "";
		for (int i = 0; i < 15; i++) {
			c = (char) (r.nextInt(26) + 'a');
			folderName = folderName + String.valueOf(c);
		}
		String systemName = System.getProperty("user.home");
		String folderPath = systemName + "\\Desktop\\ZETA Bot\\target\\" + folderName;

		// Execute wget
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("wget -m -nd -A \"*main*.js\" -P \"" + folderPath + "\" " + url);

		if (proc.waitFor() != 0) { // get exit code
			System.out.println(ANSI_CYAN + "Scraping failed. Retrying..." + ANSI_RESET);
			Thread.sleep(1000);
		}

		System.out.println(ANSI_CYAN + "Scraped!" + ANSI_RESET);

		// Get CMIDs form file
		File test2 = new File(folderPath);

		if (test2.listFiles() == null) {
			System.out.println(ANSI_CYAN + "No Candy Machines Found..." + ANSI_RESET);
			getCandyMachinesFromWebsite();
		}

		Map<String, String> candyMachineIDs = new HashMap<String, String>();
		for (File file : test2.listFiles()) {
			if (file.isDirectory()) {
			} else {

				if (file.getCanonicalPath().toString().contains("main")
						&& file.getCanonicalPath().toString().contains("js")) {
					String content = Files.readString(Path.of(file.getCanonicalPath()), StandardCharsets.US_ASCII);
					candyMachineIDs = parseCandyMachinesFromJS(content);

				}
			}
		}

		// Check all CMIDs per Request
		Map<String, String> validCandyMachineIDs = new HashMap<String, String>();
		for (String publicKey : candyMachineIDs.keySet()) {

			SolanaMintingTask t = new SolanaMintingTask(null, null, null, 0, validCandyMachineIDs, publicKey, null,
					"validateCandyMachineForURLMint", null);
			t.start();

		}

		while (validCandyMachineIDs.size() != candyMachineIDs.size()) {
			Thread.sleep(250);
		}

		// get amount of valid
		int amountOfValid = 0;
		for (String publicKey : validCandyMachineIDs.keySet()) {
			if (!validCandyMachineIDs.get(publicKey).equals("INVALID")) {

				amountOfValid++;

			}
		}

		if (amountOfValid == 0) {
			System.out.println(ANSI_CYAN + "No Candy Machines Found..." + ANSI_RESET);
			getCandyMachinesFromWebsite();
		}

		int counter = 0;
		String[] decisionArray = new String[amountOfValid];
		for (String publicKey : validCandyMachineIDs.keySet()) {
			if (!validCandyMachineIDs.get(publicKey).equals("INVALID")) {

				decisionArray[counter] = validCandyMachineIDs.get(publicKey);

			}
		}

		System.out.println(ANSI_CYAN + "\n>>> Available Candy Machines:" + ANSI_RESET);
		for (int i = 0; i < decisionArray.length; i++) {
			String json = decisionArray[i];

			JSONTokener tokener = new JSONTokener(json);
			JSONObject object = new JSONObject(tokener);
			// CM DATA
			String config_address = object.getString("config_address").replace("\"", "");
			long go_live_date = (long) object.getFloat("go_live_date");
			float items_available = object.getFloat("items_available");
			float items_redeemed = object.getFloat("items_redeemed");
			float price = object.getFloat("price") / 1000000000;
			String uuid = object.getString("uuid").replace("\"", "");
			String version = object.getString("versionNumber").replace("\"", "");

			// Parse Date
			Date date = new Date(go_live_date * 1000L);
			SimpleDateFormat jdf = new SimpleDateFormat("dd-MM-yyyy @ HH:mm:ss", Locale.getDefault());
			String launchDate = jdf.format(date);

			System.out.println(
					ANSI_CYAN + "> " + (counter + 1) + ". " + price + "SOL | " + (items_available - items_redeemed)
							+ " Available" + " | " + version.toUpperCase() + " | " + launchDate + ANSI_RESET);

		}

		System.out.println(ANSI_CYAN + "\n>>> Choose..." + ANSI_RESET);
		Scanner inScanner2 = new Scanner(System.in);
		String choice = inScanner2.next();

		// Add error handling

		// Get data of choice
		JSONTokener tokener = new JSONTokener(decisionArray[Integer.valueOf(choice) - 1]);
		JSONObject object = new JSONObject(tokener);
		// CM DATA
		String config_address = object.getString("config_address").replace("\"", "");
		long go_live_date = (long) object.getFloat("go_live_date");
		float price = object.getFloat("price") / 1000000000;
		String candyMachineVersion = object.getString("versionNumber").replace("\"", "");

		Path path = Paths
				.get(System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaMinting.csv");
		Reader inReader = new FileReader(
				System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaMinting.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(inReader);

		System.out.println("Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1) + " task(s)...\n");
		Thread.sleep(1000);

		for (CSVRecord record : records) {

			SolanaMintingTask task = new SolanaMintingTask(
					getSolanaCredentialFromWalletsFile("PRIVATE_KEY", record.get("SECRET_KEY")), config_address,
					candyMachineVersion, go_live_date, null, null, getWebhookUrl(), "MINTING", String.valueOf(price));
			task.start();

		}

	}

	public Map<String, String> parseCandyMachinesFromJS(String content) throws IOException, InterruptedException {

		Map<String, String> map = new HashMap<String, String>();

		String[] array = content.split(Pattern.quote("PublicKey(\""));
		for (String temp : array) {
			try {
				String[] temp2 = temp.split(Pattern.quote("\""));
				String target = temp2[0];

				String specialCharactersString = "!@#$%&*()'+,-./:;<=>?[]^_`{|}";
				boolean has = false;
				for (int i = 0; i < target.length(); i++) {
					char ch = target.charAt(i);
					if (specialCharactersString.contains(Character.toString(ch))) {
						has = true;
					}
				}

				if (!has) {
					map.put(target, "");
				}

			} catch (Exception e) {

			}
		}

		for (String key : map.keySet()) {
//			System.out.println(key);
		}

		return map;
	}

	public void useCustomCandyMachine() throws InterruptedException, IOException {
		SolanaTools tools = new SolanaTools(getWebhookUrl());
		System.out.println(ANSI_CYAN + "\n>>> Paste Candy Machine Key..." + ANSI_RESET);
		// Use custom CMID
		Scanner inScanner = new Scanner(System.in);
		String candyMachineKey = inScanner.next();

		// Validate Candy Machine
		try {

			String candyMachineVersion = tools.getCandyMachineVersion(candyMachineKey);
			String metaData = tools.getCandyMachineMetadata(candyMachineKey, candyMachineVersion);
			JSONTokener tokener = new JSONTokener(metaData);
			JSONObject object = new JSONObject(tokener);
			// CM DATA
			String config_address = object.getString("config_address").replace("\"", "");
			long go_live_date = (long) object.getFloat("go_live_date");
			float items_available = object.getFloat("items_available");
			float items_redeemed = object.getFloat("items_redeemed");
			float price = object.getFloat("price") / 1000000000;
			String uuid = object.getString("uuid").replace("\"", "");
			String wallet = object.getString("wallet").replace("\"", "");

			// Parse Date
			Date date = new Date(go_live_date * 1000L);
			SimpleDateFormat jdf = new SimpleDateFormat("dd-MM-yyyy @ HH:mm:ss", Locale.getDefault());
			String launchDate = jdf.format(date);

			// Print for Confirmation

			System.out.println(ANSI_CYAN + "\n> " + price + "SOL | " + (items_available - items_redeemed) + " Available"
					+ " | " + candyMachineVersion.toUpperCase() + " | " + launchDate + ANSI_RESET);

			System.out.println(ANSI_CYAN + "\n> 1. Start Tasks");
			System.out.println("> 2. Use Different Key");
			System.out.println("\n>>> Choose..." + ANSI_RESET);

			Scanner in = new Scanner(System.in);
			String input = in.next();

			switch (input) {
			case "1":
				// Iterate over SOL File
				Path path = Paths.get(
						System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaMinting.csv");
				Reader inReader = new FileReader(
						System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksSolanaMinting.csv");
				Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(inReader);

				System.out.println(ANSI_CYAN + "Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
						+ " task(s)...\n" + ANSI_RESET);
				Thread.sleep(1000);

				for (CSVRecord record : records) {
					SolanaMintingTask task = new SolanaMintingTask(
							getSolanaCredentialFromWalletsFile("PRIVATE_KEY", record.get("SECRET_KEY")), config_address,
							candyMachineVersion, go_live_date, null, null, getWebhookUrl(), "MINTING",
							String.valueOf(price));
					task.start();

				}
				break;
			case "2":
				useCustomCandyMachine();
				break;
			default:
				System.out.println(ANSI_CYAN + "... Invalid Input!\n\n" + ANSI_RESET);
				Thread.sleep(1100);
				useCustomCandyMachine();
				break;
			}

		} catch (Exception e) {
			useCustomCandyMachine();
		}

	}

}
