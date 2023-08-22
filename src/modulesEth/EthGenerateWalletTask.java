package modulesEth;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import com.opencsv.CSVWriter;

public class EthGenerateWalletTask extends Thread {

	private String password;
	private int amount;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public EthGenerateWalletTask(String password, int amount) {
		this.password = password;
		this.amount = amount;
	}

	public void run() {
//		System.out.println(
//				"[ETHER] - [" + dtf.format(now.now()) + "] - [-] - Generating " + amount + " Ether Wallet(s)...");

		for (int i = 1; i <= amount; i++) {
			try {
				System.out.println(
						"[ETHER] - [" + dtf.format(now.now()) + "] - [" + i + "] - Generating Ether Wallet...");
				String walletFileName = WalletUtils.generateFullNewWalletFile(password,
						new File(System.getProperty("user.dir") + "/target/"));
				Credentials credentials = WalletUtils.loadCredentials(password,
						System.getProperty("user.dir") + "/target/" + walletFileName);

				addToCSV(credentials.getAddress(), credentials.getEcKeyPair().getPrivateKey().toString(16));

				File file = new File(System.getProperty("user.dir") + "/target/" + walletFileName);
				file.delete();

			} catch (Exception e) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + i + "] - Retrying: "
						+ e.toString().toUpperCase() + ANSI_RESET);
				i = i - 1;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
			}

		}
		System.out.println(
				ANSI_GREEN + "[ETHER] - [" + dtf.format(now.now()) + "] - Generated Ether Wallet(s)!" + ANSI_RESET);

	}

	synchronized void addToCSV(String address, String privateKey) throws Exception {
		FileWriter credentialsFile = new FileWriter(System.getProperty("user.dir") + "\\wallets\\etherWallets.csv",
				true);
		CSVWriter writer = new CSVWriter(credentialsFile);
		String[] data1 = { "", address, privateKey };
		writer.writeNext(data1);
		writer.close();
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREY = "\u001b[30;1m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
}
