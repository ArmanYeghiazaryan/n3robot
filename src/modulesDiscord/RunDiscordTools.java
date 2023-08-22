package modulesDiscord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import main.CLITools;
import main.Main;
import modulesEth.OpenSeaSniperTask;
import modulesEth.EthMintingTask;
import net.dongliu.requests.Cookie;

public class RunDiscordTools extends Main {
	CLITools cliTools = new CLITools(false);
	private int tasksAmount;

	public RunDiscordTools(String type) throws Exception {
		if (type.equals("XP_GRINDER")) {
			runXPGrinderTasks();

		} else if (type.equals("ACCOUNT_CHECKER")) {
			runAccountCheckerTasks();

		} else if (type.equals("SERVER_JOINER")) {
			runServerJoinerTasks();

		} else if (type.equals("REACTION_ADDER")) {
			runReactionAdderTasks();

		} else if (type.equals("SERVER_LEAVER")) {
			runServerLeaverTasks();

		} else if (type.equals("PROFILE_PIC_CHANGER")) {
			runProfilePicTasks();

		} else if (type.equals("USERNAME_CHANGER")) {
			runUsernameChangeTasks();

		} else if (type.equals("MANUAL_MODE")) {
			runManualMode();

		}
	}

	public void runAccountCheckerTasks() throws Exception {

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksAccountChecker.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksAccountChecker.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		for (CSVRecord record : records) {
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));
			String checkServerRoles = record.get("CHECK_SERVER_ROLES [TRUE / FALSE]");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");

			DiscordAccountCheckerTask task = new DiscordAccountCheckerTask(discordToken, checkServerRoles, proxy);
			task.start();
		}
	}

	public void runXPGrinderTasks() throws Exception {

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksXPGrinder.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksXPGrinder.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		for (CSVRecord record : records) {
			String serverID = record.get("SERVER_ID");
			String channelID = record.get("CHANNEL_ID");
			String csvMessage = record.get("ANSWER [AI / FILE / MESSAGE]");
			String delayInSeconds = record.get("DELAY_IN_SECONDS");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String webhookUrl = getWebhookUrl();

			DiscordXPGrinderTask task = new DiscordXPGrinderTask(serverID, channelID, csvMessage, delayInSeconds, proxy,
					discordToken);
			task.start();

			Thread.sleep(getDelayInMs());

		}
		in.close();

	}

	public void runReactionAdderTasks() throws Exception {

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksReactionAdder.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksReactionAdder.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		for (CSVRecord record : records) {
			String channelID = record.get("CHANNEL_ID");
			String messageID = record.get("MESSAGE_ID");
			String emote = record.get("REACTION [EMOJI_NAME / EMOTE_NAME]");
			String waitForRole = record.get("WAIT_FOR_NEW_ROLE [TRUE / FALSE]");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));

			DiscordReactionAdder task = new DiscordReactionAdder(channelID, messageID, emote, waitForRole, discordToken,
					proxy);
			task.start();
		}
		in.close();

	}

	public void runServerJoinerTasks() throws Exception {

		// ---
		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerJoiner.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerJoiner.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		List<TwitterMonitorTask> twitterTasksList = new ArrayList<TwitterMonitorTask>();

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);
		int id = 0;
		for (CSVRecord record : records) {
			String serverInvite = record.get("SERVER_INVITE [INVITE / WAIT / @TWITTER]");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String preHarvestCaptcha = record.get("PRE_HARVEST_CAPTCHA [TRUE / FALSE]");
			HashSet<String> twoCaptchaKeys = get2CaptchaKeys();
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));
			String webhookUrl = getWebhookUrl();
			TwitterMonitorTask twitterMonitorTask = null;
			HashSet<String> twitterTokens = getTwitterTokens();

			// Check if its Twitter Task
			if (serverInvite.charAt(0) == '@') {
				boolean found = false;
				for (TwitterMonitorTask currentTask : twitterTasksList) {
					if (serverInvite.toLowerCase().equals(currentTask.getTag().toLowerCase())) {
						twitterMonitorTask = currentTask;
						found = true;
						break;
					}
				}

				if (!found) {

					TwitterMonitorTask newTask = new TwitterMonitorTask(serverInvite, twitterTokens);
					twitterTasksList.add(newTask);
					twitterMonitorTask = newTask;
					newTask.start();
				}
			}

			DiscordJoinerTask task = new DiscordJoinerTask(id, serverInvite, proxy, preHarvestCaptcha, twoCaptchaKeys,
					discordToken, webhookUrl, twitterMonitorTask);
			task.start();
			id++;

			Thread.sleep(getDelayInMs());
		}

		in.close();
	}

	public void runServerLeaverTasks() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerLeaver.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksServerLeaver.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);
		int id = 0;
		for (CSVRecord record : records) {
			String serverID = record.get("SERVER_ID [ALL / SERVERID]");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));

			DiscordLeaverTask task = new DiscordLeaverTask(serverID, proxy, discordToken);
			task.start();
			id++;
		}

		in.close();
	}

	public void runProfilePicTasks() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksProfilePictureChanger.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksProfilePictureChanger.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);
		for (CSVRecord record : records) {
			String providedImage = record.get("PROFILE_PICTURE [RANDOM / FILE_NAME]");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));

			DiscordProfilePicTask task = new DiscordProfilePicTask(providedImage, proxy, discordToken);
			task.start();
		}

		in.close();
	}

	public void runUsernameChangeTasks() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksUsernameChanger.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksUsernameChanger.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		System.out.println(ANSI_YELLOW + "\nInitializing " + Integer.valueOf((int) Files.lines(path).count() - 1)
				+ " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);
		for (CSVRecord record : records) {
			String username = record.get("NEW_USERNAME");
			String password = record.get("PASSWORD");
			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));

			DiscordChangeUsernameTask task = new DiscordChangeUsernameTask(username, password, proxy, discordToken);
			task.start();
		}

		in.close();
	}

	public void runManualMode() throws Exception {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\discord\\tasksManualMode.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\discord\\tasksManualMode.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);
		disableMainHeader();

		cliTools.setTitle("N3RO BOT - Discord Manual Mode - Status: " + 0 + "/" + this.tasksAmount);

		System.out.println(ANSI_CYAN + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		Thread.sleep(1000);

		int counter = 0;

		for (CSVRecord record : records) {

			String proxy = record.get("PROXY [RANDOM / IP:PORT]");
			String discordToken = getDiscordTokenFromProfilesFile(record.get("DISCORD_TOKEN"));

			DiscordManualMode task = new DiscordManualMode(discordToken, proxy);
			task.start();
			counter++;
			cliTools.setTitle("N3RO BOT - Discord Manual Mode - Status: " + counter + "/" + this.tasksAmount);

			while (!task.isFinished()) {
				Thread.sleep(1500);
			}

		}

		in.close();
	}
}
