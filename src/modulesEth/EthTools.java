package modulesEth;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.utils.Convert;

import com.esaulpaugh.headlong.abi.Address;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;

import main.Main;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class EthTools extends Main {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();
	private String contractAddress;
	private String ownerAddress = "0x0000000000000000000000000000000000000000";
	private BigDecimal transactionValue;
	private String function;
	private String functionABI;
	private String functionParameters;
	private String transactionData;
	private BigDecimal gasLimit;

//	private String mode;

//	public EthTools(String contractAddress, String transactionValue, String function, String functionParamet) {
//		if(mode.equals("TRANSACTION_COST_CALC")) {
//			printTransactionCosts();
//		}
//	}

	public void printTransactionCosts() {
		System.out.print(ANSI_YELLOW + "\n>>> Set Gas Limit: " + ANSI_RESET);
		// Use custom CMID
		Scanner inScanner = new Scanner(System.in);
		long providedGasLimit = Long.valueOf(inScanner.next());
		BigDecimal gasLimit = BigDecimal.valueOf(providedGasLimit);

		System.out.print(ANSI_YELLOW + "\n>>> Set NFT Price: " + ANSI_RESET);
		// Use custom CMID
		inScanner = new Scanner(System.in);
		double providedNFTPrice = Double.valueOf(inScanner.next());
		BigDecimal price = BigDecimal.valueOf(providedNFTPrice);

		StringBuilder sb = new StringBuilder();
		ArrayList<Double> x = new ArrayList<Double>();
		x.add((double) 100);
		x.add((double) 200);
		x.add((double) 300);
		x.add((double) 400);
		x.add((double) 500);
		x.add((double) 600);
		x.add((double) 750);
		x.add((double) 1000);
		x.add((double) 1250);
		x.add((double) 1500);
		x.add((double) 1750);
		x.add((double) 2000);
		x.add((double) 2500);
		x.add((double) 3000);
		x.add((double) 3500);
		x.add((double) 4000);
		x.add((double) 5000);
		x.add((double) 6000);
		x.add((double) 7000);
		x.add((double) 8000);
		x.add((double) 9000);
		x.add((double) 10000);
		x.add((double) 12500);
		x.add((double) 15000);
		x.add((double) 17500);
		x.add((double) 20000);

		// transaction in ETH = gas required * gas price in gwei * 0.000000001
		sb.append(ANSI_GREY + "\nGas Limit: " + gasLimit.toString() + " - Price: " + price.toString() + " ETH"
				+ ANSI_RESET + "\n\n");
		// [100 GWEI ] 0.0300 | 0.1300 ETH
		sb.append("             Fees  " + ANSI_GREY + "     | " + ANSI_RESET + ANSI_YELLOW + "Final Price\n"
				+ ANSI_RESET);
		for (Double entry : x) {
			BigDecimal gasFeesInEth = gasLimit.multiply(BigDecimal.valueOf(entry))
					.multiply(BigDecimal.valueOf(0.000000001));
			BigDecimal finalPrice = price.add(gasFeesInEth);

			String substring = " GWEI] ";

			if (entry < 1000) {
				substring = " GWEI  ] ";
			} else if (entry < 9999) {
				substring = " GWEI ] ";

			}
			sb.append(ANSI_GREY + "[" + entry.intValue() + substring + ANSI_RESET
					+ String.valueOf(gasFeesInEth.setScale(4, RoundingMode.HALF_UP)) + " ETH" + ANSI_GREY + " | "
					+ ANSI_RESET + ANSI_YELLOW + String.valueOf(finalPrice.setScale(4, RoundingMode.HALF_UP)) + " ETH\n"
					+ ANSI_RESET);
		}

		System.out.println(sb.toString());
	}

	public void printContractCosts() throws Exception {

		// ------------------------------
		System.out.println(ANSI_CYAN + "\n>>> Type Contract Address..." + ANSI_RESET);
		Scanner inScanner = new Scanner(System.in);
		this.contractAddress = String.valueOf(inScanner.next());

		System.out.println(ANSI_CYAN + "\n>>> Type Function Name..." + ANSI_RESET);
		inScanner = new Scanner(System.in);
		this.function = String.valueOf(inScanner.next());

		System.out.println(ANSI_CYAN + "\n>>> Type Function Parameters..." + ANSI_RESET);
		inScanner = new Scanner(System.in);
		this.functionParameters = String.valueOf(inScanner.next());

		System.out.println(ANSI_CYAN + "\n>>> Type Transaction Value..." + ANSI_RESET);
		inScanner = new Scanner(System.in);
		this.transactionValue = new BigDecimal(String.valueOf(inScanner.next()));
		// ------------------------------

		getContractABI();
		parseFunctionParameters();
		monitorContract();
		StringBuilder sb = new StringBuilder();
		ArrayList<Double> x = new ArrayList<Double>();
		x.add((double) 100);
		x.add((double) 200);
		x.add((double) 300);
		x.add((double) 400);
		x.add((double) 500);
		x.add((double) 600);
		x.add((double) 750);
		x.add((double) 1000);
		x.add((double) 1250);
		x.add((double) 1500);
		x.add((double) 1750);
		x.add((double) 2000);
		x.add((double) 2500);
		x.add((double) 3000);
		x.add((double) 3500);
		x.add((double) 4000);
		x.add((double) 5000);
		x.add((double) 6000);
		x.add((double) 7000);
		x.add((double) 8000);
		x.add((double) 9000);
		x.add((double) 10000);
		x.add((double) 12500);
		x.add((double) 15000);
		x.add((double) 17500);
		x.add((double) 20000);

		// transaction in ETH = gas required * gas price in gwei * 0.000000001
		sb.append(ANSI_GREY + "\nGas Limit: " + gasLimit.toString() + " - Price: " + transactionValue.toString()
				+ " ETH" + ANSI_RESET + "\n\n");
		// [100 GWEI ] 0.0300 | 0.1300 ETH
		sb.append(
				"             Fees  " + ANSI_GREY + "     | " + ANSI_RESET + ANSI_CYAN + "Final Price\n" + ANSI_RESET);
		for (Double entry : x) {
			BigDecimal gasFeesInEth = gasLimit.multiply(BigDecimal.valueOf(entry))
					.multiply(BigDecimal.valueOf(0.000000001));
			BigDecimal finalPrice = transactionValue.add(gasFeesInEth);

			String substring = " GWEI] ";

			if (entry < 1000) {
				substring = " GWEI  ] ";
			} else if (entry < 9999) {
				substring = " GWEI ] ";

			}
			sb.append(ANSI_GREY + "[" + entry.intValue() + substring + ANSI_RESET
					+ String.valueOf(gasFeesInEth.setScale(4, RoundingMode.HALF_UP)) + " ETH" + ANSI_GREY + " | "
					+ ANSI_RESET + ANSI_CYAN + String.valueOf(finalPrice.setScale(4, RoundingMode.HALF_UP)) + " ETH\n"
					+ ANSI_RESET);
		}

		System.out.println(sb.toString());

	}

	public void monitorContract() throws NumberFormatException, Exception {
		// Estimate with web3j

		String transactionValueHex = "0x"
				+ Convert.toWei(transactionValue, Convert.Unit.ETHER).toBigInteger().toString(16);
		try {

			String body = "{\n" + "	\"jsonrpc\":\"2.0\",\n" + "	\"method\":\"eth_estimateGas\",\n" + "	\"params\":[{\n"
					+ "		 \"from\": \"" + ownerAddress + "\",\"to\": \"" + contractAddress + "\",\"value\": \""
					+ transactionValueHex + "\",\"data\": \"" + transactionData + "\"\n" + "	}],\n" + "	\"id\":1\n"
					+ "}";

			Session session = Requests.session();

			RawResponse newSession = session
					.post("https://eth-mainnet.alchemyapi.io/v2/qb7hQLA-JAHVCZB3a7e3uwiwxaZkt1eA").body(body)
					.socksTimeout(60_000).connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + contractAddress
						+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

				throw new Exception("TESTNET_ISSUE");

			}

			String response = newSession.readToText();

			Long gasAmount = 0L;
			try {
				gasAmount = Long.decode(parseResult(response));
				this.gasLimit = new BigDecimal(gasAmount);
			} catch (Exception e) {
				throw new Exception(parseError(response));
			}
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + ownerAddress
					+ "] - Retrying: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(500);
			monitorContract();
		}

	}

	public void getContractABI() throws Exception {

		try {
			System.out.println(
					"[ETHER] - [" + dtf.format(now.now()) + "] - [" + contractAddress + "] - Getting Contract...");
			Session session = Requests.session();

			RawResponse newSession = session
					.get("https://api.etherscan.io/api?module=contract&action=getabi&address=" + contractAddress
							+ "&apikey=" + this.getEtherscanApiKey())
					.socksTimeout(60_000).connectTimeout(60_000).send();

			if (newSession.statusCode() != 200) {
				System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + contractAddress
						+ "] - Retrying: " + newSession.statusCode() + ANSI_RESET);

				Thread.sleep(1000);
				getContractABI();

			}

			String response = newSession.readToText();

			JSONObject o = new JSONObject(new JSONTokener(response));
			String status = o.getString("status");

			if (status.equals("1")) {
				JSONArray abi = new JSONArray(o.getString("result"));

				for (int i = 0; i < abi.length(); i++) {
					try {
						JSONObject currentFunction = new JSONObject(new JSONTokener(abi.get(i).toString()));
						if (currentFunction.getString("name").toLowerCase().equals(function.toLowerCase())) {
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

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + contractAddress
					+ "] - Contract Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(3000);
			getContractABI();
		}

	}

	public void parseFunctionParameters() throws Exception {
		// new Tuple(Address.wrap(address), BigInteger.valueOf(1000));
		try {
			String fctArr[] = functionParameters.split(Pattern.quote(","));
			Object[] arguments = new Object[fctArr.length];

			JSONArray inputs = new JSONArray(new JSONObject(this.functionABI).getJSONArray("inputs"));

			for (int i = 0; i < inputs.length(); i++) {
				JSONObject input = inputs.getJSONObject(i);

				String name = input.getString("type").toLowerCase();

				if (name.equals("bool")) {
					arguments[i] = Boolean.valueOf(fctArr[i]);

				} else if (name.contains("uint8")) {
					arguments[i] = Integer.valueOf(fctArr[i]);

				} else if (name.contains("int")) {
					arguments[i] = new BigInteger(fctArr[i]);

				} else if (name.contains("byte")) {
					arguments[i] = fctArr[i].getBytes();
				} else if (name.equals("string")) {
					arguments[i] = fctArr[i];
				} else if (name.equals("address")) {

					if (fctArr[i].toLowerCase().equals("my_address")) {
						arguments[i] = Address.wrap(contractAddress);
					} else {
						arguments[i] = Address.wrap(fctArr[i]);
					}

				} else {
					throw new Exception("Failed To Parse: " + arguments[i]);
				}

			}

			Tuple tuple = new Tuple(arguments);

			Function f2 = Function.fromJson(this.functionABI);

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
		} catch (Exception e) {
			System.out.println(ANSI_RED + "[ETHER] - [" + dtf.format(now.now()) + "] - [" + contractAddress
					+ "] - Parse Contract Failed: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(1500);
			parseFunctionParameters();
		}

	}

	public String getRapidAndBlock() throws InterruptedException {
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
		String block = String.valueOf(new JSONObject(new JSONTokener(response)).get("pendingBlockNumberVal"));

		int gas = new JSONObject(new JSONTokener(o.getJSONArray("estimatedPrices").get(0).toString())).getInt("price");

		return "#" + block + " - " + gas + " GWEI";
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

		return Long.valueOf(gas);
	}

	public String getAmountPendingTxContract(String contract) {
		String i = "NaN";

		try {
			Session session = Requests.session();

			RawResponse newSession = session
					.get("https://etherscan.io/txsPending?a=" + contract.toLowerCase() + "&m=hf").socksTimeout(5000)
					.connectTimeout(5000).send();

			String[] arr = newSession.readToText().split(Pattern.quote(" pending txns found"));
			String temp = arr[0];
			String arr2[] = temp.split(Pattern.quote("A total of "));

			String amount = arr2[arr2.length - 1].strip();

			if (amount.equals("0")) {
				return "null";
			} else {
				return amount;
			}

		} catch (Exception e) {
			return i;
		}

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

	public void launchNode() throws Exception {
		Process proc = Runtime.getRuntime()
				.exec("cmd /c start \"N3RO Bot - Local ETH Node - Close To Shutdown Node\" /min \""
						+ System.getProperty("user.dir")
						+ "\\target\\node.exe\" --http --syncmode \"light\" --http.port \"8400\" --rpc.txfeecap \"0\" --rpc.gascap \"0\" --rpc.allow-unprotected-txs --verbosity 1",
						null, new File(System.getProperty("user.dir") + "\\target\\"));
		System.out.println();
		System.out.println(ANSI_GREEN + "Launched Node! Check New Window." + ANSI_RESET);
		Thread.sleep(2000);
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
