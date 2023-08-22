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

public class RunEthCheckBalanceTask extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	// ask if check 1. Single Wallet or 2. Check whole Wallets file

	public RunEthCheckBalanceTask() throws InterruptedException, ExecutionException, IOException {
		runTasks();
	}

	public void runTasks() throws InterruptedException, ExecutionException, IOException {

		System.out.println(ANSI_WHITE + "\n> 1. Check Single Wallet");
		System.out.println("> 2. Check All Wallets" + ANSI_RESET);
		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		EthTopupWalletTask walletChecker = new EthTopupWalletTask(null, null, "0.0", getAlchemyKeyUrl(), null, null);

		switch (input) {
		case "1":
			System.out.print(ANSI_YELLOW + "\n>>> Set Wallet Address: " + ANSI_RESET);
			Scanner in2 = new Scanner(System.in);
			String input2 = in2.next();
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
