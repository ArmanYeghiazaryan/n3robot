package modulesSol;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import com.google.common.base.Stopwatch;

import main.BackendWebhook;
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

public class SolanaMESniperTask extends Thread {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	private String privateKey;
	private String collectionName;
	private String webhookUrl;
	private String price;
	private BigInteger floor;
	private BigInteger snipingPrice;
	private String floorParsed;
	private String snipingPriceParsed;
	private long latestScrape;
	private HashSet<String> set = new HashSet<String>();
	private boolean snipe;

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public SolanaMESniperTask(String privateKey, String collectionName, String price, String webhookUrl,
			boolean snipe) {

		this.privateKey = privateKey;
		this.collectionName = collectionName;
		this.price = price;
		this.webhookUrl = webhookUrl;
		this.snipe = snipe;

	}

	public void run() {
		try {
			prepareTask();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while (true) {
			try {
				monitorEntries();
				Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public void prepareTask() throws Exception {

	}

	public void getFloorFromAPI() throws Exception {
		Session session = Requests.session();

		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
				+ snipingPriceParsed + " ] - Getting Collection Floor...");

		RawResponse newSession = session
				.get("https://api-mainnet.magiceden.io/rpc/getCollectionEscrowStats/" + collectionName)
				.socksTimeout(30_000).connectTimeout(30_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
					+ " | Target: " + snipingPriceParsed + " ] - Retrying: " + newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			getFloorFromAPI();

		}

		JSONObject o = new JSONObject(new JSONTokener(response));
		this.floor = BigInteger.valueOf(o.getJSONObject("results").getLong("floorPrice"));

		if (price.toLowerCase().contains("floor")) {
			this.snipingPrice = BigInteger.valueOf(o.getJSONObject("results").getLong("floorPrice"));

		} else if (price.contains("%")) {

			BigDecimal anteil = BigDecimal.valueOf(1).subtract(
					BigDecimal.valueOf(Double.valueOf(price.replace("%", "").strip())).divide(BigDecimal.valueOf(100)));
			this.snipingPrice = BigDecimal.valueOf(o.getJSONObject("results").getLong("floorPrice")).multiply(anteil)
					.toBigInteger();
		} else {
			this.snipingPrice = BigDecimal.valueOf(Double.valueOf(price)).multiply(BigDecimal.valueOf(1000000000))
					.toBigInteger();
		}

		this.latestScrape = Instant.now().getEpochSecond();
		this.floorParsed = BigDecimal.valueOf(floor.longValue()).divide(BigDecimal.valueOf(1000000000)).toString();
		this.snipingPriceParsed = BigDecimal.valueOf(snipingPrice.longValue()).divide(BigDecimal.valueOf(1000000000))
				.toString();

	}

	public void monitorEntries() throws Exception {
		// Request floor if 0.5 mins after
		if (Instant.now().getEpochSecond() > latestScrape + 30) {
			getFloorFromAPI();

		}

		Session session = Requests.session();

		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
				+ snipingPriceParsed + " ] - Getting Listed NFTs...");

		RawResponse newSession = session.get(
				"https://api-mainnet.magiceden.io/rpc/getListedNFTsByQuery?q=%7B%22%24match%22%3A%7B%22collectionSymbol%22%3A%22"
						+ collectionName
						+ "%22%7D%2C%22%24sort%22%3A%7B%22takerAmount%22%3A1%2C%22createdAt%22%3A-1%7D%2C%22%24skip%22%3A0%2C%22%24limit%22%3A20%7D")
				.socksTimeout(30_000).connectTimeout(30_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200) {

			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
					+ " | Target: " + snipingPriceParsed + " ] - Retrying..." + newSession.statusCode() + ANSI_RESET);
			Thread.sleep(1000);
			monitorEntries();

		}

		JSONObject o = new JSONObject(new JSONTokener(response));
		JSONArray array = o.getJSONArray("results");

		for (int i = 0; i < array.length(); i++) {
			JSONObject currentEntry = new JSONObject(new JSONTokener(array.get(i).toString()));

			BigInteger entryPrice = BigDecimal.valueOf(currentEntry.getDouble("price"))
					.multiply(BigDecimal.valueOf(1000000000)).toBigInteger();

			// if less than that
			if (entryPrice.longValue() <= snipingPrice.longValue()) {

				String id = currentEntry.getString("id");
				String img = currentEntry.getString("img");
				String title = currentEntry.getString("title");
				String mintAddress = currentEntry.getString("mintAddress");
				double sellerFees = currentEntry.getDouble("sellerFeeBasisPoints");

				StringBuilder sb = new StringBuilder();
				// Get properties
				for (int j = 0; j < currentEntry.getJSONArray("attributes").length(); j++) {
					JSONObject currentProperty = new JSONObject(
							new JSONTokener(currentEntry.getJSONArray("attributes").get(j).toString()));
					sb.append(currentProperty.get("trait_type").toString().toUpperCase() + ": "
							+ currentProperty.get("trait_type") + "\\n");
				}

				if (!set.contains(id + entryPrice)) {

					System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
							+ " | Target: " + snipingPriceParsed + " ] - ENTRY FOUND: " + title + " - "
							+ currentEntry.getDouble("price") + "SOL");

					if (snipe) {
						snipeNFT(title, img, mintAddress, entryPrice.toString());

					} else {
						// Monitor Webhook
						sendMonitorWebhook(title, String.valueOf(currentEntry.getDouble("price")), img, mintAddress,
								sb.toString());
					}
					set.add(id + entryPrice);

				}

			}
		}

	}

	public void snipeNFT(String title, String imageUrl, String mintAddress, String price) throws Exception {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
				+ snipingPriceParsed + " ] - Submitting Transaction...");

		Session session = Requests.session();

		Map<String, Object> request = new HashMap<>();
		request.put("APIKeyID", "nVeSwSsiTI6RUf3");
		request.put("APISecretKey", "kAnoOehZPrP0VEV");
		request.put("Content-Type", "application/json");

		String body = "{\n" + "  \"wallet\": {\n" + "    \"b58_private_key\": \"" + privateKey + "\",\n"
				+ "    \"derivation_path\": \"m/44/501/0/0\",\n" + "    \"passphrase\": \"\"\n" + "  },\n"
				+ "  \"nft_price\": " + price + "\n" + "}";

		RawResponse newSession = session.post(
				"https://api.blockchainapi.com/v1/solana/nft/marketplaces/magic-eden/buy/mainnet-beta/" + mintAddress)
				.headers(request).body(body).socksTimeout(180_000).connectTimeout(180_000).send();

		String response = newSession.readToText();

		if (newSession.statusCode() != 200 || response.contains("err")) {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
					+ " | Target: " + snipingPriceParsed + " ] - Failed To Submit: " + response + ANSI_RESET);

		} else {
			JSONTokener tokener = new JSONTokener(response);
			JSONObject object = new JSONObject(tokener);
			String transactionSignature = object.getString("transaction_signature").replace("\"", "").strip();

			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
					+ snipingPriceParsed + " ] - Submitted Transaction: " + transactionSignature);

			checkTransactionStatus(title, price, imageUrl, transactionSignature);
		}

	}

	public void checkTransactionStatus(String title, String price, String imageUrl, String tx) throws Exception {
		System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
				+ snipingPriceParsed + " ] - Polling Transaction Status...");
		try {
			Session session = Requests.session();
			RawResponse newSession = session.get("https://public-api.solscan.io/transaction/" + tx).socksTimeout(60_000)
					.connectTimeout(60_000).send();

			String response = newSession.readToText();

			JSONTokener tokener = new JSONTokener(response);
			JSONObject object = new JSONObject(tokener);
			String status = object.getString("status").replace("\"", "");

			if (status.equals("Success") || status.equals("success")) {

				System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
						+ " | Target: " + snipingPriceParsed + " ] - TRANSACTION SUCCEEDED!" + ANSI_RESET);
				// send webhook
				BackendWebhook b = new BackendWebhook("Magic-Eden Snipe",
						"**Collection:** " + collectionName + "\\n**Floor:** " + floorParsed + " SOL\\n**Target:** "
								+ snipingPriceParsed + " SOL",
						null,
						"https://discord.com/api/webhooks/951536933664870502/dwZ3guOyZFo3Bi1EXTfm-n3mbIWf-iY2EpZE53YEXo6Wx5FezNtA3gI4JtyUsB3vemHn");
				sendWebhook(title, price, imageUrl, tx);

			} else if (status.equals("Fail") || status.equals("fail")) {
				System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
						+ " | Target: " + snipingPriceParsed + " ] - TRANSACTION FAILED!" + ANSI_RESET);

			}
		} catch (Exception e) {
			Thread.sleep(3000);
			checkTransactionStatus(title, price, imageUrl, tx);
		}

	}

	public void sendWebhook(String title, String price, String imageUrl, String tx) throws Exception {

		price = BigDecimal.valueOf(Long.valueOf(price)).divide(BigDecimal.valueOf((long) 1000000000)).toString();

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

		webhook.setUsername("ZETA AIO");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/839821906881806357/957787704156905482/zeta_logo_square.png");
		webhook.setTts(false);

		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("SUCCESSFULLY SNIPED SOLANA NFT")
				.setThumbnail(imageUrl).addField("Market", "||Magic-Eden||", false)
				.setUrl("https://public-api.solscan.io/transaction/" + tx).addField("NFT", "||" + title + "||", false)
				.addField("Price", "||" + price + " SOL||", true).addField("Floor", "||" + floorParsed + " SOL||", true)
				.addField("Wallet", "||" + privateKey + "||", false)
				.setFooter(dtf.format(now.now()) + " | @zeta_aio", null).setColor(new Color(43, 46, 58)));

		try {
			webhook.execute();
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
					+ snipingPriceParsed + " ] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
					+ snipingPriceParsed + " ] - Send Webhook Failed: " + e.getMessage());
			Thread.sleep(10000);
			sendWebhook(title, price, imageUrl, tx);

		}
	}

	public void sendMonitorWebhook(String title, String price, String imageUrl, String mintAddress, String traits)
			throws Exception {

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

		webhook.setUsername("ZETA AIO");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/839821906881806357/957787704156905482/zeta_logo_square.png");
		webhook.setTts(false);

		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title + " LISTED")
				.setUrl("https://www.magiceden.io/item-details/" + mintAddress).setThumbnail(imageUrl)
				.addField("Market", "[Magic-Eden](https://www.magiceden.io/marketplace/" + collectionName + ")", true)
				.addField("Price", price + " SOL", true).addField("Floor", floorParsed + " SOL", true)
				.addField("Traits", "```" + traits + "```", false)
				.setFooter(dtf.format(now.now()) + " | @zeta_aio", null).setColor(new Color(43, 46, 58)));

		try {
			webhook.execute();
			System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed
					+ " | Target: " + snipingPriceParsed + " ] - Sent Webhook." + ANSI_RESET);

		} catch (Exception e) {

			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - [Floor: " + floorParsed + " | Target: "
					+ snipingPriceParsed + " ] - Send Webhook Failed: " + e.getMessage());
			Thread.sleep(10000);
			sendMonitorWebhook(title, price, imageUrl, mintAddress, traits);

		}
	}

	public void write(String response) throws Exception {
		FileWriter writer = new FileWriter(new File("logs.txt"));
		writer.write(response);
		writer.close();
	}

}
