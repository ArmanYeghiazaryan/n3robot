package modulesSol;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.text.AttributeSet.ColorAttribute;

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

import main.DiscordWebhook;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class SolanaTools extends Thread {
	private String webhookUrl;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public SolanaTools(String webhookUrl) {

		this.webhookUrl = webhookUrl;
	}

	public void run(Map<String, String> map, String publicKey) {
		validateCandyMachineForURLMint(map, publicKey);
	}

	public void generateSolanaWallet() throws InterruptedException, IOException {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Generating Wallet...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");

		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/wallet/generate/private_key")
				.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			generateSolanaWallet();

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		String privateKey = object.getString("b58_private_key").replace("\"", "").strip();
		String publicKey = getWalletPublickey(privateKey);

		FileWriter credentialsFile = new FileWriter(
				System.getProperty("user.home") + "\\Desktop\\ZETA Bot\\wallets\\solanaWallets.csv", true);
		CSVWriter writer = new CSVWriter(credentialsFile);
		String[] data1 = { "", publicKey, privateKey };
		writer.writeNext(data1);
		writer.close();
		System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Generated Wallet." + ANSI_RESET);

	}

	public String getWalletPublickey(String privateKey) throws InterruptedException {

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		String body = "{\n" + "\"wallet\": {\n" + "\"b58_private_key\": \"" + privateKey + "\",\n"
				+ "\"derivation_path\": \"m/44/501/n/0\",\n" + "\"passphrase\": \"\"\n" + "}\n" + "}";

		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/wallet/public_key")
				.headers(request).body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);

			Thread.sleep(1000);
			getWalletPublickey(privateKey);

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		String publicKey = object.getString("public_key").replace("\"", "").strip();

		return publicKey;
	}

	public HashSet<String> getWalletNFTs(String publicKey) throws InterruptedException {
		List<String> list = Collections.<String>emptyList();

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		RawResponse newSession = session
				.get("https://api.blockchainapi.com/v1/solana/wallet/mainnet-beta/" + publicKey + "/nfts")
				.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);

			Thread.sleep(2500);
			getWalletNFTs(publicKey);

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		List<Object> nfts = object.getJSONArray("nfts_owned").toList();

		HashSet<String> map = new HashSet<String>();

		for (Object nftObject : nfts) {
			String nft = (String) nftObject;
			map.add(nft.replace("\"", "").strip());
		}

		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Found " + map.size() + " NFTs...");

		return map;

	}

	public void transferNFT(String privateKey, String receiver, String tokenAddress)
			throws IOException, LineUnavailableException, InterruptedException {

		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Transferring NFT '" + tokenAddress + "' to '"
				+ receiver + "'...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		String body = "{\n" + "  \"wallet\": {\n" + "    \"b58_private_key\": \"" + privateKey + "\"\n" + "  },\n"
				+ "  \"recipient_address\": \"" + receiver + "\",\n" + "  \"network\": \"mainnet-beta\",\n"
				+ "  \"amount\": \"1\",\n" + "  \"token_address\": \"" + tokenAddress + "\"\n" + "}";

		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/wallet/transfer")
				.headers(request).body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (!response.contains("You need `1.0` of token")) {
			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
						+ newSession.statusCode() + ANSI_RESET);
				Thread.sleep(2500);
				transferNFT(privateKey, receiver, tokenAddress);

			}

			JSONTokener tokener = new JSONTokener(response);
			JSONObject object = new JSONObject(tokener);
			String transactionSignature = object.getString("transaction_signature").replace("\"", "").strip();

			System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Sent '" + tokenAddress
					+ "' To '" + receiver + "'." + ANSI_RESET);

			sendWebhook("TRANSFERRED SOL NFT", privateKey, receiver, "1", transactionSignature);
		} else {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Could Not Find Provided NFT..."
					+ ANSI_RESET);
		}

	}

	public void transferSol(String privateKey, String receiverPublic, String amount)
			throws InterruptedException, IOException, LineUnavailableException {

		System.out.println(
				"[SOLANA] - [" + dtf.format(now.now()) + "] - Transferring SOL to '" + receiverPublic + "'...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		if (amount.toUpperCase().equals("ALL")) {
			amount = getSolBalance(getWalletPublickey(privateKey));
		}

//		String body = "{\n" + "\"b58_private_key\": \"" + privateKey + "\",\n" + "\"passphrase\": \"\",\n"
//				+ "\"recipient_address\": \"" + receiverPublic + "\",\n" + "\"network\": \"mainnet-beta\",\n"
//				+ "\"amount\": \"" + amount + "\"\n" + "}";

		String body = "{\n" + "  \"wallet\": {\n" + "    \"b58_private_key\": \"" + privateKey + "\",\n"
				+ "    \"passphrase\": \"\"\n" + "  },\n" + "  \"recipient_address\": \"" + receiverPublic + "\",\n"
				+ "  \"network\": \"mainnet-beta\",\n" + "  \"amount\": \"" + amount + "\"\n" + "}";

		System.out.println(body);
		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/wallet/transfer")
				.headers(request).body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);

			Thread.sleep(1000);
			transferSol(privateKey, receiverPublic, amount);

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		String transactionSignature = object.getString("transaction_signature").replace("\"", "").strip();

		System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Sent " + amount + "SOL To '"
				+ receiverPublic + "'." + ANSI_RESET);

		sendWebhook("PENDING SOL TRANSFER", privateKey, receiverPublic, amount, transactionSignature);

	}

	public String getSolBalance(String publicAddress) throws InterruptedException {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Checking Wallet Balance...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		String body = "{\n" + "  \"public_key\": \"" + publicAddress + "\",\n" + "  \"unit\": \"sol\",\n"
				+ "  \"network\": \"mainnet-beta\"\n" + "}";

		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/wallet/balance").headers(request)
				.body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);

			Thread.sleep(1000);
			getSolBalance(publicAddress);

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		String walletBalance = String.valueOf(object.getFloat("balance"));

		System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - Wallet Contains " + walletBalance
				+ " SOL." + ANSI_RESET);
		return walletBalance;
	}

	public String getCandyMachineVersion(String publicKey) throws Exception {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Checking Candy Machine...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		RawResponse newSession = session
				.get("https://api.blockchainapi.com/v1/solana/account/mainnet-beta/" + publicKey + "/is_candy_machine")
				.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (response.contains("is_candy_machine\":false") || response.contains("error")) {
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Provided Key Is Invalid...");
			throw new Exception("INVALID_CANDY_MACHINE");
		}

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			getCandyMachineVersion(publicKey);

		}

		JSONTokener tokener = new JSONTokener(response);
		JSONObject object = new JSONObject(tokener);
		String candyMachineVersion = object.getString("candy_machine_contract_version").replace("\"", "");

		return candyMachineVersion;
	}

	public String getCandyMachineMetadata(String publicKey, String version) throws Exception {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Getting Candy Machine Details...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		String body = "{\n" + "  \"candy_machine_id\": \"" + publicKey + "\",\n" + "  \"network\": \"mainnet-beta\",\n"
				+ "  \"candy_machine_contract_version\": \"" + version + "\"\n" + "}";

		RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/nft/candy_machine/metadata")
				.headers(request).body(body).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (response.contains("is_candy_machine\":false") || response.contains("error")) {
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Provided Key Is Invalid...");
			throw new Exception("INVALID_CANDY_MACHINE");
		}

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			getCandyMachineMetadata(publicKey, version);

		}

		return response;

	}

	public void validateCandyMachineForURLMint(Map<String, String> map, String publicKey) {
		try {
			String version = getCandyMachineVersion(publicKey);
			String metaData = getCandyMachineMetadata(publicKey, version);

			JSONTokener tokener = new JSONTokener(metaData);
			JSONObject object = new JSONObject(tokener).accumulate("versionNumber", version);

			map.put(publicKey, object.toString());
		} catch (Exception e) {
			map.put(publicKey, "INVALID");
		}
	}

	public boolean checkTransaction(String txSignature) throws InterruptedException {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Polling TX Status In 45s...");

		Thread.sleep(45000);

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		RawResponse newSession = session.get("https://api.blockchainapi.com/v1/solana/account/mainnet-beta/")
				.headers(request).socksTimeout(60_000).connectTimeout(60_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			checkTransaction(txSignature);

		}

		if (response.contains("meta': {'err': None")) {
			return true;
		} else {
			return false;
		}

	}

	public void sendWebhook(String title, String seedPhrase, String receiver, String amount, String txHash)
			throws IOException, LineUnavailableException, InterruptedException {

		Color color = null;

		if (title.equals("PENDING SOL TRANSFER")) {
			color = Color.orange;
		} else {
			color = new Color(43, 46, 58);

		}

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
		webhook.setUsername("ZETA AIO");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/839821906881806357/957787704156905482/zeta_logo_square.png");
		webhook.setTts(false);
		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title).setColor(color)

				.addField("From", "||" + seedPhrase + "||", false).addField("To", "||" + receiver + "||", false)
				.addField("Value", amount, true).addField("Hash", "||https://solscan.io/tx/" + txHash + "||", false)

				.setFooter(dtf.format(now.now()) + " | @zeta_aio", null));

		try {
			webhook.execute();
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Send Webhook Failed: " + e.getMessage());
			Thread.sleep(10000);
			sendWebhook(title, seedPhrase, receiver, amount, txHash);

		}
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
}
