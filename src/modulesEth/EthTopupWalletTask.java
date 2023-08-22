package modulesEth;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import com.opencsv.CSVWriter;

import main.DiscordWebhook;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class EthTopupWalletTask extends Thread {

	private String senderPrivateKey;
	private String senderPublicAddress;
	private String receiverPublicAddress;
	private String transactionValueString;
	private String txHash;
	private String alchemyKeyUrl;
	private String etherscanApiKey;
	private String webhookUrl;
	private Credentials credentials;
	private Web3j web3j;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private boolean isFinished;
	private String status;

	public EthTopupWalletTask(String senderPrivateKey, String receiverPublicAddress, String transactionValue,
			String alchemyKeyUrl, String etherscanApiKey, String webhookUrl) {
		System.err.close();
		this.senderPrivateKey = senderPrivateKey;
		this.receiverPublicAddress = receiverPublicAddress;
		this.transactionValueString = transactionValue;
		this.alchemyKeyUrl = alchemyKeyUrl;
		this.etherscanApiKey = etherscanApiKey;
		this.webhookUrl = webhookUrl;
		this.web3j = Web3j.build(new HttpService(alchemyKeyUrl));
	}

	public void run() {

		try {
			sendEther();
//			sendWebhook("PENDING");
			pollTransactionStatus();
		} catch (Exception e) {
			if (senderPublicAddress == null) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPrivateKey
						+ "] - Retrying: " + e.getMessage().toUpperCase() + ANSI_RESET);
			} else {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
						+ "] - Retrying: " + e.getMessage().toUpperCase() + ANSI_RESET);
			}

			try {
				Thread.sleep(1000);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			run();

		}
	}

	public void sendEther() throws InterruptedException, IOException, TransactionException, Exception {

		System.out.println("[ETHER] - [" + dtf.format(now.now())
				+ "] - [------------------------------------------] - Getting Ether Wallet...");
		this.credentials = Credentials.create(senderPrivateKey);

		this.senderPublicAddress = credentials.getAddress();

		BigDecimal transactionValue = new BigDecimal(0);

		// Get TransactionValue
		if (transactionValueString.toLowerCase().equals("all")) {

			transactionValue = getWalletBalance(senderPublicAddress, null);

			BigDecimal gasLimit = BigDecimal.valueOf(21000);
			BigDecimal gasFeesInEth = gasLimit.multiply(BigDecimal.valueOf(getRapidGas()))
					.multiply(BigDecimal.valueOf(0.000000001));

			transactionValue = transactionValue.subtract(gasFeesInEth).setScale(4, RoundingMode.HALF_DOWN);

		} else {
			transactionValue = new BigDecimal(transactionValueString);
		}

		System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress + "] - Sending '"
				+ transactionValue.toString() + " ETH' To '" + receiverPublicAddress + "'...");

		this.transactionValueString = transactionValue.toString();
		TransactionReceipt transactionReceipt = Transfer
				.sendFunds(web3j, credentials, receiverPublicAddress, transactionValue, Convert.Unit.ETHER).send();

		this.txHash = transactionReceipt.getTransactionHash();
		System.out.println(ANSI_YELLOW + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
				+ "] - Received Transaction Hash: " + txHash + ANSI_RESET);

	}

	public boolean getIsFinished() {
		return this.isFinished;
	}

	public BigDecimal getWalletBalance(String publicAddress, String name)
			throws InterruptedException, ExecutionException {
		BigDecimal balance = null;

		try {
			EthGetBalance ethGetBalance = web3j.ethGetBalance(publicAddress, DefaultBlockParameterName.LATEST)
					.sendAsync().get();

			BigInteger wei = ethGetBalance.getBalance();
			BigInteger inte = new BigInteger("1000000000000000000");

			BigDecimal curreny = new BigDecimal(inte);
			BigDecimal eth = new BigDecimal(wei);
			balance = eth.divide(curreny);

			if (name != null) {
				System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [Name: '" + name + "' | "
						+ publicAddress + "] - Wallet Contains " + balance + "E");
			}

		} catch (Exception e) {
			System.out.println(
					"[ETHER] - [" + dtf.format(now.now()) + "] - [" + publicAddress + "] - Error: " + e.getMessage());

		}

		return balance;
	}

	public void pollTransactionStatus() throws InterruptedException {

		System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
				+ "] - Monitoring Transaction Status..." + ANSI_RESET);

		while (!this.isFinished) {
			Session session = Requests.session();

			RawResponse newSession = session
					.get("https://api.etherscan.io/api?module=transaction&action=gettxreceiptstatus&txhash=" + txHash
							+ "&apikey=" + etherscanApiKey)
					.socksTimeout(60_000).connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
						+ "] - Retrying: " + newSession.statusCode());
				Thread.sleep(1000);
				pollTransactionStatus();
			}

			String response = newSession.readToText();

			try {
				String[] array = response.split("status\":\"");
				String temp = array[1];
				String[] array2 = temp.split("\"");
				String status = array2[0];

				if (status.equals("1")) {
					System.out.println(ANSI_GREEN + "[ETHER] - [" + dtf.format(now.now()) + "] - ["
							+ senderPublicAddress + "] - Transaction Succeeded." + ANSI_RESET);
					sendWebhook("SUCCESS");
					this.isFinished = true;
					this.status = "SUCCESS";

				} else if (status.equals("0")) {
					System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
							+ "] - Transaction Failed." + ANSI_RESET);
					sendWebhook("FAILED");
					this.isFinished = true;
					this.status = "FAILED";

				} else {
					Thread.sleep(3000);
				}
			} catch (Exception e) {
				Thread.sleep(5000);
				pollTransactionStatus();
			}
		}

	}

	public String getStatus() {
		return this.status;
	}

	public long getRapidGas() throws InterruptedException {
		Session session = Requests.session();

		RawResponse newSession = session.get("https://blocknative-api.herokuapp.com/data").socksTimeout(60_000)
				.connectTimeout(60_000).send();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
					+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

			Thread.sleep(500);
			getRapidGas();

		}
		String response = newSession.readToText();
		JSONObject o = new JSONObject(new JSONTokener(response));
		int gas = new JSONObject(new JSONTokener(o.getJSONArray("estimatedPrices").get(0).toString())).getInt("price");

		System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + this.senderPublicAddress
				+ "] - Rapid Gas Currently: " + gas + " GWEI" + ANSI_RESET);

		return Long.valueOf(gas) - 2;
	}

	public void sendWebhook(String status) throws IOException, LineUnavailableException, InterruptedException {

		this.txHash = this.txHash.toLowerCase();

		Color color = null;

		if (status.equals("PENDING")) {
			color = Color.orange;
		} else if (status.equals("SUCCESS")) {
			color = Color.green;
		} else if (status.equals("FAILED")) {
			color = Color.red;
		}

		String title = "";
		if (status.equals("PENDING")) {
			title = "PENDING TOPUP TRANSACTION";
		} else if (status.equals("SUCCESS")) {
			title = "TOPUP TRANSACTION SUCCEEDED";
		} else if (status.equals("FAILED")) {
			title = "TOPUP TRANSACTION FAILED";
		}

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
		webhook.setUsername("N3RO BOT");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
		webhook.setTts(false);
		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title).setColor(color)
				.setUrl("https://etherscan.io/tx/" + txHash.toLowerCase())
				.addField("From", "||" + senderPublicAddress + "||", false)
				.addField("To", "||" + receiverPublicAddress + "||", false)
				.addField("Value", "" + this.transactionValueString.toString() + " ETH", true)

				.setFooter(dtf.format(now.now()) + " | @n3robot", null));

		try {
			webhook.execute();
			System.out.println(
					"[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress + "] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + this.senderPublicAddress
					+ "] - Send Webhook Failed: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(10000);
			sendWebhook(status);
		}
	}

	public String getSenderPrivateKey() {
		return this.senderPrivateKey;
	}

	public String getTransactionValue() {
		return this.transactionValueString;
	}

	public String getReceiverPublic() {
		return this.receiverPublicAddress;
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
