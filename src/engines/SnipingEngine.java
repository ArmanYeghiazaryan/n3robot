package engines;

import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import main.Main;
import main.MainMenu;
import modulesEth.RunEthMintingTask;
import modulesEth.RunOpenSeaSniperTask;
import modulesSol.RunMagicEdenSnipingTask;
import modulesSol.RunSolanaTools;

public class SnipingEngine extends Main {

	public SnipingEngine() throws Exception {

		displayMenu();
	}

	public void displayMenu() throws Exception {

//		printLogo();

		System.out.println(ANSI_WHITE + "\n> 1. OpenSea Sniper" + ANSI_RESET);
//		System.out.println("> 2. MagicEden Sniper" + ANSI_RESET);
		System.out.println(ANSI_GREY + "> 2. Back To Overview" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		switch (input) {
		case "1":
			RunOpenSeaSniperTask runOpenSeaSniperTask = new RunOpenSeaSniperTask();
			break;
//		case "2":
//			RunMagicEdenSnipingTask runMagicEdenSnipingTask = new RunMagicEdenSnipingTask(true);
//			break;

		case "2":
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
