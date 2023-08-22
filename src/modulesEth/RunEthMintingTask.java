package modulesEth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

import com.esaulpaugh.headlong.abi.Function;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import main.CLITools;
import main.Main;
import modulesOther.PremintTask;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class RunEthMintingTask extends Main {
	List<EthMintingTask> tasksList = new ArrayList<EthMintingTask>();
	CLITools cliTools = new CLITools(false);

	public RunEthMintingTask() throws Exception {
	}

	public void runTasksFromFile() throws Exception {

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		disableMainHeader();
		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		cliTools.setTitle("N3RO BOT - " + new EthTools().getRapidGas() + " GWEI - Pending: " + 0 + " Failed: " + 0
				+ " Success: " + 0);

		int counter = 0;
		String contractAddress = "";
		for (CSVRecord record : records) {
			counter++;
			String secretKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));

			String useSafeMode = record.get("SAFE_MODE [TRUE / FALSE]");
			contractAddress = record.get("CONTRACT_ADDRESS");
			String function = record.get("FUNCTION");
			String functionParameters = record.get("FUNCTION_PARAMETERS");

			String transactionData = record.get("TRANSACTION_DATA");
			String transactionValue = record.get("TRANSACTION_VALUE");

			String amountGasAddedToRapid = record.get("GAS_PRICE [RAPID / RAPID + X / X]");
			String gasLimit = record.get("GAS_LIMIT [AUTO / CUSTOM]");
			String timerString = record.get("TIMER [dd-MM-yyyy HH:mm:ss]");
			String alchemyKeyUrl = getAlchemyKeyUrl();
			String etherscanApiKey = getEtherscanApiKey();
			String webhookUrl = getWebhookUrl();

			EthMintingTask task = new EthMintingTask(counter, useSafeMode, secretKey, contractAddress, function,
					functionParameters, transactionData, transactionValue, amountGasAddedToRapid, gasLimit, timerString,
					null, alchemyKeyUrl, etherscanApiKey, webhookUrl, twoCaptchaKeys);
			task.start();
			tasksList.add(task);
		}

		in.close();

		while (true) {
			monitorStatus(contractAddress);
			Thread.sleep(1000);
		}

	}

	public void runTasksFromInput() throws Exception {

		System.out.print(ANSI_YELLOW + "\n>>> Use SAFE_MODE? (y/empty): " + ANSI_RESET);

		Scanner inScanner = new Scanner(System.in);
		String input = inScanner.nextLine().strip();
		String useSafeMode = "FALSE";

		if (input.toLowerCase().equals("y")) {
			useSafeMode = "TRUE";
		}

		System.out.print(ANSI_YELLOW + "\n>>> Set CONTRACT_ADDRESS: " + ANSI_RESET);
		String contractAddress = inScanner.nextLine().strip();

		String function = "";
		System.out.print(ANSI_YELLOW + "\n>>> Set FUNCTION (name/empty): " + ANSI_RESET);
		function = inScanner.nextLine().strip();

		String functionParameters = "";
		if (function.length() != 0) {
			System.out.print(ANSI_YELLOW + "\n>>> Set FUNCTION_PARAMETERS: " + ANSI_RESET);
			functionParameters = inScanner.nextLine().strip();

		}

		String transactionData = "";
		if (function == null || function.strip().equals("")) {
			System.out.print(ANSI_YELLOW + "\n>>> Set TRANSACTION_DATA: " + ANSI_RESET);
			transactionData = inScanner.nextLine().strip();

		}

		System.out.print(ANSI_YELLOW + "\n>>> Set TRANSACTION_VALUE: " + ANSI_RESET);
		String transactionValue = inScanner.nextLine().strip();

		System.out.print(ANSI_YELLOW + "\n>>> Set GAS_PRICE (Rapid/Rapid+X/X): " + ANSI_RESET);
		String amountGasAddedToRapid = inScanner.nextLine().strip();

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		disableMainHeader();
		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		cliTools.setTitle("N3RO BOT - " + new EthTools().getRapidGas() + " GWEI - Pending: " + 0 + " Failed: " + 0
				+ " Success: " + 0);

		int counter = 0;
		for (CSVRecord record : records) {
			counter++;
			String secretKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));

			String gasLimit = record.get("GAS_LIMIT [AUTO / CUSTOM]");
			String timerString = record.get("TIMER [dd-MM-yyyy HH:mm:ss]");
			String alchemyKeyUrl = getAlchemyKeyUrl();
			String etherscanApiKey = getEtherscanApiKey();
			String webhookUrl = getWebhookUrl();

			EthMintingTask task = new EthMintingTask(counter, useSafeMode, secretKey, contractAddress, function,
					functionParameters, transactionData, transactionValue, amountGasAddedToRapid, gasLimit, timerString,
					null, alchemyKeyUrl, etherscanApiKey, webhookUrl, twoCaptchaKeys);
			task.start();
			tasksList.add(task);
		}

		in.close();

		while (true) {
			monitorStatus(contractAddress);
			Thread.sleep(1000);
		}

	}

	public void runTasksFromTX() throws Exception {

		System.out.print(ANSI_YELLOW + "\n>>> Set Reference-TX (URL/Hash): " + ANSI_RESET);

		Scanner inScanner = new Scanner(System.in);
		String input = inScanner.nextLine().strip();

		String tx;

		if (input.contains("etherscan")) {
			tx = input.split(Pattern.quote("/"))[input.split(Pattern.quote("/")).length - 1].replace("/", "");
		} else {
			tx = input;
		}

		System.out.println(ANSI_GREY + "\n>>> Scraping Details..." + ANSI_RESET);

		String contractAddress = null;
		String transactionData = null;
		String transactionValue = null;
		Session session = Requests.session();

		// 1. Get sender Address to get transactionData

		RawResponse newSession = session
				.get("https://api.etherscan.io/api?module=proxy&action=eth_getTransactionByHash&txhash=" + tx)
				.socksTimeout(60_000).connectTimeout(60_000).send();
		String response = newSession.readToText();
		JSONObject o = new JSONObject(new JSONTokener(response));

		while (!response.contains("to\":\"")) {
			System.out.println(ANSI_RED + "Scraping TX failed! Make sure you are using a succeeded TX..." + ANSI_RESET);
			Thread.sleep(1000);

			System.out.print(ANSI_YELLOW + "\n>>> Set Reference-TX (URL/Hash): " + ANSI_RESET);

			input = inScanner.nextLine().strip();
			if (input.contains("etherscan")) {
				tx = input.split(Pattern.quote("/"))[input.split(Pattern.quote("/")).length - 1].replace("/", "");
			} else {
				tx = input;
			}

			newSession = session
					.get("https://api.etherscan.io/api?module=proxy&action=eth_getTransactionByHash&txhash=" + tx)
					.socksTimeout(60_000).connectTimeout(60_000).send();
			response = newSession.readToText();
			o = new JSONObject(new JSONTokener(response));
		}

		contractAddress = o.getJSONObject("result").getString("to");
		transactionData = o.getJSONObject("result").getString("input");

		String hexWithout0x = o.getJSONObject("result").getString("value").substring(2,
				o.getJSONObject("result").getString("value").length());

		BigInteger value = new BigInteger(hexWithout0x, 16);

//		if (!value.substring(0, 1).toLowerCase().equals("0x")) {
//			value = "0x" + value;
//		}

		transactionValue = Convert.fromWei(value.toString(), Unit.ETHER).toString();

		String methodNameHex = transactionData.substring(0, 10);
		String methodName = getMethodName(contractAddress, methodNameHex);

		System.out.println(ANSI_GREEN + "\n>>> Contract: " + contractAddress + " | Method: " + methodName
				+ " | Transaction Value: " + transactionValue + "E" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Set GAS_PRICE (Rapid/Rapid+X/X): " + ANSI_RESET);
		String amountGasAddedToRapid = inScanner.nextLine().strip();

		monitorStock(contractAddress);

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthMinting.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		disableMainHeader();
		System.out.println("");
		System.out.println(ANSI_YELLOW + "Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		cliTools.setTitle("N3RO BOT - " + new EthTools().getRapidGas() + " GWEI - Pending: " + 0 + " Failed: " + 0
				+ " Success: " + 0);

		int counter = 0;
		for (CSVRecord record : records) {
			counter++;
			String secretKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String useSafeMode = "FALSE";

			String gasLimit = record.get("GAS_LIMIT [AUTO / CUSTOM]");
			String timerString = record.get("TIMER [dd-MM-yyyy HH:mm:ss]");
			String alchemyKeyUrl = getAlchemyKeyUrl();
			String etherscanApiKey = getEtherscanApiKey();
			String webhookUrl = getWebhookUrl();
			String function = "";
			String functionParameters = "";

			EthMintingTask task = new EthMintingTask(counter, useSafeMode, secretKey, contractAddress, function,
					functionParameters, transactionData, transactionValue, amountGasAddedToRapid, gasLimit, timerString,
					null, alchemyKeyUrl, etherscanApiKey, webhookUrl, twoCaptchaKeys);
			task.start();
			tasksList.add(task);
		}

		in.close();

		while (true) {
			monitorStatus(contractAddress);
			Thread.sleep(1000);
		}

	}

	public String getMethodName(String contract, String hexName) throws Exception {

		Session session = Requests.session();

		RawResponse newSession = session.get("https://api.etherscan.io/api?module=contract&action=getabi&address="
				+ contract + "&apikey=" + new Main().getEtherscanApiKey()).socksTimeout(60_000).connectTimeout(60_000)
				.send();

		String response = newSession.readToText();

		while (newSession.statusCode() != 200) {
			newSession = session.get("https://api.etherscan.io/api?module=contract&action=getabi&address=" + contract
					+ "&apikey=" + new Main().getEtherscanApiKey()).socksTimeout(60_000).connectTimeout(60_000).send();
			response = newSession.readToText();
			System.out.println(ANSI_RED + "Scraping Method Name Failed!" + ANSI_RESET);
		}

		JSONObject o = new JSONObject(new JSONTokener(response));

		JSONArray abi = new JSONArray(o.getString("result"));

		for (int i = 0; i < abi.length(); i++) {
			try {
				JSONObject currentFunction = new JSONObject(new JSONTokener(abi.get(i).toString()));

				Function f2 = Function.fromJson(currentFunction.toString());
				String selector = "0x" + f2.selectorHex().toLowerCase();

				if (selector.equals(hexName.toLowerCase())) {
					return currentFunction.getString("name");
				}

			} catch (Exception e) {

			}

		}

		return "NOT_FOUND";

	}

	public void monitorStock(String contract) throws Exception {

		try {
			System.out.print(ANSI_YELLOW + "\n>>> Wait till X % Supply minted (X/empty): " + ANSI_RESET);

			Scanner inScanner = new Scanner(System.in);
			String input = inScanner.nextLine().strip();

			if (!input.strip().equals("")) {
				System.out.println(ANSI_GREY + "\n>>> Scraping Details..." + ANSI_RESET);

				BigDecimal targetSizePerCent = new BigDecimal(input);
				BigDecimal collectionSize = new BigDecimal(getCollectionSize(contract));
				BigDecimal amountMinted = new BigDecimal(getAmountMinted(contract, new Main().getEtherscanApiKey()));
				BigDecimal amountMintedInPerCent = amountMinted.divide(collectionSize, 4, RoundingMode.HALF_DOWN)
						.multiply(new BigDecimal("100"));

				System.out.println("");
				System.out.println(ANSI_GREY + "[ETHER] - [" + contract + "] - Monitoring Supply. Already Minted: "
						+ amountMinted + "/" + collectionSize + " ~ " + amountMintedInPerCent + "%");

				while (amountMintedInPerCent.doubleValue() < targetSizePerCent.doubleValue()) {
					Thread.sleep(2000);
					amountMinted = new BigDecimal(getAmountMinted(contract, new Main().getEtherscanApiKey()));
					amountMintedInPerCent = amountMinted.divide(collectionSize, 4, RoundingMode.HALF_DOWN)
							.multiply(new BigDecimal("100"));

					System.out.println(ANSI_GREY + "[ETHER] - [" + contract + "] - Monitoring Supply. Already Minted: "
							+ amountMinted + "/" + collectionSize + " ~ " + amountMintedInPerCent + "%");
				}

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "Invalid Input!\n" + ANSI_RESET);
			e.printStackTrace();
			Thread.sleep(500);
			monitorStock(contract);
		}

	}

	public int getAmountMinted(String contract, String apiKey) throws Exception {

		Config config = new Config().connectTimeout(30_000);
		UnirestInstance unirest = new UnirestInstance(config);

		HttpResponse<String> response = unirest
				.get("https://api.etherscan.io/api?module=stats&action=tokensupply&contractaddress=" + contract
						+ "&apikey=" + apiKey)
				.header("accept", "application/json").asString();

		String responseBody = response.getBody();

		while (response.getStatus() != 200) {
			response = unirest.get("https://api.etherscan.io/api?module=stats&action=tokensupply&contractaddress="
					+ contract + "&apikey=" + apiKey).header("accept", "application/json").asString();

			responseBody = response.getBody();

			System.out.println(ANSI_RED + "Fetching Etherscan Failed. Retrying..." + ANSI_RESET);
			Thread.sleep(2000);

		}

		JSONObject o = new JSONObject(new JSONTokener(responseBody));

		return Integer.valueOf(o.getString("result"));
	}

	public int getCollectionSize(String contract) throws Exception {
		Config config = new Config().connectTimeout(30_000);
		UnirestInstance unirest = new UnirestInstance(config);

		HttpResponse<String> response = unirest.get("https://api.opensea.io/api/v1/asset_contract/" + contract)
				.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
				.asString();

		String responseBody = response.getBody();

		while (response.getStatus() != 200) {
			response = unirest.get("https://api.opensea.io/api/v1/asset_contract/" + contract)
					.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
					.asString();

			responseBody = response.getBody();

			System.out.println(ANSI_RED + "Fetching OS Failed. Retrying..." + ANSI_RESET);
			Thread.sleep(1000);

		}

		JSONObject o = new JSONObject(new JSONTokener(responseBody));

		String slug = o.getJSONObject("collection").getString("slug");

		// get stock
		response = unirest.get("https://api.opensea.io/api/v1/collection/" + slug + "/stats")
				.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
				.asString();

		responseBody = response.getBody();

		while (response.getStatus() != 200) {
			response = unirest.get("https://api.opensea.io/api/v1/collection/" + slug + "/stats")
					.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
					.asString();

			responseBody = response.getBody();

			System.out.println(ANSI_RED + "Fetching Size Failed. Retrying..." + ANSI_RESET);
			Thread.sleep(1000);

		}

		o = new JSONObject(new JSONTokener(responseBody));

		return (int) o.getJSONObject("stats").getDouble("total_supply");
	}

	public void displayContract(String contract) {

	}

	public void monitorStatus(String contract) {
		try {
			int amountPending = 0;
			int amountFailed = 0;
			int amountSuccess = 0;
			for (EthMintingTask task : tasksList) {

				if (task.getStatus().equals("PENDING")) {
					amountPending++;
				} else if (task.getStatus().equals("FAILED")) {
					amountFailed++;
				} else if (task.getStatus().equals("SUCCESS")) {
					amountSuccess++;

				}
			}

			String amountPendingInContract = new EthTools().getAmountPendingTxContract(contract);
			long gas = new EthTools().getRapidGas();
			cliTools.setTitle("N3RO BOT - " + gas + " GWEI - Contract Pending TX: " + amountPendingInContract
					+ " - Pending: " + amountPending + " Failed: " + amountFailed + " Success: " + amountSuccess);

		} catch (Exception e) {
		}

	}
}
