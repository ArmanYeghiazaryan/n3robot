package modulesEth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Proxy;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Proxies;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class OpenSeaSniperTask extends Thread {

	private String safeMode;
	private String collectionName;
	private String maxPrice;
	private String traits;
	private String gasPrice;
	private String gasLimit;
	private String privateKey;
	private BigDecimal floor;
	private String userPublicAddress;
	private String alchemyURL;
	private String etherscanApiKey;
	private String webhookURL;

	private String proxy;
	private String ip;
	private int port;
	private String username;
	private String password;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	static HashSet<String> assetsSet = new HashSet<String>();
	private long latestFloorScrape = 0;
	boolean sleep = true;
	private HashMap<String, String> traitsMap = new HashMap<String, String>();

	//// NFT DATA
	private int maxAmount;
	private String id;
	private BigInteger assetPrice;
	private BigInteger assetPriceStarting;
	private BigDecimal assetPriceInEthSingle;
	private BigDecimal assetPriceInEthAll;
	private String assetName;
	private String assetImg;
	private String assetUrl;
	private String assetTokenID;
	private String assetContract;
	private String assetSchemaName;
	private String auctionType;
	private JSONArray finalArray;
	private int amountFound;
	private int taskId;
	////

	public OpenSeaSniperTask(int taskId, String safeMode, String collectionName, String maxAmount, String maxPrice,
			String traits, String gasPrice, String gasLimit, String privateKey, String proxy, String alchemyURL,
			String etherscanApiKey, String webhookURL) {
		System.err.close();

		this.taskId = taskId;
		this.safeMode = safeMode;
		this.collectionName = collectionName;
		this.maxAmount = Integer.valueOf(maxAmount);
		this.maxPrice = maxPrice;
		this.traits = traits;
		this.gasPrice = gasPrice;
		this.gasLimit = gasLimit;
		this.privateKey = privateKey;
		this.alchemyURL = alchemyURL;
		this.etherscanApiKey = etherscanApiKey;
		this.webhookURL = webhookURL;
		this.proxy = proxy;
	}

	public void run() {
		try {
			prepareTask();

			while (true) {
				setProxy();
				monitorAssets();
				Thread.sleep(1500);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void prepareTask() throws InterruptedException {

		System.out.println("[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - [" + taskId
				+ "] - Preparing Task...");

		Web3j web3j = Web3j.build(new HttpService(this.alchemyURL));

		// Get address
		Credentials credentials = Credentials.create(privateKey);
		this.userPublicAddress = credentials.getAddress();

		// set traits
		if (!this.traits.equals("")) {
			String arr[] = traits.split(Pattern.quote(";"));

			for (String entry : arr) {
				String entryArray[] = entry.split("=");
				String trait = entryArray[0];
				String value = entryArray[1];

				traitsMap.put(trait.toLowerCase(), value.toLowerCase());
			}

		}

	}

	public void getFloor() throws Exception {
		try {

			System.out.println(ANSI_GREY + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - Getting Floor..." + ANSI_RESET);

			Config config = new Config().connectTimeout(30_000);

			if (this.ip != null) {
				config.proxy(new Proxy(this.ip, this.port, this.username, password));
			}

			UnirestInstance unirest = new UnirestInstance(config);

			HttpResponse<String> response = unirest
					.get("https://api.opensea.io/api/v1/collection/" + collectionName + "/stats")
					.header("accept", "application/json").asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

//				setProxy();

				Thread.sleep(1000);
				throw new Exception("GET_FLOOR_FAILED_" + response.getStatus());

			}

			this.floor = new JSONObject(new JSONTokener(responseBody)).getJSONObject("stats")
					.getBigDecimal("floor_price");
			this.latestFloorScrape = Instant.now().getEpochSecond();

			System.out.println(ANSI_GREY + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - Current Floor: " + floor + " ETH" + ANSI_RESET);

		} catch (Exception e) {

			if (e.toString().contains("JSONObject[\"floor_price\"] is not a BigDecimal (null).")) {
				this.floor = BigDecimal.valueOf(0);
			} else {

				System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
						+ "] - [" + taskId + "] - Request Error: " + e.getMessage() + ANSI_RESET);
				Thread.sleep(500);
				getFloor();
			}
		}
	}

	public void monitorAssets() throws Exception {

		long currentTime = Instant.now().getEpochSecond();

		if (this.latestFloorScrape == 0 || (currentTime - this.latestFloorScrape) > 10) {
			getFloor();
		}

		try {

			System.out.println("[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - [" + taskId
					+ "] - Monitoring Assets...");

//			request.put("X-API-KEY", "7ff0746b048a4f848e2ecadb55b7ae5c"); // bought

			Config config = new Config().connectTimeout(30_000);

			if (this.ip != null) {
				config.proxy(new Proxy(this.ip, this.port, this.username, password));
			}

			UnirestInstance unirest = new UnirestInstance(config);

			HttpResponse<String> response = unirest
					.get("https://api.opensea.io/api/v1/events?only_opensea=true&collection_slug=" + collectionName
							+ "&event_type=created&occurred_after=" + this.latestFloorScrape)
					.header("accept", "application/json").header("X-API-KEY", "2f6f419a083c46de9d83ce3dbe7db601")
					.asString();

			String responseBody = response.getBody();

			if (response.getStatus() != 200) {

//				setProxy();

				System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
						+ "] - [" + taskId + "] - MONITOR ASSETS FAILED - " + response.getStatus() + " - Retrying..."
						+ ANSI_RESET);
				Thread.sleep(1000);

			} else {

				JSONArray a = new JSONObject(new JSONTokener(responseBody)).getJSONArray("asset_events");
				this.finalArray = new JSONArray();
				this.assetPriceInEthAll = new BigDecimal(0);
				boolean found = false;
				this.amountFound = 0;
				unirest.close();
				// Get each listing
				for (int i = 0; i < a.length(); i++) {

					if (amountFound >= this.maxAmount) {
						break;
					}

					try {
						JSONObject o = new JSONObject(new JSONTokener(a.get(i).toString()));
						JSONObject entry = new JSONObject();

						this.id = String.valueOf(o.getInt("id"));
						this.auctionType = o.getString("auction_type");
						this.assetPrice = new BigInteger(o.getString("ending_price"));
						this.assetPriceStarting = new BigInteger(o.getString("starting_price"));

						this.assetName = o.getJSONObject("asset").getString("name");
						this.assetImg = o.getJSONObject("asset").getString("image_url");
						this.assetUrl = o.getJSONObject("asset").getString("permalink");
						this.assetTokenID = o.getJSONObject("asset").getString("token_id");
						this.assetContract = o.getJSONObject("asset").getJSONObject("asset_contract")
								.getString("address");
						this.assetSchemaName = o.getJSONObject("asset").getJSONObject("asset_contract")
								.getString("schema_name");
						this.assetPriceInEthSingle = new BigDecimal(
								Convert.fromWei(assetPrice.toString(), Unit.ETHER).toString());

						if (auctionType.equals("dutch") && this.assetPrice.equals(this.assetPriceStarting)
								&& (assetPrice.compareTo(getTargetPriceInWei()) == -1
										|| assetPrice.compareTo(getTargetPriceInWei()) == 0)
								&& hasCorrectTraits() && !assetSetContains(id)) {

							System.out.println(ANSI_YELLOW + "[OPENSEA] - [" + dtf.format(now.now()) + "] - ["
									+ collectionName + "] - Asset Found: " + assetName + " - "
									+ assetPriceInEthSingle.toString() + " ETH" + ANSI_RESET);

							this.assetPriceInEthAll = assetPriceInEthAll.add(assetPriceInEthSingle);
							// add to finalArray
							entry.put("standard", this.assetSchemaName);
							entry.put("address", this.assetContract);
							entry.put("tokenId", this.assetTokenID);
							entry.put("amount", 1);
							finalArray.put(entry);
							// .......
							found = true;
							amountFound++;

						}

					} catch (Exception e) {
//					e.printStackTrace();
//					System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
//							+ "] - OS Asset Error: " + e.getMessage() + ANSI_RESET);
//					write(a.get(i).toString());
//					Thread.sleep(1000000);
					}

				}

				if (found) {

					getTransactionDataAndSubmit();
				}
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - Request Error OS: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(1000);
		}

	}

	public boolean hasCorrectTraits() throws Exception {

		if (!this.traitsMap.isEmpty()) {
			try {
//
//				System.out.println(ANSI_GREY + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
//						+ "] - Checking Traits..." + ANSI_RESET);

				Session session = Requests.session();

				Map<String, Object> request = new HashMap<>();
				request.put("Accept", "application/json");
				request.put("X-API-KEY", "7ff0746b048a4f848e2ecadb55b7ae5c");

				RawResponse newSession = session
						.get("https://api.opensea.io/api/v1/assets?token_ids=" + this.assetTokenID + "&collection_slug="
								+ this.collectionName + "&order_direction=desc&limit=1")
						.socksTimeout(60_000).connectTimeout(60_000).headers(request).send();

				String response = newSession.readToText();

				if (newSession.statusCode() != 200) {

					System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
							+ "] - CHECK TRAITS FAILED - " + newSession.statusCode() + " - Retrying..." + ANSI_RESET);
					Thread.sleep(700);
					hasCorrectTraits();

				} else {

					JSONObject o = new JSONObject(new JSONTokener(response));
					JSONArray assetTraitsArray = o.getJSONArray("assets").getJSONObject(0).getJSONArray("traits");

					HashMap<String, String> assetTraits = new HashMap<String, String>();

					// Scrape all assets
					for (int i = 0; i < assetTraitsArray.length(); i++) {
						JSONObject trait = new JSONObject(new JSONTokener(assetTraitsArray.get(i).toString()));
						assetTraits.put(String.valueOf(trait.get("trait_type")).toLowerCase(),
								String.valueOf(trait.get("value")).toLowerCase());
					}

					// Confirm all assets
					for (String traitKey : traitsMap.keySet()) {
						String traitValue = traitsMap.get(traitKey);

						// Check if has trait
						if (!assetTraits.keySet().contains(traitKey)) {
							return false;
						}

						String assetKeyValue = assetTraits.get(traitKey);

						// Check if Value is correct
						try {
							if (traitValue.contains("[") && traitValue.contains("]")) {
								// Check range
								traitValue = traitValue.replace("[", "").replace("]", "");
								String arr[] = traitValue.split("-");
								BigDecimal smallestAmount = new BigDecimal(arr[0]);
								BigDecimal highestAmount = new BigDecimal(arr[1]);
								BigDecimal realAmount = new BigDecimal(assetKeyValue);

								if (realAmount.compareTo(smallestAmount) == -1
										|| realAmount.compareTo(highestAmount) == 1) {
									return false;
								}
							} else {

								if (!assetKeyValue.equals(traitValue)) {
									return false;
								}

							}

						} catch (Exception e) {
							return false;
						}
					}

					return true;
				}

			} catch (Exception e) {

				System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
						+ "] - [" + taskId + "] - Request Error: " + e.getMessage() + ANSI_RESET);
				Thread.sleep(500);
			}
			return false;

		} else {
			return true;

		}

	}

	synchronized boolean assetSetContains(String id) {
		boolean contains = this.assetsSet.contains(id);

		if (!contains) {
			this.assetsSet.add(id);
		}

		return contains;
	}

	public void write(String response) throws IOException {
		FileWriter writer = new FileWriter(new File("logs.txt"));
		writer.write(response);
		writer.close();
	}

	public BigInteger getTargetPriceInWei() throws Exception {

		try {
			BigInteger floorInWei = Convert.toWei(this.floor, Convert.Unit.ETHER).toBigInteger();
			BigDecimal floorInWeiDecimal = BigDecimal.valueOf(floorInWei.longValue());

			if (this.maxPrice.toLowerCase().equals("floor")) {
				return floorInWei.subtract(BigInteger.valueOf(1));
			} else if (this.maxPrice.contains("%")) {

				// prozentuale Kosten
				BigDecimal costs = BigDecimal.valueOf(Double.valueOf(maxPrice.replace("%", "").strip()))
						.divide(BigDecimal.valueOf(100));

				return floorInWeiDecimal.subtract(floorInWeiDecimal.multiply(costs)).toBigInteger();
			} else {
				return Convert.toWei(this.maxPrice, Convert.Unit.ETHER).toBigInteger();
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - Failed To Parse MAX_PRICE From CSV..." + ANSI_RESET);

			Thread.sleep(2000);
			getTargetPriceInWei();
			return null;
		}

	}

	public void getTransactionDataAndSubmit() throws Exception {

		String err = "";
		try {

			System.out.println(ANSI_GREY + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - Getting Signature..." + ANSI_RESET);

			Map<String, Object> request = new HashMap<>();
			request.put("Connection", "keep-alive");
			request.put("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"99\", \"Google Chrome\";v=\"99\"");
			request.put("Accept", "application/json");
			request.put("Content-Type", "application/json");
			request.put("Sec-Ch-Ua-Mobile", "?0");
			request.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");
			request.put("Sec-Ch-Ua-Platform", "\"Windows\"");
			request.put("Origin", "https://www.gem.xyz");
			request.put("Sec-Fetch-Site", "cross-site");
			request.put("Sec-Fetch-Mode", "cors");
			request.put("Sec-Fetch-Dest", "empty");
			request.put("Referer", "https://www.gem.xyz/");
			request.put("Accept-Language", "de,en-GB;q=0.9,en;q=0.8,en-US;q=0.7,es;q=0.6,ca;q=0.5");

			Session session = Requests.session();

//			String body = "{\"sender\":\"" + this.userPublicAddress
//					+ "\",\"balanceToken\":\"0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\",\"useFlashbots\":"
//					+ String.valueOf(Boolean.valueOf(this.safeMode)) + ",\"sell\":[],\"buy\":[{\"standard\":\""
//					+ assetSchemaName + "\",\"address\":\"" + assetContract + "\",\"tokenId\":\"" + assetTokenID
//					+ "\",\"amount\":1}]}";

			String body = "{\"sender\":\"" + this.userPublicAddress
					+ "\",\"balanceToken\":\"0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\",\"useFlashbots\":"
					+ String.valueOf(Boolean.valueOf(this.safeMode)) + ",\"sell\":[],\"buy\":"
					+ this.finalArray.toString() + "}";

			RawResponse newSession = session.post("https://gem-route-api.herokuapp.com/route").headers(request)
					.body(body).socksTimeout(60_000).connectTimeout(60_000).send();

			String response = newSession.readToText();

			if (newSession.statusCode() != 200) {

				System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName
						+ "] - GET SIGNATURE FAILED - " + newSession.statusCode() + " - Retrying..." + ANSI_RESET);
				Thread.sleep(1000);
				monitorAssets();

			} else {
				err = response;

				JSONObject o = new JSONObject(new JSONTokener(response));
				String targetContract = o.getString("contractAddress");
				String targetHexData = o.getString("transaction");

				// send transaction
				EthMintingTask task = new EthMintingTask(this.taskId, assetName, assetUrl, assetImg,
						this.floor.toString(), assetPriceInEthAll.toString(), String.valueOf(this.amountFound), "false",
						this.privateKey, targetContract, targetHexData, assetPriceInEthAll.toString(), this.gasPrice,
						gasLimit, "", null, this.alchemyURL, etherscanApiKey, webhookURL);
				task.start();

				while (!task.getIsFinished()) {
					Thread.sleep(1000);
				}
			}

		} catch (Exception e) {

			String error = "";
			if (e.toString().contains("JSONObject[\"transaction\"] is not a string.")) {
				error = "ASSET MAY NOT BE ON SALE";
			} else {
				error = e.getMessage();
			}

			System.out.println(ANSI_RED + "[OPENSEA] - [" + dtf.format(now.now()) + "] - [" + collectionName + "] - ["
					+ taskId + "] - GET SIGNATURE FAILED: " + error + ANSI_RESET);
		}

	}

	public void setProxy() throws Exception {
		if (this.proxy.toLowerCase().equals("true")) {
			String[] p = getProxy().split(":");
			this.ip = p[0];
			this.port = Integer.valueOf(p[1]);
			this.username = p[2];
			this.password = p[3];

		}
	}

	public String getProxy() throws Exception {
		List<String> proxies = new ArrayList<String>();

		// GENERATE RANDOM PROXY
		File file = new File(System.getProperty("user.dir") + "\\tasks\\eth\\proxiesOpenSea.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				proxies.add(line);
			}
		}

		int randomNum = ThreadLocalRandom.current().nextInt(0, proxies.size());

		return proxies.get(randomNum);

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
