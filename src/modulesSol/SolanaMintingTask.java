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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Stopwatch;

import main.DiscordWebhook;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

/*
 * add try and catch to methods to retry and avoid run... errors
 * 
 * switch to sendTransaction 
 * https://ethereum.stackexchange.com/questions/72889/how-to-create-a-rawtransaction-for-contract-interaction-web3j
 */

public class SolanaMintingTask extends Thread {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	private String seedPhrase;
	private String configAddress;
	private String candyMachineVersion;
	private long dropTimeStamp;
	private Map<String, String> map;
	private String publicKey;
	private String webhookUrl;
	private String mode;
	private String price;
	private SolanaTxTask solanaTxTask;

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public SolanaMintingTask(String seedPhrase, String configAddress, String candyMachineVersion, long dropTimeStamp,
			Map<String, String> map, String publicKey, String webhookUrl, String mode, String price) {

		this.seedPhrase = seedPhrase;
		this.configAddress = configAddress;
		this.candyMachineVersion = candyMachineVersion;
		this.dropTimeStamp = dropTimeStamp;
		this.map = map;
		this.publicKey = publicKey;
		this.mode = mode;
		this.price = price;
		this.webhookUrl = webhookUrl;

	}

	public void run() {

		try {

			if (mode.equals("validateCandyMachineForURLMint")) {
				validateCandyMachineForURLMint(map, publicKey);
			}

			if (mode.equals("MINTING")) {
				this.solanaTxTask = new SolanaTxTask(seedPhrase, publicKey, price, webhookUrl);
//				this.solanaTxTask.start();
				mintNFT(seedPhrase, configAddress, candyMachineVersion, dropTimeStamp);
			}

		} catch (Exception e) {
			System.out.println(
					ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: " + e.toString() + ANSI_RESET);
			try {
				write(e.toString());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
			}

		}

	}

	public void write(String response) throws Exception {
		FileWriter writer = new FileWriter(new File("logs.txt"));
		writer.write(response);
		writer.close();
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
				.headers(request).socksTimeout(30_000).connectTimeout(30_000).send();

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
				.headers(request).body(body).socksTimeout(30_000).connectTimeout(30_000).send();

		String response = newSession.readToText();

		if (response.contains("is_candy_machine\":false") || response.contains("error")) {
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Provided Key Is Invalid...");
			throw new Exception("INVALID_CANDY_MACHINE");
		}

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
					+ newSession.statusCode() + ANSI_RED);
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

	public void mintNFT(String secretKey, String configAddress, String candyMachineVersion, long dropTimeStamp)
			throws InterruptedException, IOException, LineUnavailableException {

		try {
			long currentTimeStamp = Instant.now().getEpochSecond();

			if (currentTimeStamp < dropTimeStamp) {
				long difference = dropTimeStamp - currentTimeStamp;

				if (difference > 3) {
					difference = difference - 3;
				}

				System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Minting Not Live Yet. Waiting "
						+ difference + "s...");
				Thread.sleep(difference * 1000);
				mintNFT(secretKey, configAddress, candyMachineVersion, dropTimeStamp);
			}

			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Submitting Transaction...");

			Session session = Requests.session();

			Map<String, Object> request = new HashMap<>();
			request.put("APIKeyID", "nVeSwSsiTI6RUf3");
			request.put("APISecretKey", "kAnoOehZPrP0VEV");
			request.put("Content-Type", "application/json");

			String body = "{\n" + "  \"wallet\": {\n" + "    \"b58_private_key\": \"" + secretKey + "\",\n"
					+ "    \"derivation_path\": \"m/44/501/n/0\",\n" + "    \"passphrase\": \"\"\n" + "  },\n"
					+ "  \"network\": \"mainnet-beta\",\n" + "  \"config_address\": \"" + configAddress + "\",\n"
					+ "  \"candy_machine_contract_version\": \"" + candyMachineVersion + "\"\n" + "}";

			RawResponse newSession = session.post("https://api.blockchainapi.com/v1/solana/nft/candy_machine/mint")
					.headers(request).body(body).socksTimeout(20_000).connectTimeout(20_000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200 || response.contains("err")) {
				System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: "
						+ response.strip() + ANSI_RESET);
				Thread.sleep(500);
				mintNFT(secretKey, configAddress, candyMachineVersion, dropTimeStamp);

			}

			JSONTokener tokener = new JSONTokener(response);
			JSONObject object = new JSONObject(tokener);
			String transactionSignature = object.getString("transaction_signature").replace("\"", "").strip();

			System.out.println(
					"[SOLANA] - [" + dtf.format(now.now()) + "] - Submitted Transaction: " + transactionSignature);

//			solanaTxTask.addTx(transactionSignature);

			// Submit again
			Thread.sleep(500);
			mintNFT(secretKey, configAddress, candyMachineVersion, dropTimeStamp);
		} catch (Exception e) {

			String error = "";

			if (e.toString().toLowerCase().contains("read timed out")) {
				error = "TIMEOUT_EXCEPTION / SOLANA NETWORK SLOW";
			} else {
				error = e.getMessage();
			}

			System.out.println(
					ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Retrying: " + e.getMessage() + ANSI_RED);
			mintNFT(secretKey, configAddress, candyMachineVersion, dropTimeStamp);
		}

	}

}
