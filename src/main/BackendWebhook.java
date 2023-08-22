package main;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackendWebhook extends Thread {

	private String title;
	private String body;
	private String content;
	private String webhookLink;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public BackendWebhook(String title, String body, String content, String webhookLink) {
		this.title = title;
		this.body = body;
		this.content = content;
		this.webhookLink = webhookLink;
		run();
	}

	public void run() {
		DiscordWebhook webhook = new DiscordWebhook(webhookLink);

		webhook.setUsername("N3RO BOT");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png");

		webhook.setTts(false);
		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(title)
				.setThumbnail(
						"https://media.discordapp.net/attachments/959444733283958864/971729374715994152/nero_logo.png")
				.setFooter(dtf.format(now.now()) + " | @n3robot", null).setColor(new Color(43, 46, 58))
				.setDescription(body));

		if (this.content != null) {
			webhook.setContent(content);
		}

		try {
			webhook.execute();

		} catch (Exception e) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			run();

		}
	}
}
