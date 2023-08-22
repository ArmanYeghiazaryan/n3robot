package modulesEth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import main.Main;

public class RunCustomSig extends Main {

	public RunCustomSig() throws InterruptedException, IOException {
		runTasks();
	}

	public void runTasks() throws InterruptedException, IOException {

		while (true) {

			// Get fromWallet. Check if it is in file or not.
			System.out.println(ANSI_CYAN + ">>> Type Wallet Public Address..." + ANSI_RESET);

			Scanner inScanner = new Scanner(System.in);
			String input = inScanner.nextLine();

			CustomSignature x = new CustomSignature(input);
			x.start();
			while (!x.isFinished()) {
				Thread.sleep(2000);
			}
		}

	}

}
