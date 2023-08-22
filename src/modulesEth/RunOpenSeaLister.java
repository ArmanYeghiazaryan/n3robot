package modulesEth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONObject;
import org.json.JSONTokener;

import engines.MintingEngine;
import engines.ToolsEngine;
import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.Proxy;
import kong.unirest.UnirestInstance;
import main.Main;

public class RunOpenSeaLister extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	// ask if check 1. Single Wallet or 2. Check whole Wallets file

	public RunOpenSeaLister() throws Exception {
		runTasks();
	}

	public String getCollectionSlug() throws Exception {

		String slug = null;
		try {
			System.out.print(ANSI_YELLOW + "\n>>> Provide Collection-URL: " + ANSI_RESET);

			Scanner in = new Scanner(System.in);
			String input = in.next();
			in.close();

			String arr[] = input.split(Pattern.quote("/"));
			slug = arr[arr.length - 1];

			if (slug.contains("?")) {
				arr = slug.split(Pattern.quote("?"));
				slug = arr[0];
			}

		} catch (Exception e) {
			System.out.println(ANSI_RED + "Invalid Collection-URL!" + ANSI_RESET);

			Thread.sleep(2000);
			getCollectionSlug();
		}

		return slug;

	}

	public String getCollectionData(String slug) throws Exception {
		String data = "";

		try {

			Config config = new Config().connectTimeout(30_000);

			UnirestInstance unirest = new UnirestInstance(config);

			HttpResponse<String> response = unirest.get("https://api.opensea.io/api/v1/collection/" + slug + "/")
					.header("accept", "application/json").asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200 || !responseBody.contains("schema_name")) {

				throw new Exception("GET_FLOOR_FAILED_" + response.getStatus());

			}

			data = responseBody;

		} catch (Exception e) {

			System.out.println(ANSI_RED + "Get Collection Failed! Retrying..." + ANSI_RESET);

			Thread.sleep(1000);
			getCollectionData(slug);

		}

		return data;
	}

	public void selectWallets(String contractAddress) throws Exception {
		System.out.println(ANSI_WHITE + "\n\n> 1. Select Wallets Manually");
		System.out.println("> 2. Scan Wallets From etherWallets.csv");
		System.out.println("> 3. Scan Wallets From tasksEthMinting.csv" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);
		Scanner in = new Scanner(System.in);
		String input = in.next();
//		in.close();
		switch (input) {
		case "1":
			selectWalletsManually(contractAddress);
			break;
		case "2":
			selectWalletsFromWalletsFile(contractAddress);
			break;

		case "3":
			selectWalletsFromMintingFile(contractAddress);
			break;
		default:
			System.out.println(ANSI_RED + "Invalid Input!" + ANSI_RESET);
			Thread.sleep(1100);
			selectWallets(contractAddress);
			break;
		}
	}

	public void selectWalletsManually(String contractAddress) throws Exception {
		Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		int counter = 0;
		System.out.println("");
		for (CSVRecord record : records) {
			counter++;

			System.out.println(ANSI_WHITE + "> " + counter + ". " + getWalletName(record) + ANSI_RESET);
		}

		in.close();

		System.out.print(ANSI_YELLOW + "\n>>> Select One Or More Wallets (Ex. '1' or '1,2,3'): " + ANSI_RESET);
		Scanner inS = new Scanner(System.in);
		String input = inS.next();
		in.close();
		HashSet<String> set = new HashSet<String>();

		for (String entry : input.split(Pattern.quote(","))) {

			try {

				set.add(getPrivateKeyFromWallets(Integer.valueOf(entry)));

			} catch (Exception e) {
				System.out.println(ANSI_RED + "Invalid Number '" + entry + "'! Skipping..." + ANSI_RESET);
			}

		}

		launchTasks(set, contractAddress);
	}

	public void selectWalletsFromWalletsFile(String contractAddress) throws Exception {
		Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		HashSet<String> set = new HashSet<String>();

		for (CSVRecord record : records) {

			if (record.get("PRIVATE_KEY").length() > 60) {
				set.add(record.get("PRIVATE_KEY"));

			} else {
				System.out.println(
						ANSI_RED + "Invalid Wallet '" + record.get("PRIVATE_KEY") + "'! Skipping..." + ANSI_RESET);

			}
		}

		in.close();
		launchTasks(set, contractAddress);

	}

	public void selectWalletsFromMintingFile(String contractAddress) throws Exception {
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		HashSet<String> set = new HashSet<String>();

		for (CSVRecord record : records) {

			set.add(new Main().getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY")));
		}

		in.close();
		launchTasks(set, contractAddress);

	}

	public String getPrivateKeyFromWallets(int line) throws Exception {
		Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		int counter = 0;
		for (CSVRecord record : records) {
			counter++;
			if (counter == line) {

				return record.get("PRIVATE_KEY");
			}
		}

		throw new Exception("INVALID_LINE");
	}

	public String getAssetsPrice() throws Exception {
		System.out.print(ANSI_YELLOW + "\n>>> Provide Listing Price (Ex.: '0.5' or '0.5-1.2'): " + ANSI_RESET);

		try {
			Scanner in = new Scanner(System.in);
			String input = in.next().strip();
			in.close();
			BigDecimal minPrice;
			BigDecimal maxPrice;

			if (input.contains("-")) {
				minPrice = new BigDecimal(input.split(Pattern.quote("-"))[0]);
				maxPrice = new BigDecimal(input.split(Pattern.quote("-"))[1]);

				if (minPrice.doubleValue() > maxPrice.doubleValue()) {
					return maxPrice.toString() + "-" + minPrice.toString();
				}

			} else {
				minPrice = new BigDecimal(input);
				return minPrice.toString() + "-" + minPrice.toString();

			}

			return input;
		} catch (Exception e) {
			throw new Exception("Invalid NFT Price!");

		}
	}

	public void launchTasks(HashSet<String> set, String contractAddress) throws Exception {
		String price = getAssetsPrice();

		System.out.println(ANSI_YELLOW + "\nInitializing " + set.size() + " task(s)...\n" + ANSI_RESET);

		int counter = 0;
		String alchemyKeyUrl = new Main().getAlchemyKeyUrl();
		for (String key : set) {
			counter++;
			EthListItemTask task = new EthListItemTask(counter, key, contractAddress, price, alchemyKeyUrl);
			task.start();
		}

//		try {
//			String price = getAssetsPrice();
//			double listingPrice;
//			double maxPrice = new BigDecimal(price.split(Pattern.quote("-"))[1]).doubleValue();
//			double minPrice = new BigDecimal(price.split(Pattern.quote("-"))[0]).doubleValue();
//
//			if (maxPrice != minPrice) {
//				listingPrice = ThreadLocalRandom.current().nextDouble(minPrice, maxPrice);
//				listingPrice = new BigDecimal(listingPrice).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
//			} else {
//				listingPrice = maxPrice;
//			}
//
//		} catch (Exception e) {
//			System.out.println(ANSI_RED + e.getMessage() + ANSI_RESET);
//			Thread.sleep(1000);
//			launchTasks(set, contractAddress);
//		}

	}

	public String getWalletName(CSVRecord record) {

		if (!record.get("NAME").equals("")) {
			return record.get("NAME");
		} else if (!record.get("ADDRESS").equals("")) {
			return record.get("ADDRESS");
		} else if (!record.get("PRIVATE_KEY").equals("")) {
			return record.get("PRIVATE_KEY");
		} else {
			return "INVALID ENTRY";
		}

	}

	public void runTasks() throws Exception {

		// GET COLLECTION DATA
		String slug = getCollectionSlug();

		JSONObject o = new JSONObject(new JSONTokener(getCollectionData(slug)));

		String collectionName = o.getJSONObject("collection").getString("name");
		String collectionSize = String
				.valueOf(o.getJSONObject("collection").getJSONObject("stats").get("total_supply"));
		BigDecimal collectionFloor = o.getJSONObject("collection").getJSONObject("stats").getBigDecimal("floor_price");
		BigInteger collectionFees = new BigInteger(
				o.getJSONObject("collection").getString("dev_seller_fee_basis_points")).add(
						new BigInteger(o.getJSONObject("collection").getString("opensea_seller_fee_basis_points")));
		BigDecimal collectionFeesInPercent = new BigDecimal(collectionFees).divide(BigDecimal.valueOf(100));
		String contractAddress = new JSONObject(new JSONTokener(
				o.getJSONObject("collection").getJSONArray("primary_asset_contracts").get(0).toString()))
						.getString("address");

		System.out
				.print(ANSI_GREEN + "\n>>> Collection: " + collectionName + " | Items: " + collectionSize + " | Fees: "
						+ collectionFeesInPercent.toString() + "% | Floor: " + collectionFloor + "E" + ANSI_RESET);

		// SELECT WALLETS

		selectWallets(contractAddress);

		EthTopupWalletTask walletChecker = new EthTopupWalletTask(null, null, "0.0", getAlchemyKeyUrl(), null, null);

		switch (slug) {
		case "1":
			System.out.print(ANSI_YELLOW + "\n>>> Set Wallet Address: " + ANSI_RESET);
			Scanner in2 = new Scanner(System.in);
			String input2 = in2.next();
			in2.close();
			walletChecker.getWalletBalance(input2, "");
			runTasks();
			break;
		case "2":
			Path path = Paths.get(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
			Reader inReader = new FileReader(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv");
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(inReader);
			for (CSVRecord record : records) {
				walletChecker.getWalletBalance(record.get("ADDRESS"), record.get("NAME"));
			}
			break;
		default:
			System.out.println(ANSI_RED + "Invalid Input!\n" + ANSI_RESET);
			Thread.sleep(1100);
			runTasks();
			break;
		}

	}
}
