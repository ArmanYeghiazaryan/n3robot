package modulesEth;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

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
import org.web3j.utils.Convert;

import com.opencsv.CSVWriter;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.ReCaptcha;

import main.DiscordWebhook;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class CustomSignature extends Thread {

	private String wallet;
	private String sig;
	private boolean isFinished;

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public CustomSignature(String wallet) {

		this.wallet = wallet.strip();

	}

	public void run() {

		try {
			genSig();
		} catch (Exception e) {
			System.out.println(
					"[ETHER] - [" + dtf.format(now.now()) + "] - [" + wallet + "] - Task Failed: " + e.getMessage());

		}
	}

	public void genSig() throws Exception {

		Session session = Requests.session();
		System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + wallet + "] - Generating Signature...");

		Map<String, Object> request = new HashMap<>();
		request.put("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51 Safari/537.36");

		RawResponse newSession = session.post("https://mint.imaginaryones.com/common/generate-signature")
				.headers(request).body("{\"address\": \"" + this.wallet + "\"}").socksTimeout(60_000)
				.connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + wallet + "] -  Error: "
					+ newSession.statusCode());

			Thread.sleep(1000);
			genSig();

		} else {
			JSONObject o = new JSONObject(new JSONTokener(response));

			this.sig = o.getJSONObject("data").getString("signature");
			System.out.println(ANSI_GREEN + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + wallet
					+ "] - SUCCESS: Check customSignatures.csv!" + ANSI_RESET);
			addToFile();
			this.isFinished = true;

		}
	}

	synchronized void addToFile() throws Exception {
		FileWriter writer = new FileWriter(
				System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\tasks\\eth\\customSignatures.csv", true);
		writer.write("\nimaginaryones," + this.wallet + "," + this.sig);
		writer.close();
	}

	public boolean isFinished() {
		return this.isFinished;
	}

//	public void sendWebhook(String status) throws IOException, LineUnavailableException, InterruptedException {
//		Color color = null;
//
//		if (status.equals("PENDING")) {
//			color = Color.orange;
//		} else if (status.equals("SUCCESS")) {
//			color = Color.green;
//		} else if (status.equals("FAILED")) {
//			color = Color.red;
//		}
//
//		String title = "";
//		if (status.equals("PENDING")) {
//			title = "PENDING TOPUP TRANSACTION";
//		} else if (status.equals("SUCCESS")) {
//			title = "TOPUP TRANSACTION SUCCEEDED";
//		} else if (status.equals("FAILED")) {
//			title = "TOPUP TRANSACTION FAILED";
//		}
//
//		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
//		webhook.setUsername("ZETA BOT");
//		webhook.setTts(false);
//		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title).setColor(color)
//
//				.addField("From", "||" + senderPublicAddress + "||", false)
//				.addField("To", "||" + receiverPublicAddress + "||", false)
//				.addField("Value", "**" + transactionValue + "E**", true)
//				.addField("Hash", "||https://etherscan.io/tx/" + txHash + "||", false)
////				.addField("Balances",
////						"||**Sender: "||",
////						false)
//
//				.setFooter("Made With <3 By @ZETABot", null));
//
//		try {
//			webhook.execute();
//			System.out.println(
//					"[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress + "] - Sent Webhook.");
//
//		} catch (Exception e) {
//			if (e.toString().contains("Server returned HTTP response code: 429 for URL")) {
//				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + senderPublicAddress
//						+ "] - Webhook ratelimited! Retrying in 10s..." + ANSI_RESET);
//				Thread.sleep(10000);
//				sendWebhook(status);
//			}
//		}
//	}

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
