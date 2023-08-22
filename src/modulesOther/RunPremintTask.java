package modulesOther;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.web3j.crypto.Credentials;

import main.CLITools;
import main.Main;
import modulesEth.EthMintingTask;
import modulesEth.EthTopupWalletTask;
import net.dongliu.requests.Cookie;

public class RunPremintTask extends Main {

	List<PremintTask> tasksList = new ArrayList<PremintTask>();
	CLITools cliTools = new CLITools(false);
	int tasksAmount;

	public RunPremintTask() throws Exception {
//		runRaffleTasks();
		disableMainHeader();

	}

	public void runRaffleTasks() throws InterruptedException, IOException {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntries.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintEntries.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO BOT - Premint Entries - Status: " + 0 + "/" + this.tasksAmount);

		Thread.sleep(1000);
		int taskId = 0;

		for (CSVRecord record : records) {
			taskId++;
			String raffleUrl = record.get("RAFFLE_URL");
			String customField = record.get("CUSTOM_FIELD");
			String privateKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			HashSet<String> twoCaptchakeys = get2CaptchaKeys();

			String webhookURL = getWebhookUrl();
			int delay = getDelayInMs();
			PremintTask task = new PremintTask(taskId, "RAFFLE", raffleUrl, customField, "", "", privateKey, proxy,
					twoCaptchakeys, delay, tasksAmount, null);
			task.start();
			tasksList.add(task);

			Thread.sleep(150);
			while (amountCurrentTasks() > 0) {
				Thread.sleep(2000);
			}

			Thread.sleep(getDelayInMs());

		}

		in.close();

	}

	public void runTrainTasks() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintTrain.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintTrain.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO Bot - Premint Train - Status: " + 0 + "/" + this.tasksAmount);

		Thread.sleep(1000);

		int taskId = 0;

		for (CSVRecord record : records) {
			taskId++;
			String raffleUrl = record.get("RAFFLE_URL");
			String customField = record.get("CUSTOM_FIELD");
			String transactionValue = record.get("TRANSACTION_VALUE [X / ALL]");
			String gasPrice = record.get("GAS_PRICE [RAPID / RAPID + X / X]");
			String privateKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			HashSet<String> twoCaptchakeys = get2CaptchaKeys();
			String webhookURL = getWebhookUrl();
			int delay = getDelayInMs();

			PremintTask task = new PremintTask(taskId, "RAFFLE", raffleUrl, customField, transactionValue, gasPrice,
					privateKey, proxy, twoCaptchakeys, delay, tasksAmount, null);
			tasksList.add(task);

		}
		in.close();

		int index = 0;
		taskId = 0;
		for (PremintTask currentTask : tasksList) {

			taskId++;
			int counter = 0;
			for (String url : currentTask.getRaffleURL().split(";")) {

				String customField = currentTask.getCustomField();
				if (url.contains("|")) {
					String arr[] = url.split(Pattern.quote("|"));
					url = arr[0];
					customField = arr[1];
				}

				PremintTask newTask = new PremintTask(taskId, "RAFFLE", url, customField,
						currentTask.getTransactionValue(), currentTask.getGasPrice(), currentTask.getPrivateKey(),
						currentTask.getProxy(), currentTask.getTwoCaptchaKeys(), currentTask.getDelay(),
						currentTask.getTasksAmount(), currentTask.getWebhookURL());
				newTask.setRaffleURL(url);

				if (counter == 0) {
					newTask.start();

				} else {
					newTask.run();

				}
				while (!newTask.getIsFinished()) {
					Thread.sleep(2000);
				}
				counter++;

			}

			int nextIndex = index + 1;
			if (nextIndex < tasksList.size()) {
				PremintTask nextTask = tasksList.get(nextIndex);

				Credentials credentials = Credentials.create(nextTask.getPrivateKey());
				String publicNextTask = credentials.getAddress();

				EthMintingTask topUpForNext = new EthMintingTask(taskId, "false", currentTask.getPrivateKey(),
						publicNextTask, "", "", "0x", currentTask.getTransactionValue(), currentTask.getGasPrice(),
						"21000", "", null, alchemyKeyUrl, etherscanApiKey, webhookUrl, null);
				topUpForNext.start();

				while (!topUpForNext.getIsFinished()) {
					Thread.sleep(2000);
				}

			}

			index++;
			Thread.sleep(getDelayInMs());

		}

	}

	public int amountCurrentTasks() {
		int amountUnfinished = 0;

		for (PremintTask currentTask : tasksList) {
			if (!currentTask.getIsFinished()) {
				amountUnfinished++;
			}
		}

		return amountUnfinished;

	}

	public void runWinChecker() throws InterruptedException, IOException {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintChecker.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintChecker.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO BOT - Premint Checker - Status: " + 0 + "/" + this.tasksAmount);

		Thread.sleep(1000);

		int taskId = 0;
		for (CSVRecord record : records) {
			taskId++;
			String raffleUrl = record.get("RAFFLE_URL");
			String privateKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");

			String webhookURL = getWebhookUrl();
			int delay = getDelayInMs();
			PremintTask task = new PremintTask(taskId, "CHECKER", raffleUrl, null, "", "", privateKey, proxy, null,
					delay, tasksAmount, webhookURL);
			task.start();
			tasksList.add(task);

			Thread.sleep(150);
			while (amountCurrentTasks() > 0) {
				Thread.sleep(2000);
			}

		}

		in.close();

	}

	public void runInitializer() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintInitializer.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\other\\tasksPremintInitializer.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO BOT - Premint Initializer - Status: " + 0 + "/" + this.tasksAmount);

		Thread.sleep(1000);

		int counter = 0;

		int taskId = 0;
		for (CSVRecord record : records) {
			taskId++;
			String raffleUrl = "https://www.premint.xyz/n3ro/";
			String privateKey = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("PRIVATE_KEY"));
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");

			String webhookURL = getWebhookUrl();
			int delay = getDelayInMs();
			PremintTask task = new PremintTask(taskId, "initializer", raffleUrl, null, "", "", privateKey, proxy, null,
					delay, tasksAmount, webhookURL);
			task.start();

			counter++;

			if (counter < tasksAmount) {
				System.out.print(ANSI_YELLOW + ">>> Press Enter to launch next task..." + ANSI_RESET);

				Scanner inScanner = new Scanner(System.in);
				String input = inScanner.nextLine();
			}

		}

		in.close();

	}

}
