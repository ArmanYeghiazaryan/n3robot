package modulesEth;

import java.util.Scanner;

public class RunEthGenerateWalletTask {

	public RunEthGenerateWalletTask() throws InterruptedException {
		runTasks();
	}

	public void runTasks() throws InterruptedException {
		System.out.print(ANSI_YELLOW + "\n>>> Set Wallet Amount: " + ANSI_RESET);

		Scanner in = new Scanner(System.in);
		String input = in.next();
		int amount = 0;

		try {
			amount = Integer.valueOf(input);
		} catch (Exception e) {
			System.out.println(ANSI_RED + "Invalid Input!\n\n" + ANSI_RESET);
			Thread.sleep(1000);
			runTasks();
		}

//		for (int i = 1; i <= amount; i++) {

		EthGenerateWalletTask ethGenerateWalletTask = new EthGenerateWalletTask("ZETA Bot", amount);
		ethGenerateWalletTask.start();

//			if (i % 10 == 0) {
//				Thread.sleep(15000);
//			}
//		}

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
