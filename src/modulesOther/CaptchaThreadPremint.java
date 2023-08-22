package modulesOther;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.HCaptcha;
import com.twocaptcha.captcha.ReCaptcha;

import io.github.alperensert.capmonster_java.tasks.RecaptchaV2Task;

public class CaptchaThreadPremint extends Thread {

	private String publicAddress;
	private String captchaUrl;
	private String captchaSiteKey;
	private HashSet<String> twoCaptchaKeys;
	private String captchaToken;
	private boolean stop;
	private long nextScrape = 0;
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static LocalDateTime now = LocalDateTime.now();

	public CaptchaThreadPremint(String publicAddress, String captchaUrl, String captchaSiteKey,
			HashSet<String> twoCaptchaKeys) {
		this.publicAddress = publicAddress;
		this.captchaUrl = captchaUrl;
		this.captchaSiteKey = captchaSiteKey;
		this.twoCaptchaKeys = twoCaptchaKeys;
	}

	public void run() {

		while (!stop) {
			try {
				long currentTime = Instant.now().getEpochSecond();

				if (captchaToken == null || currentTime > nextScrape) {
					requestCaptcha();
				} else {
					Thread.sleep(4000);
				}
			} catch (Exception e) {
				System.out.println(ANSI_RED + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - ["
						+ Thread.currentThread().getName() + "] - Captcha Thread Error: " + e.toString() + ANSI_RESET);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}

	}

	public void stopHarvestor() {
		this.stop = true;
		System.out.println(ANSI_GREY + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - [" + publicAddress
				+ "] - Captcha Thread Stopped!" + ANSI_RESET);
	}

	public String getRandom2CaptchaKey() {
		List<String> asList = new ArrayList<String>(twoCaptchaKeys);
		Collections.shuffle(asList);
		return String.valueOf(asList.get(0));
	}

	public void requestCaptcha() throws Exception {

		String response = "";

		try {
			System.out.println(ANSI_GREY + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - [" + publicAddress
					+ "] - Requesting Captcha..." + ANSI_RESET);
			RecaptchaV2Task recaptchaV2Task = new RecaptchaV2Task(getRandom2CaptchaKey());
			RecaptchaV2Task.TaskBuilder taskBuilder = new RecaptchaV2Task.TaskBuilder(this.captchaUrl,
					this.captchaSiteKey);

			System.out.println(ANSI_GREY + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - [" + publicAddress
					+ "] - Waiting For Captcha..." + ANSI_RESET);
			int taskId = recaptchaV2Task.createTask(taskBuilder);
			JSONObject result = recaptchaV2Task.joinTaskResult(taskId);

			response = new JSONObject(new JSONTokener(result.toString())).getString("gRecaptchaResponse");

			System.out.println(ANSI_CYAN + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - [" + publicAddress
					+ "] - Received Captcha!" + ANSI_RESET);

		} catch (Exception e) {
			System.out.println(ANSI_RED + "[CAPTCHA] - [" + dtf.format(now.now()) + "] - [" + publicAddress
					+ "] - Captcha Error: " + e.getMessage() + ANSI_RESET);
			Thread.sleep(2500);
			requestCaptcha();

		}

		this.captchaToken = response;
		this.nextScrape = Instant.now().getEpochSecond() + 60;
	}

	public String getCaptchaToken() throws Exception {
		while (this.captchaToken == null) {
			Thread.sleep(1000);
		}

		String temp = captchaToken;
		this.captchaToken = null;
		return temp;
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
