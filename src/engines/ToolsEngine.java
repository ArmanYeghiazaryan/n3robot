package engines;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import main.Main;
import main.MainMenu;
import modulesEth.EthTools;
import modulesEth.RunCustomSig;
import modulesEth.RunEthCancelTransaction;
import modulesEth.RunEthCheckBalanceTask;
import modulesEth.RunEthGenerateWalletTask;
import modulesEth.RunEthMintingTask;
import modulesEth.RunEthSpeedUpTask;
import modulesEth.RunEthTopupWalletTask;
import modulesEth.RunOpenSeaLister;
import modulesOther.RunPremintTask;
import modulesSol.RunMagicEdenSnipingTask;
import modulesSol.RunSolanaTools;

public class ToolsEngine extends Main {

	public ToolsEngine() throws Exception {

		displayMenu();
	}

	public void displayMenu() throws Exception {

//		printLogo();

		System.out.println(ANSI_GREY + "\n> Wallets" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 1. Check Balances");
		System.out.println("> 2. Generate Wallets");
		System.out.println("> 3. Topup Wallets");
		System.out.println(ANSI_GREY + "\n> TXn" + ANSI_RESET);
		System.out.println("> 4. Gas Fee Calculator");
		System.out.println("> 5. Speed Up Transactions");
		System.out.println("> 6. Cancel Transactions");
		System.out.println(ANSI_GREY + "\n> Private Node" + ANSI_RESET);
		System.out.println("> 7. Launch Server" + ANSI_RESET);
		System.out.println(ANSI_GREY + "\n> OpenSea" + ANSI_RESET);
		System.out.println("> 8. Item Lister" + ANSI_RESET);

//		System.out.println(ANSI_PURPLE + "\n> SOLANA" + ANSI_RESET);
//		System.out.println(ANSI_CYAN + "> 7. Check Balances");
//		System.out.println("> 8. Generate Wallets");
//		System.out.println("> 9. Topup Wallets");
//		System.out.println("> 10. Transfer NFTs");

		System.out.println(ANSI_GREY + "\n> 8. Back To Overview" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		switch (input) {
		case "1":
			RunEthCheckBalanceTask runEthCheckBalanceTask = new RunEthCheckBalanceTask();

			break;
		case "2":
			RunEthGenerateWalletTask runEthGenerateWalletTask = new RunEthGenerateWalletTask();

			break;
		case "3":
			RunEthTopupWalletTask runEthTopupWalletTask = new RunEthTopupWalletTask();

			break;
		case "4":
			EthTools ethTools = new EthTools();
//			ethTools.printContractCosts();
			ethTools.printTransactionCosts();
			break;
		case "5":
			RunEthSpeedUpTask runEthSpeedUpTask = new RunEthSpeedUpTask();

			break;
		case "6":
			RunEthCancelTransaction runEthCancelTransaction = new RunEthCancelTransaction();
			break;

		case "7":
			new EthTools().launchNode();
			displayMenu();
			break;

		case "8":
			RunOpenSeaLister runOsLister = new RunOpenSeaLister();
			break;

		case "9":
			MainMenu newSession = new MainMenu(false);
			break;

		default:
			System.out.println(ANSI_RED + "Invalid Input!" + ANSI_RESET);
			Thread.sleep(1100);
			displayMenu();
			break;

		}

	}
}
