package engines;

import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import main.Main;
import main.MainMenu;
import modulesDiscord.RunDiscordTools;
import modulesEth.RunEthMintingTask;
import modulesSol.RunMagicEdenSnipingTask;
import modulesSol.RunSolanaTools;

public class DiscordEngine extends Main {

	public DiscordEngine() throws Exception {

		displayMenu();
	}

	public void displayMenu() throws Exception {

//		printLogo();
		// USERNAME_CHANGER

		System.out.println(ANSI_GREY + "\n> Server Tools" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 1. Server Joiner");
		System.out.println("> 2. Server Leaver" + ANSI_RESET);

		System.out.println(ANSI_GREY + "\n> Channel Tools" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 3. XP Grinder");
		System.out.println("> 4. Reaction Adder" + ANSI_RESET);

		System.out.println(ANSI_GREY + "\n> Account Tools" + ANSI_RESET);
		System.out.println(ANSI_WHITE + "> 5. Account Checker");
		System.out.println("> 6. Username Changer");
		System.out.println("> 7. Profile Picture Changer");

		System.out.println("\n> 8. Manual Mode" + ANSI_RESET);

		System.out.println(ANSI_GREY + "\n> 9. Back To Overview" + ANSI_RESET);

		System.out.print(ANSI_YELLOW + "\n>>> Select: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();

		switch (input) {

		case "1":
			RunDiscordTools runServerJoiner = new RunDiscordTools("SERVER_JOINER");
			break;
		case "2":
			RunDiscordTools runServerLeaver = new RunDiscordTools("SERVER_LEAVER");
			break;

		case "3":
			RunDiscordTools runXPGrinder = new RunDiscordTools("XP_GRINDER");
			break;

		case "4":
			RunDiscordTools runReactionAdder = new RunDiscordTools("REACTION_ADDER");
			break;

		case "5":
			RunDiscordTools runAccountChecker = new RunDiscordTools("ACCOUNT_CHECKER");
			break;

		case "6":
			RunDiscordTools runUsernameChangeTasks = new RunDiscordTools("USERNAME_CHANGER");
			break;

		case "7":
			RunDiscordTools runProfilePicTasks = new RunDiscordTools("PROFILE_PIC_CHANGER");
			break;

		case "8":
			RunDiscordTools runManualMode = new RunDiscordTools("MANUAL_MODE");
			break;

		case "9":
			MainMenu newSession = new MainMenu(false);
			break;

		default:
			System.out.println(ANSI_CYAN + "... Invalid Input!" + ANSI_RESET);
			Thread.sleep(1100);
			displayMenu();
			break;

		}

	}

}
