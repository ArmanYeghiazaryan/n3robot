package modulesEth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Convert;

import com.esaulpaugh.headlong.abi.Address;

import main.Main;

public class RunEthTopupWalletTask extends Main {
	List<List<EthTopupWalletTask>> tasksLists = new ArrayList<List<EthTopupWalletTask>>();

	public RunEthTopupWalletTask() throws InterruptedException, IOException {
		runTasks();
	}

	public void runTasks() throws InterruptedException, IOException {
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthWalletTopup.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\eth\\tasksEthWalletTopup.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		int tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

//		System.out.println(ANSI_CYAN + "Initializing " + tasksAmount
//				+ " task(s)...\n" + ANSI_RESET);
//		Thread.sleep(1000);

		// Add all tasks to TasksList

		for (CSVRecord record : records) {

			// Get fromWallet. Check if it is in file or not.
			String fromWallet = "";
			if (record.get("FROM_WALLET").length() != 64) {
				fromWallet = getEtherCredentialFromWalletsFile("PRIVATE_KEY", record.get("FROM_WALLET"));
			} else {
				fromWallet = record.get("FROM_WALLET");
			}

			// Get toWallet. Check if is in file or not.
			String toWallet = "";
			if (record.get("TO_WALLET").subSequence(0, 2).equals("0x")) {
				toWallet = record.get("TO_WALLET");
			} else {
				toWallet = getEtherCredentialFromWalletsFile("ADDRESS", record.get("TO_WALLET"));
			}

			String transactionValue = record.get("TRANSACTION_VALUE [ALL / X ETH]");
			String alchemyKeyUrl = getAlchemyKeyUrl();
			String etherscanApiKey = getEtherscanApiKey();

			EthTopupWalletTask ethTopupWalletTask = new EthTopupWalletTask(fromWallet, toWallet, transactionValue,
					alchemyKeyUrl, etherscanApiKey, webhookUrl);

			boolean listExists = false;
			for (List<EthTopupWalletTask> list : tasksLists) {

				boolean hasAllTransactionInIt = false;
				for (EthTopupWalletTask task : list) {
					if (task.getTransactionValue().toLowerCase().equals("all")) {
						hasAllTransactionInIt = true;
						break;
					}

				}

				if (!hasAllTransactionInIt) {
					for (EthTopupWalletTask task : list) {

						if (!ethTopupWalletTask.getTransactionValue().toLowerCase().equals("all")
								&& task.getSenderPrivateKey().toLowerCase().equals(fromWallet.toLowerCase())) {
							listExists = true;
							list.add(ethTopupWalletTask);
							break;
						}

					}
				}

			}

			if (!listExists) {
				List<EthTopupWalletTask> newList = new ArrayList<EthTopupWalletTask>();
				newList.add(ethTopupWalletTask);
				tasksLists.add(newList);
			}

		}
		in.close();

		String input = "";

		if (tasksAmount != tasksLists.size()) {
			System.out.println(ANSI_YELLOW + "\n>>> " + tasksAmount + " Task(s) loaded. Type 'y' to bundle them to "
					+ ANSI_GREEN + tasksLists.size() + ANSI_RESET + ANSI_YELLOW + " Transaction(s) or hit enter..."
					+ ANSI_RESET);

			Scanner inScanner = new Scanner(System.in);
			input = inScanner.nextLine();
		}

		if (input.toLowerCase().equals("y")) {
			// bundle
			int counter = 0;
			for (List<EthTopupWalletTask> list : tasksLists) {
				counter++;
				// if all or size == 1

				if (list.size() == 1) {
					for (EthTopupWalletTask task : list) {
						task.start();

						if (task.getTransactionValue().toLowerCase().equals("all")) {
							while (!task.getIsFinished()) {
								Thread.sleep(1000);
							}
						}
					}
				} else {
					// if bundle
					List<Address> addressList = new ArrayList<Address>();
					List<String> valuesList = new ArrayList<String>();
					BigDecimal valueAll = new BigDecimal(0);
					String privateKey = "";

					for (EthTopupWalletTask task : list) {
						privateKey = task.getSenderPrivateKey();

						addressList.add(Address.wrap(Address.toChecksumAddress(task.getReceiverPublic())));
						valuesList.add(Convert.toWei(task.getTransactionValue(), Convert.Unit.ETHER).toBigInteger()
								.toString());
						valueAll = valueAll.add(new BigDecimal(task.getTransactionValue()));
					}

					EthMintingTask topUpForNext = new EthMintingTask(counter, "false", privateKey,
							"0xD152f549545093347A162Dce210e7293f1452150", "disperseEther",
							String.valueOf(addressList) + "," + String.valueOf(valuesList), "", valueAll.toString(),
							"rapid", "auto", "", null, alchemyKeyUrl, etherscanApiKey, webhookUrl, null);
					topUpForNext.start();

				}

			}

		} else {
			// do not bundle
			for (List<EthTopupWalletTask> list : tasksLists) {
				for (EthTopupWalletTask task : list) {
					task.start();
//					while (!task.getIsFinished()) {
//						Thread.sleep(1000);
//					}

				}
			}

		}

	}

}
