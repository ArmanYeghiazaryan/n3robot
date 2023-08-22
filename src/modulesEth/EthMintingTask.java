package modulesEth;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert.Unit;

import com.esaulpaugh.headlong.abi.Address;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.google.common.base.Stopwatch;

import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import main.BackendWebhook;
import main.DiscordWebhook;
import modulesOther.CaptchaThreadPremint;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

/*
 * https://abi.hashex.org/
 * https://www.aax.com/en-US
 */

public class EthMintingTask extends Thread {

	private String useSafeMode;
	private String secretKey;
	private String ownerAddress;
	private String contractAddress;
	private String function;
	private String functionParameters;
	private String transactionData;
	private String transactionValue;
	private String transactionValueHex;
	private String txHash;
	private long rapidGas;
	private String amountGasAddedToRapid;
	private String finalGasPrice;
	private String providedGasLimit;
	private String timerString;
	private BigInteger nonce;
	private String alchemyKeyUrl;
	private String webhookUrl;
	private String etherscanApiKey;
	private Web3j web3j;
	private Credentials credentials;
	private long sendingDelay;
	private boolean isFinished;
	private String functionABI;
	private String status = "idle";
	private String elsTicket;
	private String elsSignature;
	private CaptchaThreadPremint captcha;
	private HashSet<String> twocapKeys;

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private String timeStamp;
	private int taskId;

	// OPENSEA SNIPING
	private String assetName;
	private String assetURL;
	private String assetImg;
	private String assetFloor;
	private String assetPrice;
	private String assetsAmount;

	// ------------------------

	public EthMintingTask(int taskId, String useSafeMode, String secretKey, String contractAddress, String function,
			String functionParameters, String transactionData, String transactionValue, String amountGasAddedToRapid,
			String providedGasLimit, String timerString, String customNonce, String alchemyKeyUrl,
			String etherscanApiKey, String webhookUrl, HashSet<String> twocapKeys) {
		System.err.close();

		this.taskId = taskId;
		this.useSafeMode = useSafeMode;
		this.secretKey = secretKey.strip();
		this.contractAddress = contractAddress.strip();
		this.function = function.strip();
		this.functionParameters = functionParameters.strip();
		this.transactionData = transactionData.strip();
		this.transactionValue = transactionValue.strip();
		this.amountGasAddedToRapid = amountGasAddedToRapid.strip();
		this.providedGasLimit = providedGasLimit.strip();
		this.timerString = timerString.strip();
		if (customNonce != null) {
			this.nonce = BigInteger.valueOf(Long.valueOf(customNonce));
		}

		this.etherscanApiKey = etherscanApiKey;
		this.alchemyKeyUrl = alchemyKeyUrl;
		this.webhookUrl = webhookUrl;
		this.twocapKeys = twocapKeys;

	}

	public EthMintingTask(int taskId, String assetName, String assetURL, String assetImg, String assetFloor,
			String assetPrice, String assetsAmount, String useSafeMode, String secretKey, String contractAddress,
			String transactionData, String transactionValue, String amountGasAddedToRapid, String providedGasLimit,
			String timerString, String customNonce, String alchemyKeyUrl, String etherscanApiKey, String webhookUrl) {

		this.taskId = taskId;
		this.assetName = assetName;
		this.assetURL = assetURL;
		this.assetImg = assetImg;
		this.assetFloor = assetFloor;
		this.assetPrice = assetPrice;
		this.assetsAmount = assetsAmount;
		this.useSafeMode = useSafeMode;
		this.secretKey = secretKey;
		this.contractAddress = contractAddress;
		this.transactionData = transactionData;
		this.transactionValue = transactionValue;
		this.amountGasAddedToRapid = amountGasAddedToRapid;
		this.providedGasLimit = providedGasLimit;
		this.timerString = timerString;
		if (customNonce != null) {
			this.nonce = BigInteger.valueOf(Long.valueOf(customNonce));
		}

		this.etherscanApiKey = etherscanApiKey;
		this.alchemyKeyUrl = alchemyKeyUrl;
		this.webhookUrl = webhookUrl;
	}

	public void run() {

		try {
			prepareTask();
			initializeTimer();

			while (!monitorContract()) {
				Thread.sleep(this.sendingDelay);
			}
			sendTransaction();
			addPendingTransactionToJson();
			parseWebhook("PENDING");
			pollTransactionStatus();
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now())
					+ "] - [------------------------------------------] - [" + taskId + "] - Generous Error: "
					+ e.getMessage() + ANSI_RESET);

			e.printStackTrace();
		}

	}

	public void write(String response) throws IOException {
		FileWriter writer = new FileWriter(new File("logs.txt"));
		writer.write(response);
		writer.close();
	}

	public void prepareTask() throws Exception {

		System.out.println("[ETHER] - [" + dtf.format(now.now())
				+ "] - [------------------------------------------] - [" + taskId + "] - Preparing Wallet...");

		this.web3j = Web3j.build(new HttpService(alchemyKeyUrl));

		// Get address
		this.credentials = Credentials.create(secretKey);
		this.ownerAddress = credentials.getAddress();

		// Check if should send all
		parseTransactionValue();

		// Set transactionValue as hex
		this.transactionValueHex = "0x"
				+ Convert.toWei(transactionValue, Convert.Unit.ETHER).toBigInteger().toString(16);

		// setNonce
		if (this.nonce == null) {
			this.nonce = getNonce();
		}

		this.contractAddress = this.contractAddress.strip();

		if (this.function != null && !this.function.equals("")) {
			this.functionParameters = this.functionParameters.strip();
			getContractABI();
			parseFunctionParameters();
		}

	}

	public void getContractABI() throws Exception {

		try {

			String response = "";

			if (this.contractAddress.equals("0xD152f549545093347A162Dce210e7293f145215")) {
				System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
						+ "] - Detected Disperse.." + ANSI_RESET);

				response = "[{\"constant\":false,\"inputs\":[{\"name\":\"token\",\"type\":\"address\"},{\"name\":\"recipients\",\"type\":\"address[]\"},{\"name\":\"values\",\"type\":\"uint256[]\"}],\"name\":\"disperseTokenSimple\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"token\",\"type\":\"address\"},{\"name\":\"recipients\",\"type\":\"address[]\"},{\"name\":\"values\",\"type\":\"uint256[]\"}],\"name\":\"disperseToken\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"recipients\",\"type\":\"address[]\"},{\"name\":\"values\",\"type\":\"uint256[]\"}],\"name\":\"disperseEther\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";

				JSONArray abi = new JSONArray(new JSONTokener(response));

				getElsParameters(false);

				for (int i = 0; i < abi.length(); i++) {
					try {
						JSONObject currentFunction = new JSONObject(new JSONTokener(abi.get(i).toString()));

						if (currentFunction.getString("name").toLowerCase().equals(this.function.toLowerCase())) {
							this.functionABI = currentFunction.toString();

							return;
						}
					} catch (Exception e) {

					}

				}

				throw new Exception("PROVIDED_FUNCTION_NOT_FOUND");

			} else {
				System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
						+ "] - Getting Contract...");
				Session session = Requests.session();

				RawResponse newSession = session
						.get("https://api.etherscan.io/api?module=contract&action=getabi&address="
								+ this.contractAddress + "&apikey=" + this.etherscanApiKey)
						.socksTimeout(60_000).connectTimeout(60_000).send();

				if (newSession.statusCode() != 200) {
					System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
							+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

					Thread.sleep(1000);
					getContractABI();

				}

				response = newSession.readToText();

				JSONObject o = new JSONObject(new JSONTokener(response));
				String status = o.getString("status");

				if (status.equals("1")) {
					JSONArray abi = new JSONArray(o.getString("result"));

					for (int i = 0; i < abi.length(); i++) {
						try {
							JSONObject currentFunction = new JSONObject(new JSONTokener(abi.get(i).toString()));

							if (currentFunction.getString("name").toLowerCase().equals(this.function.toLowerCase())) {
								this.functionABI = currentFunction.toString();

								return;
							}
						} catch (Exception e) {

						}

					}

					throw new Exception("PROVIDED_FUNCTION_NOT_FOUND");

				} else {
					throw new Exception(o.getString("result"));
				}
			}

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Contract Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(3000);
			getContractABI();
		}

	}

	public void getElsParameters(boolean useCaptcha) throws Exception {
		try {
			System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
					+ "] - Requesting Signature...");

			Config config = new Config().connectTimeout(30_000);

			UnirestInstance unirest = new UnirestInstance(config);

			String body = "";

			if (useCaptcha) {
				body = captcha.getCaptchaToken();
			}

			HttpResponse<String> response = unirest.get("https://api.elysiumshell.xyz/public/" + this.ownerAddress)
					.header("Authority", "discord.com").header("Accept", "*/*")
					.header("Content-Type", "application/json").header("Accept-Language", "de")
					.header("Referer", "https://discord.com/channels/@me")
					.header("Sec-Ch-Ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"")
					.header("Sec-Ch-Ua-Mobile", "?0").header("Sec-Ch-Ua-Platform", "\"Windows\"")
					.header("Sec-Fetch-Dest", "empty").header("Sec-Fetch-Mode", "cors")
					.header("Sec-Fetch-Site", "same-origin")

					.header("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
					.queryString("token", body).asString();

			String responseBody = response.getBody();

			System.out.println(responseBody);

			if (response.getStatus() != 200) {

				throw new Exception(responseBody.toUpperCase());
			} else {
				JSONObject o = new JSONObject(new JSONTokener(responseBody.toLowerCase()));

				this.elsTicket = String.valueOf(o.get("ticket"));
				this.elsSignature = o.getString("signature");

				this.functionParameters = this.functionParameters + "," + this.elsTicket + "," + this.elsSignature;

				System.out.println(functionParameters);

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - GET SIG FAILED: " + e.getMessage() + ANSI_RESET);

			Thread.sleep(3000);

			if (e.getMessage().toUpperCase().equals("PUBLIC SALE NOT LIVE YET")) {
				getElsParameters(false);
			} else {
				getElsParameters(true);

			}

		}

	}

	public void parseFunctionParameters() throws Exception {
		// new Tuple(Address.wrap(address), BigInteger.valueOf(1000));
		try {
			if (this.functionParameters != null && this.functionParameters.toLowerCase().contains("my_address")) {
				this.functionParameters = functionParameters.toLowerCase().replace("my_address", this.ownerAddress);
			}

			if (!this.functionParameters.equals("")) {
				String fctArr[] = this.functionParameters.split(Pattern.quote(","));
				Object[] arguments = new Object[fctArr.length];

				JSONArray inputs = new JSONArray(new JSONObject(this.functionABI).getJSONArray("inputs"));

				for (int i = 0; i < inputs.length(); i++) {
					JSONObject input = inputs.getJSONObject(i);

					String type = input.getString("type").toLowerCase();

					if (type.equals("bool")) {
						arguments[i] = Boolean.valueOf(fctArr[i]);

					} else if (type.contains("uint256[]")) {
//					arguments[i] = String.valueOf(fctArr[i]); // splits too early , in function parameter. does not detect array

					} else if (type.contains("address[]")) {
//					arguments[i] = String.valueOf(fctArr[i]);

					} else if (type.contains("uint8")) {
						arguments[i] = Integer.valueOf(fctArr[i]);

					} else if (type.contains("int")) {
						arguments[i] = new BigInteger(fctArr[i]);

					} else if (type.contains("byte")) {
						arguments[i] = fctArr[i].getBytes();
					} else if (type.equals("string")) {
						arguments[i] = fctArr[i];
					} else if (type.equals("address")) {

						arguments[i] = Address.wrap(Address.toChecksumAddress(fctArr[i]));

					} else {
						throw new Exception("Failed To Parse: " + arguments[i]);
					}

				}

				Tuple tuple = null;

				// disperse
				if (this.contractAddress.toLowerCase().equals("0xd152f549545093347a162dce210e7293f1452150")) {
					tuple = getDisperseAddressArray();
				} else {
					// detect tuple
					tuple = new Tuple(arguments);
				}

				Function f2 = Function.fromJson(this.functionABI);

//			System.out.println("0x" + f2.selectorHex());

				ByteBuffer one = f2.encodeCall(tuple);
				String result = Function.formatCall(one.array());

				StringBuilder sb = new StringBuilder();
				sb.append("0x");

				String arr[] = result.split("\n");
				for (String s : arr) {
					String[] temp = s.split(Pattern.quote(" "));
					String data = temp[temp.length - 1];
					sb.append(data);
				}

				String hexData = sb.toString();

				this.transactionData = hexData;
			} else {
				// parse function with empty input
				Function f2 = Function.fromJson(this.functionABI);
				String hexSelector = "0x" + f2.selectorHex();
				this.transactionData = hexSelector;

			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Parse Contract Failed: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(1500);
			parseFunctionParameters();
		}

	}

	public Tuple getDisperseAddressArray() {
		String[] disperse = this.functionParameters.replace(" ", "").split(Pattern.quote("],["));
		String[] disperseAdd = disperse[0].replace("[", "").split(",");
		Address[] addresses = new Address[disperseAdd.length];

		for (int i = 0; i < disperseAdd.length; i++) {
			addresses[i] = Address.wrap(Address.toChecksumAddress(disperseAdd[i].toString()));
		}

		disperseAdd = disperse[1].replace("]", "").split(",");
		BigInteger[] values = new BigInteger[disperseAdd.length];

		for (int i = 0; i < disperseAdd.length; i++) {
			values[i] = new BigInteger(disperseAdd[i]);
		}

		Tuple tuple = new Tuple(addresses, values);

		return tuple;

	}

	public void parseTransactionValue() throws Exception {
		if (transactionValue.toLowerCase().equals("all")) {

			BigDecimal transactionValueTemp = getWalletBalance();

			BigDecimal gasLimit = BigDecimal.valueOf(21000);
			BigDecimal gasFeesInEth = gasLimit.multiply(BigDecimal.valueOf(getRapidGas()))
					.multiply(BigDecimal.valueOf(0.000000001));

			transactionValue = transactionValueTemp.subtract(gasFeesInEth).setScale(4, RoundingMode.HALF_DOWN)
					.toString();

		}
	}

	public void initializeTimer() throws Exception {
		if (!this.timerString.toLowerCase().equals("")) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Date date = sdf.parse(timerString);
			long launchTime = date.getTime() / 1000;
			System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
					+ "] - Detected Timer: " + timerString + ANSI_RESET);

			long currentTime = Instant.now().getEpochSecond();

			while (launchTime > currentTime) {
				Thread.sleep(1000);
				currentTime = Instant.now().getEpochSecond();
			}

		}
	}

	public BigInteger getNonce() throws InterruptedException, ExecutionException {

		EthGetTransactionCount ethGetTransactionCount = web3j
				.ethGetTransactionCount(ownerAddress, DefaultBlockParameterName.LATEST).sendAsync().get();

		return ethGetTransactionCount.getTransactionCount();
	}

	public long getRapidGas() throws InterruptedException {
		Session session = Requests.session();

		RawResponse newSession = session.get("https://blocknative-api.herokuapp.com/data").socksTimeout(60_000)
				.connectTimeout(60_000).send();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
					+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

			Thread.sleep(500);
			getRapidGas();

		}
		String response = newSession.readToText();
		JSONObject o = new JSONObject(new JSONTokener(response));
		int gas = new JSONObject(new JSONTokener(o.getJSONArray("estimatedPrices").get(0).toString())).getInt("price");

		System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
				+ "] - Rapid Gas Currently: " + gas + " GWEI" + ANSI_RESET);

		return Long.valueOf(gas);
	}

	public boolean monitorContract() throws NumberFormatException, Exception {
		// Estimate with web3j

		this.rapidGas = Long.valueOf(getFinalGasPrice());
		String hexRapidGas = "0x"
				+ Convert.toWei(String.valueOf(rapidGas), Convert.Unit.GWEI).toBigInteger().toString(16);

		int latestBlock = 0;
		if (this.useSafeMode.toLowerCase().equals("true")) {
			latestBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber().intValue();

		}
		try {

			String body = "{\n" + "	\"jsonrpc\":\"2.0\",\n" + "	\"method\":\"eth_estimateGas\",\n" + "	\"params\":[{\n"
					+ "		 \"from\": \"" + ownerAddress + "\",\"to\": \"" + contractAddress + "\", \"value\": \""
					+ transactionValueHex + "\",\"data\": \"" + transactionData + "\"\n" + "	}],\n" + "	\"id\":1\n"
					+ "}";

			Session session = Requests.session();

			Map<String, Object> request = new HashMap<>();
			request.put("Content-Type", "application/json");
			RawResponse newSession = session.post(alchemyKeyUrl).headers(request).body(body).socksTimeout(60_000)
					.connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
						+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

				throw new Exception("MONITOR_CONTRACT_ERROR");

			}

			String response = newSession.readToText();

			Long gasAmount = 0L;
			try {
				gasAmount = Long.decode(parseResult(response));
				setGasLimit(gasAmount);
				getNextBlock(latestBlock);
				return true;
			} catch (Exception e) {
				throw new Exception(parseError(response));
			}
		} catch (Exception e) {

			if (e.toString().contains("MONITOR_CONTRACT_ERROR")) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
						+ taskId + "] - Retrying: " + e.getMessage() + ANSI_RESET);
			} else {
				System.out.println(ANSI_YELLOW + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
						+ "] - [" + taskId + "] - Retrying: " + e.getMessage() + ANSI_RESET);
			}

			return false;
		}

	}

	public void setGasLimit(Long gasAmount) throws Exception {
		if (this.providedGasLimit.toLowerCase().equals("auto")) {
			this.providedGasLimit = String.valueOf(gasAmount + (gasAmount / 100 * 40)); // Add 40%
			System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Set Gas-Limit: " + this.providedGasLimit + ANSI_RESET);
		}
	}

	public void getNextBlock(int providedBlock) throws Exception {

		if (this.useSafeMode.toLowerCase().equals("true")) {
			System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Waiting For Target Block: " + (providedBlock + 1) + ANSI_RESET);
			int currentBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber().intValue();
			while (currentBlock < (providedBlock + 1)) {

				Thread.sleep(300);
				currentBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber().intValue();
			}

		}
	}

	public String getFinalGasPrice() throws Exception {
		amountGasAddedToRapid = amountGasAddedToRapid.replace(" ", "");

		if (amountGasAddedToRapid.toLowerCase().equals("rapid")) {
			this.sendingDelay = 1000;
			return BigInteger.valueOf(getRapidGas()).toString();
		} else if (amountGasAddedToRapid.toLowerCase().contains("rapid")
				&& amountGasAddedToRapid.toLowerCase().contains("+")) {
			this.sendingDelay = 1500;
			String amountToAddOnRapid = amountGasAddedToRapid.toLowerCase().replace("rapid+", "").strip();
			return BigInteger.valueOf(getRapidGas()).add(BigInteger.valueOf(Integer.valueOf(amountToAddOnRapid)))
					.toString();
		} else {
			try {
				int preSetGwei = Integer.parseInt(amountGasAddedToRapid.strip());
				this.sendingDelay = 700;
				return String.valueOf(preSetGwei);
			} catch (Exception e) {
				this.sendingDelay = 700;
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
						+ taskId + "] - Parsing Provided Gas Fee Failed. Using Rapid Gas..." + ANSI_RESET);
				return BigInteger.valueOf(getRapidGas()).toString();

			}
		}

	}

	public String signTransaction() throws Exception {

		finalGasPrice = getFinalGasPrice();

		System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
				+ "] - Signing Transaction... [" + transactionValue + " E | " + finalGasPrice + " GWEI]");

		Integer gasLimit = 0;

		try {
			if (providedGasLimit.equals("")) {
				gasLimit = 300000;
			} else {
				gasLimit = Integer.valueOf(providedGasLimit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		BigInteger gasLimitInteger = BigInteger.valueOf(gasLimit.intValue());

		StaticGasProvider gasProvider = new StaticGasProvider(
				Convert.toWei(finalGasPrice, Convert.Unit.GWEI).toBigInteger(), gasLimitInteger);

		RawTransaction raw = RawTransaction.createTransaction(nonce, gasProvider.getGasPrice(), gasLimitInteger,
				contractAddress, Convert.toWei(transactionValue, Convert.Unit.ETHER).toBigInteger(), transactionData);
		byte[] signedMessage = TransactionEncoder.signMessage(raw, 1, credentials);

		return Numeric.toHexString(signedMessage);

	}

	public void sendTransaction() throws Exception {

		String body = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_sendRawTransaction\",\"params\":[\"" + signTransaction()
				+ "\"],\"id\":1}";

		System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
				+ "] - Sending Transaction... [" + transactionValue + " E | " + finalGasPrice + " GWEI]");

		Session session = Requests.session();
		Map<String, Object> request = new HashMap<>();
		request.put("Content-Type", "application/json");
		RawResponse newSession = session.post(alchemyKeyUrl).headers(request).body(body).socksTimeout(60_000)
				.connectTimeout(60_000).send();

		if (newSession.statusCode() != 200) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
					+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

			Thread.sleep(300);
			sendTransaction();

		}

		String response = newSession.readToText();

		try {
			this.txHash = parseResult(response);
			System.out.println(ANSI_YELLOW + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Received Hash: " + txHash + ANSI_RESET);
			this.status = "PENDING";

			this.timeStamp = dtf.format(now);
		} catch (Exception e) {

			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Retrying: " + parseError(response) + ANSI_RESET);

			Thread.sleep(sendingDelay);
			sendTransaction();
		}

	}

	public void pollTransactionStatus() throws InterruptedException {

		System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
				+ "] - Monitoring Transaction Status..." + ANSI_RESET);

		while (!this.isFinished) {

			Session session = Requests.session();

			RawResponse newSession = session
					.get("https://api.etherscan.io/api?module=transaction&action=gettxreceiptstatus&txhash=" + txHash
							+ "&apikey=" + etherscanApiKey)
					.socksTimeout(60_000).connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
						+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);
				Thread.sleep(1000);
				pollTransactionStatus();
			}

			String response = newSession.readToText();

			try {

				JSONObject o = new JSONObject(new JSONTokener(response));
				String status = o.getJSONObject("result").getString("status");

				if (status.equals("1")) {

					if (!hasTXInternalErrors()) {
						System.out.println(ANSI_GREEN + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
								+ "] - [" + taskId + "] - Transaction Succeeded." + ANSI_RESET);
						this.status = "SUCCESS";

						if (!this.transactionData.toLowerCase().equals("0x") && !this.contractAddress.toLowerCase()
								.equals("0xd152f549545093347a162dce210e7293f1452150")) {
							BackendWebhook b = new BackendWebhook("ETH Contract Mint",
									"**Contract:** [" + contractAddress + "](https://etherscan.io/address/"
											+ contractAddress + ")\\n**Value:** " + transactionValue + " ETH",
									null,
									"https://discord.com/api/webhooks/951536933664870502/dwZ3guOyZFo3Bi1EXTfm-n3mbIWf-iY2EpZE53YEXo6Wx5FezNtA3gI4JtyUsB3vemHn");
						}

						parseWebhook("SUCCESS");
						removeEntryFromPendingTransactions(txHash);
						this.isFinished = true;
					} else {
						System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
								+ "] - [" + taskId + "] - Transaction Failed." + ANSI_RESET);
						this.status = "FAILED";
						parseWebhook("FAILED");
						removeEntryFromPendingTransactions(txHash);
						this.isFinished = true;
					}

				} else if (status.equals("0")) {
					System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
							+ "] - [" + taskId + "] - Transaction Failed." + ANSI_RESET);
					this.status = "FAILED";
					parseWebhook("FAILED");
					removeEntryFromPendingTransactions(txHash);
					this.isFinished = true;

				} else {
					Thread.sleep(3000);
				}
			} catch (Exception e) {
				Thread.sleep(5000);
				pollTransactionStatus();
			}
		}

	}

	public boolean hasTXInternalErrors() throws Exception {
		if (this.assetFloor == null) {
			return false;
		} else {
			// Wait som seconds for reverting TX
			Thread.sleep(5000);
			Session session = Requests.session();

			RawResponse newSession = session
					.get("https://api.etherscan.io/api?module=account&action=txlistinternal&address="
							+ this.ownerAddress + "&sort=desc&apikey=" + etherscanApiKey)
					.socksTimeout(60_000).connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
						+ taskId + "] - Retrying: " + newSession.statusCode() + ANSI_RESET);
				Thread.sleep(1000);
				hasTXInternalErrors();
			}

			String response = newSession.readToText();
			JSONObject o = new JSONObject(new JSONTokener(response));
			JSONArray result = o.getJSONArray("result");
			try {
				for (int i = 0; i < result.length(); i++) {
					JSONObject entry = result.getJSONObject(i);
					String hash = entry.getString("hash");
					if (hash.toLowerCase().equals(this.txHash.toLowerCase())) {
						try {
							this.transactionValue = Convert.fromWei(entry.getString("value"), Unit.ETHER).toString();
						} catch (Exception e) {
							e.printStackTrace();
						}

						return true;
					}

				}

			} catch (Exception e) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
						+ taskId + "] - Confirmation Failed: " + e.getMessage() + ANSI_RESET);
			}
		}

		return false;
	}

	public BigDecimal getWalletBalance() throws InterruptedException, ExecutionException {
		BigDecimal balance = null;

		try {
			EthGetBalance ethGetBalance = web3j.ethGetBalance(this.ownerAddress, DefaultBlockParameterName.LATEST)
					.sendAsync().get();

			BigInteger wei = ethGetBalance.getBalance();

			balance = new BigDecimal(Convert.fromWei(wei.toString(), Unit.ETHER).toString());

			System.out.println(ANSI_GREY + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
					+ "] - Wallet Contains " + balance.toString() + " ETH" + ANSI_RESET);

		} catch (Exception e) {
			System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
					+ "] - Retrying: " + e.getMessage());
		}

		return balance;
	}

	public String parseError(String json) {
		try {

			JSONTokener tokener = new JSONTokener(json);
			String errorMessage = new JSONObject(tokener).getJSONObject("error").getString("message").replace("\"", "")
					.toUpperCase();

			return errorMessage;
		} catch (Exception e) {
			return json;
		}

	}

	public String parseResult(String json) throws Exception {

		try {
//			System.out.println(json);
			JSONTokener tokener = new JSONTokener(json);
			String rerultMessage = new JSONObject(tokener).getString("result").replace("\"", "").toUpperCase();

			return rerultMessage;
		} catch (Exception e) {
			throw new Exception("ParseResultError");
		}

	}

	synchronized void addPendingTransactionToJson() throws Exception {
		String content = Files.readString(
				Path.of(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json"),
				Charset.defaultCharset());
		JSONArray array = new JSONArray(new JSONTokener(content));

		JSONObject newObject = new JSONObject();
		newObject.put("secretKey", secretKey);
		newObject.put("contractAddress", contractAddress);
		newObject.put("transactionData", transactionData);
		newObject.put("transactionValue", transactionValue);
		newObject.put("gasPrice", finalGasPrice);
		newObject.put("providedGasLimit", providedGasLimit);
		newObject.put("nonce", String.valueOf(nonce));
		newObject.put("alchemyKeyUrl", alchemyKeyUrl);
		newObject.put("etherscanApiKey", etherscanApiKey);
		newObject.put("webhookUrl", webhookUrl);
		newObject.put("transactionHash", txHash);
		newObject.put("timeStamp", timeStamp);

		array.put(newObject);

		FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json");
		writer.write(array.toString());
		writer.close();
	}

	synchronized void removeEntryFromPendingTransactions(String transactionHash) throws Exception {
		String content = Files.readString(
				Path.of(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json"),
				Charset.defaultCharset());
		JSONArray array = new JSONArray(new JSONTokener(content));

		for (int i = 0; i < array.length(); i++) {
			JSONObject o = new JSONObject(new JSONTokener(array.get(i).toString()));
			if (o.getString("transactionHash").equals(transactionHash)) {
				array.remove(i);
				break;
			}
		}

		FileWriter writer = new FileWriter(System.getProperty("user.dir") + "\\target\\ethPendingTransactions.json");
		writer.write(array.toString());
		writer.close();
	}

	public boolean getIsFinished() {
		return this.isFinished;
	}

	public void parseWebhook(String status) throws Exception {
		if (this.assetName != null) {
			sendSniperWebhook(status);
		} else {
			sendWebhook(status);
		}
	}

	public void sendWebhook(String status) throws IOException, LineUnavailableException, InterruptedException {

		Color color = Color.GRAY;
		if (status.equals("PENDING")) {
			color = Color.orange;
		} else if (status.equals("SUCCESS")) {
			color = Color.green;
		} else if (status.equals("FAILED")) {
			color = Color.red;
		}

		String title = "";
		if (status.equals("PENDING")) {
			title = "PENDING ETHER TRANSACTION";
		} else if (status.equals("SUCCESS")) {
			title = "ETHER TRANSACTION SUCCEEDED";
		} else if (status.equals("FAILED")) {
			title = "ETHER TRANSACTION FAILED";
		}

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

		if (status.equals("PENDING")) {
			webhook.setUsername("N3RO BOT");
			webhook.setAvatarUrl(
					"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
			webhook.setTts(false);
			webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title)
					.setUrl("https://etherscan.io/tx/" + txHash.toLowerCase())

					.addField("From", "||[" + ownerAddress + "](https://etherscan.io/address/" + ownerAddress + ")||",
							false)
					.addField("To",
							"[**" + contractAddress + "**](https://etherscan.io/address/" + contractAddress + ")",
							false)
					.addField("Nonce", "||" + nonce + "||", true)
					.addField("Gwei", String.valueOf("||" + finalGasPrice + "||"), true)
					.addField("Value", "**" + transactionValue + " ETH**", true)

					.setFooter(dtf.format(now.now()) + " | @n3robot", null).setColor(color));
		} else {
			webhook.setUsername("N3RO BOT");
			webhook.setAvatarUrl(
					"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
			webhook.setTts(false);
			webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title)
					.setUrl("https://etherscan.io/tx/" + txHash.toLowerCase())
					.addField("From", "||[" + ownerAddress + "](https://etherscan.io/address/" + ownerAddress + ")||",
							false)
					.addField("To",
							"[**" + contractAddress + "**](https://etherscan.io/address/" + contractAddress + ")",
							false)
					.addField("Nonce", "||" + nonce + "||", true)
					.addField("Gwei", String.valueOf("**" + finalGasPrice + "**"), true)
					.addField("Value", "**" + transactionValue + " ETH**", true)
					.setFooter(dtf.format(now.now()) + " | @n3robot", null).setColor(color));
		}

		try {
			webhook.execute();
			System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
					+ "] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Send Webhook Failed: " + e.getMessage() + ANSI_RESET);

		}
	}

	public void sendSniperWebhook(String status) throws IOException, LineUnavailableException, InterruptedException {

		Color color = Color.GRAY;
		if (status.equals("PENDING")) {
			color = Color.orange;
		} else if (status.equals("SUCCESS")) {
			color = Color.green;
		} else if (status.equals("FAILED")) {
			color = Color.red;
		}

		String title = "";
		if (status.equals("PENDING")) {
			title = "PENDING SNIPER TRANSACTION";
		} else if (status.equals("SUCCESS")) {
			title = "SNIPER TRANSACTION SUCCEEDED";
		} else if (status.equals("FAILED")) {
			title = "SNIPER TRANSACTION FAILED";
		}

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

		if (status.equals("PENDING")) {
			webhook.setUsername("N3RO BOT");
			webhook.setAvatarUrl(
					"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
			webhook.setTts(false);
			webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title)
					.setUrl("https://etherscan.io/tx/" + txHash.toLowerCase())

					.addField("From", "||[" + ownerAddress + "](https://etherscan.io/address/" + ownerAddress + ")||",
							false)
					.addField("Item", "[**" + assetName + "**](" + assetURL + ")", false)
					.addField("Price", "**" + assetPrice + " ETH**", true)
					.addField("Floor", "**" + assetFloor + " ETH**", true)
					.addField("Gwei", String.valueOf("**" + finalGasPrice + "**"), true)
					.addField("Qty", "**" + this.assetsAmount + "**", true).setThumbnail(assetImg)
					.setFooter(dtf.format(now.now()) + " | @n3robot", null).setColor(color));
		} else {
			webhook.setUsername("N3RO BOT");
			webhook.setAvatarUrl(
					"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");
			webhook.setTts(false);
			webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title)
					.setUrl("https://etherscan.io/tx/" + txHash.toLowerCase())
					.addField("From", "||[" + ownerAddress + "](https://etherscan.io/address/" + ownerAddress + ")||",
							false)
					.addField("Item", "[**" + assetName + "**](" + assetURL + ")", false)
					.addField("Price", "**" + assetPrice + " ETH**", true)
					.addField("Floor", "**" + assetFloor + " ETH**", true)
					.addField("Gwei", String.valueOf("**" + finalGasPrice + "**"), true)
					.addField("Qty", "**" + this.assetsAmount + "**", true).setThumbnail(assetImg)
					.setFooter(dtf.format(now.now()) + " | @n3robot", null).setColor(color));
		}

		try {
			webhook.execute();
			System.out.println("[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - [" + taskId
					+ "] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress + "] - ["
					+ taskId + "] - Send Webhook Failed: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(10000);
			sendWebhook(status);

		}
	}

	public String getStatus() {
		return this.status;
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
