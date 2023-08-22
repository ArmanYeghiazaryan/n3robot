package engines;

import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import main.Main;
import main.MainMenu;
import modulesEth.RunEthMintingTask;
import modulesEth.RunOpenSeaSniperTask;
import modulesOther.RunHublotTask;
import modulesOther.RunPremintTask;
import modulesOther.RunTwitterTask;
import modulesOther.TwitterAppTask;
import modulesSol.RunMagicEdenSnipingTask;
import modulesSol.RunSolanaTools;

public class OtherEngine extends Main {

	public OtherEngine() throws Exception {

		displayMenu();
	}

	public void displayMenu() throws Exception {

//		printLogo();

		System.out.println(ANSI_GREY + "\nPREMINT" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 1. Premint Initializer");
		System.out.println("> 2. Premint Checker");
		System.out.println("> 3. Premint Entries");
		System.out.println("> 4. Premint Train" + ANSI_RESET);

		System.out.println(ANSI_CYAN + "\nTWITTER" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 5. Twitter Initializer");
		System.out.println("> 6. Twitter Follower" + ANSI_RESET);

//		System.out.println(ANSI_WHITE + "\n> 7. 1800Flowers Raffle" + ANSI_RESET);

		System.out.println(ANSI_PURPLE + "\n> 7. Murakami x Hublot" + ANSI_RESET);

		System.out.println(ANSI_GREY + "\n> 8. Back To Overview" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		switch (input) {

		case "1":
			RunPremintTask runPremintInit = new RunPremintTask();
			runPremintInit.runInitializer();
			;
			break;
		case "2":
			RunPremintTask runPremintChecker = new RunPremintTask();
			runPremintChecker.runWinChecker();
			;
			break;

		case "3":
			RunPremintTask runPremintEntries = new RunPremintTask();
			runPremintEntries.runRaffleTasks();
			;

			break;

		case "4":
			RunPremintTask runPremintTrain = new RunPremintTask();
			runPremintTrain.runTrainTasks();
			;
			break;

		case "5":
			RunTwitterTask tw = new RunTwitterTask();
			tw.runInitializer();
			break;

		case "6":
			RunTwitterTask tw2 = new RunTwitterTask();
			tw2.runFollower();
			break;

		case "7":
			RunHublotTask run = new RunHublotTask();
			run.runRaffleTasks();
			break;

		case "8":
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
