package modulesOther;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import main.CLITools;
import main.Main;
import modulesEth.EthGenerateWalletTask;
import modulesEth.EthTopupWalletTask;

public class RunTwitterTask extends Main {

	public RunTwitterTask() throws Exception {
		disableMainHeader();

	}

	public void runFollower() throws Exception {

		List<TwitterAppTask> tasksList = new ArrayList<TwitterAppTask>();
		CLITools cliTools = new CLITools(false);

		String consumer = getValueFromConfig("Twitter Consumer-Key");
		String consumerSecret = getValueFromConfig("Twitter Consumer-Key-Secret");

		System.out.print(ANSI_YELLOW + "\n>>> Set Target Username: " + ANSI_RESET);
		Scanner userInput = new Scanner(System.in);
		String userToBeFollowed = userInput.nextLine();

		userToBeFollowed = userToBeFollowed.replace("@", "");

		Path path = Paths.get(System.getProperty("user.dir") + "\\target\\twitterTokens.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\target\\twitterTokens.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		int tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO BOT - Twitter Follower - Status: " + 0 + "/" + tasksAmount);
		Thread.sleep(1000);

		for (CSVRecord record : records) {

			String screenName = record.get("USERNAME");
			String accessToken = record.get("TOKEN");
			String accessTokenSecret = record.get("TOKEN_SECRET");
			TwitterAppTask tw = new TwitterAppTask(consumer, consumerSecret, screenName, accessToken, accessTokenSecret,
					userToBeFollowed);
			tw.start();

			tasksList.add(tw);
			Thread.sleep(getDelayInMs());

		}

		int finishedTasks = 0;

		while (finishedTasks != tasksAmount) {
			for (TwitterAppTask task : tasksList) {

				if (task.isFinished()) {
					finishedTasks++;
					cliTools.setTitle("N3RO BOT - Twitter Follower - Status: " + finishedTasks + "/" + tasksAmount);
				}

				Thread.sleep(500);

			}
		}

	}

	public void runInitializer() throws Exception {

		TwitterAppTask tw = new TwitterAppTask(getValueFromConfig("Twitter Consumer-Key"),
				getValueFromConfig("Twitter Consumer-Key-Secret"));
		tw.initializeAccounts();

	}

}
