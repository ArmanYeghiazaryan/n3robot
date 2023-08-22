package modulesEth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import engines.MintingEngine;
import engines.ToolsEngine;
import main.Main;

public class RunEthCancelTransaction extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public RunEthCancelTransaction() throws InterruptedException, ExecutionException, IOException {
		runTasks();
	}

	public void runTasks() throws InterruptedException, ExecutionException, IOException {

		String content = Files.readString(
				Path.of(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json"),
				Charset.defaultCharset());

		System.out.println();
		JSONArray array = new JSONArray(new JSONTokener(content));

		for (int i = 1; i <= array.length(); i++) {
			JSONObject entry = new JSONObject(new JSONTokener(array.get(i - 1).toString()));
			String contractAddress = entry.getString("contractAddress");
			String transactionValue = entry.getString("transactionValue");
			String gasPrice = entry.getString("gasPrice");
			String transactionHash = entry.getString("transactionHash");
			String timeStamp = entry.getString("timeStamp");

			System.out.println(ANSI_YELLOW + "[" + i + "] " + timeStamp + " | " + contractAddress + " | "
					+ transactionValue + " ETH | " + gasPrice + " GWEI | TX: " + transactionHash.substring(0, 4) + "..."
					+ transactionHash.substring(transactionHash.length() - 4, transactionHash.length()) + ANSI_RESET);
		}

		System.out.println(ANSI_GREY + "[" + (array.length() + 1) + "] Cancel All" + ANSI_RESET);
		System.out.println(ANSI_GREY + "[" + (array.length() + 2) + "] Clear Transactions" + ANSI_RESET);
		System.out.println(ANSI_GREY + "[ENTER] Refresh Transactions" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.nextLine();
		int i = 0;

		try {
			i = Integer.valueOf(input);

			if (i == 0 || i > (array.length() + 2)) {
				throw new Exception("INVALID_INPUT");
			}

			if (i == (array.length() + 2)) {
				// Clear logs
				createNewPendingTransactionsFile();
			}

			else if (i == (array.length() + 1)) {
				// Cancel all

				System.out.print(ANSI_YELLOW + "\n>>> Set Gas Price (RAPID / GWEI Amount): " + ANSI_RESET);

				in = new Scanner(System.in);
				input = in.nextLine();

				if (!input.toLowerCase().equals("rapid")) {
					i = Integer.valueOf(input);

				}

				for (int j = 0; j < array.length(); j++) {
					JSONObject entry = array.getJSONObject(j);

					String secretKey = entry.getString("secretKey");
					String contractAddress = entry.getString("contractAddress");
					String transactionData = entry.getString("transactionData");
					String transactionValue = entry.getString("transactionValue");

					String gaslimit = entry.getString("providedGasLimit");
					String nonce = entry.getString("nonce");
					String alchemyKeyUrl = entry.getString("alchemyKeyUrl");
					String etherscanApiKey = entry.getString("etherscanApiKey");
					String webhookUrl = entry.getString("webhookUrl");
					String transactionHash = entry.getString("transactionHash");

					EthMintingTask ethCancelTask = new EthMintingTask(j, "FALSE", secretKey, contractAddress, "", "",
							"0x", "0", input, gaslimit, "", nonce, alchemyKeyUrl, etherscanApiKey, webhookUrl, null);

					ethCancelTask.start();
				}

				createNewPendingTransactionsFile();

			}

			else {
				JSONObject entry = new JSONObject(new JSONTokener(array.get(i - 1).toString()));
				String secretKey = entry.getString("secretKey");
				String contractAddress = entry.getString("contractAddress");
				String gaslimit = entry.getString("providedGasLimit");
				String nonce = entry.getString("nonce");
				String alchemyKeyUrl = entry.getString("alchemyKeyUrl");
				String etherscanApiKey = entry.getString("etherscanApiKey");
				String webhookUrl = entry.getString("webhookUrl");
				String transactionHash = entry.getString("transactionHash");

				System.out.print(ANSI_YELLOW + "\n>>> Set Gas Price (RAPID / GWEI Amount): " + ANSI_RESET);

				in = new Scanner(System.in);
				input = in.nextLine();

				if (!input.toLowerCase().equals("rapid")) {
					i = Integer.valueOf(input);

				}

				EthMintingTask ethCancelTask = new EthMintingTask(i, "FALSE", secretKey, contractAddress, "", "", "0x",
						"0", input, gaslimit, "", nonce, alchemyKeyUrl, etherscanApiKey, webhookUrl, null);

				ethCancelTask.start();
				removeEntryFromPendingTransactions(transactionHash);

			}

			Thread.sleep(500);
			runTasks();

		} catch (Exception e) {
			if (input.length() == 0) {
				System.out.println(ANSI_YELLOW + "Refreshing...\n" + ANSI_RESET);

			} else {
				System.out.println(ANSI_RED + "Invalid Input!\n" + ANSI_RESET);

			}

			Thread.sleep(500);
			runTasks();
		}

	}
}
