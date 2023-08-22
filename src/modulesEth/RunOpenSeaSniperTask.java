package modulesEth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import engines.MintingEngine;
import engines.ToolsEngine;
import main.Main;
import modulesSol.SolanaMESniperTask;

public class RunOpenSeaSniperTask extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public RunOpenSeaSniperTask() throws InterruptedException, ExecutionException, IOException {
		runTasks();
	}

	public void runTasks() throws InterruptedException, ExecutionException, IOException {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\eth\\tasksOpenSeaSniping.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksOpenSeaSniping.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		int counter = 0;
		for (CSVRecord record : records) {
			counter++;
			String safeMode = record.get("SAFE_MODE [TRUE / FALSE]");
			String collectionName = record.get("COLLECTION_NAME");
			String maxAmount = record.get("MAX_AMOUNT");
			String maxPrice = record.get("MAX_PRICE [FLOOR / X ETH / X%]");
			String traits = record.get("TRAITS [TRAIT1=EXP;...]");
			String gasPrice = record.get("GAS_PRICE [RAPID / RAPID + X / X]");
			String gasLimit = record.get("GAS_LIMIT");
			String privateKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String proxy = record.get("PROXY [TRUE / FALSE]");

			String alchemyKeyUrl = getAlchemyKeyUrl();
			String etherscanApiKey = getEtherscanApiKey();
			String webhookURL = getWebhookUrl();

			OpenSeaSniperTask task = new OpenSeaSniperTask(counter, safeMode, collectionName, maxAmount, maxPrice,
					traits, gasPrice, gasLimit, privateKey, proxy, alchemyKeyUrl, etherscanApiKey, webhookURL);
			task.start();

			Thread.sleep(getDelayInMs());

		}

		in.close();

	}
}
