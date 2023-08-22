package modulesOther;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.web3j.crypto.Credentials;

import main.CLITools;
import main.Main;
import modulesEth.EthMintingTask;
import modulesEth.EthTopupWalletTask;
import net.dongliu.requests.Cookie;

public class RunHublotTask extends Main {

	List<PremintTask> tasksList = new ArrayList<PremintTask>();
	CLITools cliTools = new CLITools(false);
	int tasksAmount;

	public RunHublotTask() throws Exception {
//		runRaffleTasks();
		disableMainHeader();

	}

	public void runRaffleTasks() throws InterruptedException, IOException {

		Path path = Paths.get(System.getProperty("user.dir") + "\\tasks\\other\\tasksHublot.csv");
		Reader in = new FileReader(System.getProperty("user.dir") + "\\tasks\\other\\tasksHublot.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);

		this.tasksAmount = Integer.valueOf((int) Files.lines(path).count() - 1);

		System.out.println(ANSI_YELLOW + "\nInitializing " + tasksAmount + " task(s)...\n" + ANSI_RESET);
		cliTools.setTitle("N3RO BOT - Hublot Entries - Tasks Loaded: " + this.tasksAmount);

		Thread.sleep(1000);
		int taskId = 0;

		for (CSVRecord record : records) {
			taskId++;
			String email = record.get("EMAIL");
			String name = record.get("FIRST_NAME");
			String lastName = record.get("LAST_NAME");
			String phoneCountry = record.get("PHONE_COUNTRY");
			String phonePrefix = record.get("PHONE_PREFIX");
			String phoneNr = record.get("PHONE_NR");

			HublotRaffle task = new HublotRaffle(taskId, "random", email, name, lastName, phoneCountry, phonePrefix,
					phoneNr);
			task.start();

		}

		in.close();

	}

}
