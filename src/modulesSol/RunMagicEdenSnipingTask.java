package modulesSol;

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
import modulesEth.EthMintingTask;
import modulesEth.EthTopupWalletTask;

public class RunMagicEdenSnipingTask extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private boolean snipe;

	public RunMagicEdenSnipingTask(boolean snipe) throws InterruptedException, ExecutionException, IOException {
		this.snipe = snipe;

		if (snipe) {
			runSniperTasks();

		} else {
			runMonitorTasks();
		}
	}

	public void runSniperTasks() throws InterruptedException, ExecutionException, IOException {

		Path path = Paths
				.get(System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksMagicEdenSniper.csv");
		Reader in = new FileReader(
				System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksMagicEdenSniper.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_CYAN + "Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		for (CSVRecord record : records) {
			String collectionName = record.get("COLLECTION");
			String price = record.get("MAX_PRICE [FLOOR / X SOL / X%]");
			String privateKey = record.get("SECRET_KEY");
			String webhookUrl = getWebhookUrl();

			SolanaMESniperTask task = new SolanaMESniperTask(privateKey, collectionName, price, webhookUrl, true);
			task.start();

		}

	}

	public void runMonitorTasks() throws InterruptedException, ExecutionException, IOException {

		Path path = Paths
				.get(System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksMagicEdenMonitor.csv");
		Reader in = new FileReader(
				System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\sol\\tasksMagicEdenMonitor.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_CYAN + "Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		for (CSVRecord record : records) {
			String collectionName = record.get("COLLECTION");
			String price = record.get("MAX_PRICE [FLOOR / X SOL / X%]");
			String webhookUrl = getWebhookUrl();

			SolanaMESniperTask task = new SolanaMESniperTask("", collectionName, price, webhookUrl, false);
			task.start();

		}

	}
}
