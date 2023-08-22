package engines;

import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import main.Main;
import main.MainMenu;
import modulesEth.RunEthMintingTask;
import modulesSol.RunSolanaTools;

public class MintingEngine extends Main {

	public MintingEngine() throws Exception {

		displayMenu();
	}

	public void displayMenu() throws Exception {

//		printLogo();

		System.out.println(ANSI_WHITE + "\n> 1. ETH Minting - Start From TX Input" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 2. ETH Minting - Start From Manual Input" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 3. ETH Minting - Start From Minting File" + ANSI_RESET);

//		System.out.println("> 2. Solana Candy Machine Minting" + ANSI_RESET);
		System.out.println(ANSI_GREY + "\n> 4. Back To Overview" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		switch (input) {
		case "1":
			RunEthMintingTask runEthMintingTaskTX = new RunEthMintingTask();
			runEthMintingTaskTX.runTasksFromTX();
			break;
		case "2":
			RunEthMintingTask runEthMintingTaskInput = new RunEthMintingTask();
			runEthMintingTaskInput.runTasksFromInput();
			break;
		case "3":
			RunEthMintingTask runEthMintingTaskFile = new RunEthMintingTask();
			runEthMintingTaskFile.runTasksFromFile();
			break;
		case "4":
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
