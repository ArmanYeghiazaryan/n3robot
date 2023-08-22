package modulesEth;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import com.opencsv.CSVWriter;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.Proxy;
import kong.unirest.UnirestInstance;

public class EthListItemTask extends Thread {

	private int taskId;
	private String secretKey;
	private String ownerAddress;
	private String contract;
	private String price;
	private Web3j web3j;
	private Credentials credentials;
	private String alchemyKeyUrl;
	HashSet<String> tokenSet = new HashSet<String>();

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public EthListItemTask(int taskId, String secretKey, String contract, String price, String alchemyKeyUrl) {
		this.taskId = taskId;
		this.secretKey = secretKey;
		this.contract = contract.toLowerCase();
		this.price = price;
		this.alchemyKeyUrl = alchemyKeyUrl;

	}

	public void run() {
		try {
			prepareTask();
			checkAssets();

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[OS-LISTER] - [" + dtf.format(now.now())
					+ "] - [------------------------------------------] - [" + taskId + "] - Generous Error: "
					+ e.getMessage() + ANSI_RESET);
		}

	}

	public void checkAssets() throws Exception {

		try {
			System.out.println(ANSI_GREY + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
					+ "] - [" + taskId + "] - Getting NFTs..." + ANSI_RESET);

			Config config = new Config().connectTimeout(30_000);

//			if (this.ip != null) {
//				config.proxy(new Proxy(this.ip, this.port, this.username, password));
//			}
			UnirestInstance unirest = new UnirestInstance(config);

			HttpResponse<String> response = unirest
					.get("https://api.opensea.io/api/v1/assets?owner=" + this.ownerAddress
							+ "&order_direction=desc&asset_contract_address=" + this.contract + "&limit=50")
					.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
					.asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

//				setProxy();

				System.out.println(ANSI_RED + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
						+ "] - [" + taskId + "] - CHECK ASSETS FAILED - " + response.getStatus() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);
				throw new Exception("INVALID_RESPONSE");

			} else {

				JSONArray array = new JSONObject(new JSONTokener(responseBody)).getJSONArray("assets");

				if (array.length() == 0) {
					System.out
							.println(ANSI_RED + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
									+ "] - [" + taskId + "] - No NFTs Found! Stopping Task..." + ANSI_RESET);
					throw new Exception("NFTs_MISSING");

				} else {
					System.out.println(
							ANSI_PURPLE + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
									+ "] - [" + taskId + "] - Found " + array.length() + " NFT(s)!" + ANSI_RESET);

					// List NFTs...

					for (int i = 0; i < array.length(); i++) {
						JSONObject o = new JSONObject(new JSONTokener(array.get(i).toString()));
						String name = o.getString("name");
						String tokenID = o.getString("token_id");
						double listingPrice = getPrice();
						System.out.println(ANSI_YELLOW + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - ["
								+ this.ownerAddress + "] - [" + taskId + "] - Listing '" + name + "' For "
								+ listingPrice + "E..." + ANSI_RESET);

						listNft(name, tokenID, listingPrice);
					}

				}

			}
		} catch (Exception e) {
			if (e.toString().contains("NFTs_MISSING")) {
				throw new Exception("TASK_STOPPED");
			} else {
				checkAssets();
			}
		}

	}

	public void listNft(String name, String tokenID, double price) throws Exception {
		try {

			Config config = new Config().connectTimeout(0).socketTimeout(0);

//			if (this.ip != null) {
//				config.proxy(new Proxy(this.ip, this.port, this.username, password));
//			}

			String body = "{\n" + "    \"privateKey\": \"" + this.secretKey + "\",\n" + "    \"rpc_url\": \""
					+ this.alchemyKeyUrl + "\",\n" + "    \"asset\": {\n" + "        \"tokenAddress\": \""
					+ this.contract + "\",\n" + "        \"tokenId\": \"" + tokenID + "\"\n" + "    },\n"
					+ "    \"accountAddress\": \"" + this.ownerAddress + "\",\n" + "    \"startAmount\": " + price
					+ "\n" + "}";

//			System.out.println(body);

			UnirestInstance unirest = new UnirestInstance(config);

			HttpResponse<String> response = unirest.post("https://api.n3robot.io/api/v1/private/sell_item")
					.header("accept", "*/*").header("Content-Type", "application/json")
					.header("Authorization",
							"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2Mjk2NjRiNGQzYzk1NDliMjc1OTBhMWIiLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsImlhdCI6MTY1NDAyMzM2NywiZXhwIjoxNjU0ODg3MzY3fQ.TQA0BFckoeUpmZ3TIyUdroIN0vJKX_CiqXiLmqNTdkg")
					.body(body).asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200 || responseBody.contains("message\":\"Success\"")) {

//				setProxy();

				String error = "UNKNOWN";

				try {
					error = new JSONObject(new JSONTokener(responseBody)).getString("message");
					error = new JSONObject(new JSONTokener(error)).getString("details");
				} catch (Exception e) {

				}

				System.out.println(ANSI_RED + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
						+ "] - [" + taskId + "] - LIST ASSET FAILED - " + response.getStatus() + " - " + error
						+ ANSI_RESET);

			} else {

				System.out.println(ANSI_GREEN + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
						+ "] - [" + taskId + "] - Listed '" + name + "' For " + price + "E Successfully!" + ANSI_RESET);

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[OS-LISTER] - [" + dtf.format(now.now()) + "] - [" + this.ownerAddress
					+ "] - [" + taskId + "] - Task Error: " + e.getMessage() + ANSI_RESET);
		}
	}

	public double getPrice() {
		double listingPrice;
		double maxPrice = new BigDecimal(price.split(Pattern.quote("-"))[1]).doubleValue();
		double minPrice = new BigDecimal(price.split(Pattern.quote("-"))[0]).doubleValue();

		if (maxPrice != minPrice) {
			listingPrice = ThreadLocalRandom.current().nextDouble(minPrice, maxPrice);
			listingPrice = new BigDecimal(listingPrice).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
		} else {
			listingPrice = maxPrice;
		}

		return listingPrice;
	}

	public void prepareTask() {
		System.out.println("[OS-LISTER] - [" + dtf.format(now.now())
				+ "] - [------------------------------------------] - [" + taskId + "] - Preparing Wallet...");

		if (this.alchemyKeyUrl == null || this.alchemyKeyUrl.equals("")) {
			this.alchemyKeyUrl = "http://localhost:8400";

		}
		this.web3j = Web3j.build(new HttpService(alchemyKeyUrl));

		this.credentials = Credentials.create(secretKey);
		this.ownerAddress = credentials.getAddress().toLowerCase();
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
