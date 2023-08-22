package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.transaction.type.LegacyTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.crypto.transaction.type.LegacyTransaction;
import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.crypto.transaction.type.TransactionType;

import engines.DiscordEngine;
import engines.MintingEngine;
import engines.OtherEngine;
import engines.SnipingEngine;
import engines.ToolsEngine;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import net.dongliu.requests.utils.Cookies;

public class MainMenu extends Main {

	public MainMenu(boolean initializeConfig) throws InterruptedException, TransactionException, Exception {

		if (initializeConfig) {
			initializeConfig();
		}
		displayMenu();
	}

	public MainMenu(boolean initializeConfig, Worker worker)
			throws InterruptedException, TransactionException, Exception {

		if (initializeConfig) {
			initializeConfig();
			String latestVersion = getLatestBotVersion();

			if (!latestVersion.equals(botVersion.strip())) {
				downloadLatestVersion();
			}
		}

		worker.setData(discordUsername, botVersion);
		displayMenu();
	}

	public void displayMenu() throws InterruptedException, TransactionException, Exception {
		new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		printLogo();

		System.out.println(ANSI_RESET + ANSI_WHITE + "\n> 1. NFT Minting");
//		System.out.println("[2] NFT Sniping");
		System.out.println("> 2. NFT Sniping");
		System.out.println("> 3. NFT Tools");
		System.out.println("> 4. NFT Raffles");
//		System.out.println("> 5. Discord Tools");

//		System.out.println("[X] NFT Monitors");
		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.nextLine();

		switch (input) {
		case "1":
			MintingEngine mintingEngine = new MintingEngine();
			break;
		case "2":
			SnipingEngine snipingEngine = new SnipingEngine();
			break;
		case "3":
			ToolsEngine toolsEngine = new ToolsEngine();
			break;
		case "4":
			Process proc = Runtime.getRuntime().exec("cmd /c start N3RORaffles.exe", null,
					new File(System.getProperty("user.dir")));
//			OtherEngine otherEngine = new OtherEngine();
			break;
//		case "5":
//			DiscordEngine discordEngine = new DiscordEngine();
//			break;

		default:
			System.out.println(ANSI_RED + "Invalid Input!" + ANSI_RESET);
			Thread.sleep(1100);
			displayMenu();
			break;

		}
	}

}
