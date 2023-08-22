package main;

import modulesEth.EthTools;

public class Worker extends Thread {
	private static CLITools cliTools = new CLITools(false);
	private boolean displayMainHeader = true;
	private String username;
	private String botVersion;

	public Worker() {

	}

	public void run() {

		while (true) {

			if (displayMainHeader) {
				try {
					while (this.username == null) {
						Thread.sleep(500);
					}
					cliTools.setTitle("N3RO BOT - " + new EthTools().getRapidAndBlock());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}

		}
	}

	public void setData(String username, String botVersion) {
		this.username = username;
		this.botVersion = botVersion;
	}

	public void disableMainHeader() {
		this.displayMainHeader = false;
	}

	public void displayDownload() {

	}

}
