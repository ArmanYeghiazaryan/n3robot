package modulesSol;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONObject;
import org.json.JSONTokener;

import main.BackendWebhook;
import main.DiscordWebhook;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class SolanaTxTask extends Thread {

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	private List<String> list;
	private List<String> checkedTX;

	private Session session = Requests.session();
	private String sender;
	private String price;
	private String publicKey;

	private String imageUrl;
	private String webhookUrl;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public SolanaTxTask(String sender, String publicKey, String price, String webhookUrl) {
		this.sender = sender;
		this.publicKey = publicKey;
		this.price = price;
		this.webhookUrl = webhookUrl;
		this.list = new ArrayList<>(Arrays.asList());
		this.checkedTX = new ArrayList<>(Arrays.asList());

	}

	public void run() {

		while (true) {
			try {
				checkTxStatus();
				Thread.sleep(1000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
			}

		}

	}

	public void checkTxStatus() {

		for (String tx : list) {
			try {

				if (checkedTX.contains(tx)) {
					continue;
				}

				RawResponse newSession = session.get("https://public-api.solscan.io/transaction/" + tx)
						.socksTimeout(60_000).connectTimeout(60_000).send();

				String response = newSession.readToText();

				JSONTokener tokener = new JSONTokener(response);
				JSONObject object = new JSONObject(tokener);
				String status = object.getString("status").replace("\"", "");

				if (status.equals("Success") || status.equals("success")) {

					System.out.println(ANSI_GREEN + "[SOLANA] - [" + dtf.format(now.now())
							+ "] - TRANSACTION SUCCEEDED: " + tx + ANSI_RESET);
//					list.remove(tx);
					BackendWebhook b = new BackendWebhook("SOL CM Mint", "**Price:** " + price + " SOL", null,
							"https://discord.com/api/webhooks/951536933664870502/dwZ3guOyZFo3Bi1EXTfm-n3mbIWf-iY2EpZE53YEXo6Wx5FezNtA3gI4JtyUsB3vemHn");
					sendWebhook(tx);
					checkedTX.add(tx);

				} else if (status.equals("Fail") || status.equals("fail")) {
//					list.remove(tx);
					System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - TRANSACTION FAILED: "
							+ tx + ANSI_RESET);
					checkedTX.add(tx);

				}

			} catch (Exception e) {

			} finally {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

			}

		}

	}

	public void addTx(String tx) {
		this.list.add(tx);
	}

	public void sendWebhook(String tx) throws IOException, LineUnavailableException, InterruptedException {

		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

		webhook.setUsername("ZETA AIO");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/839821906881806357/957787704156905482/zeta_logo_square.png");
		webhook.setTts(false);

		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("SUCCESSFULLY MINTED SOLANA NFT")
//				.setThumbnail(imageUrl)
				.setUrl("https://public-api.solscan.io/transaction/" + tx).addField("From", "||" + sender + "||", false)
				.addField("Value", "" + price + " SOL", true)

				.setFooter(dtf.format(now.now()) + " | @zeta_aio", null).setColor(new Color(43, 46, 58)));

		try {
			webhook.execute();
			System.out.println("[SOLANA] - [" + dtf.format(now.now()) + "] - Sent Webhook.");

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[SOLANA] - [" + dtf.format(now.now()) + "] - Send Webhook Failed: "
					+ e.getMessage() + ANSI_RESET);
			Thread.sleep(10000);
			sendWebhook(tx);

		}
	}
}
