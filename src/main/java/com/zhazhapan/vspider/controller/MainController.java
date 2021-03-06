/**
 * 
 */
package com.zhazhapan.vspider.controller;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import com.zhazhapan.util.Checker;
import com.zhazhapan.util.FileExecutor;
import com.zhazhapan.util.Formatter;
import com.zhazhapan.util.ThreadPool;
import com.zhazhapan.util.Utils;
import com.zhazhapan.util.dialog.Alerts;
import com.zhazhapan.vspider.App;
import com.zhazhapan.vspider.Crawler;
import com.zhazhapan.vspider.VsController;
import com.zhazhapan.vspider.models.CrawlConfig;
import com.zhazhapan.vspider.modules.constant.DefaultConfigValues;
import com.zhazhapan.vspider.modules.constant.Values;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * @author pantao
 *
 */
public class MainController {

	private Logger logger = Logger.getLogger(MainController.class);

	@FXML
	public TextArea logOut;

	@FXML
	public Label stautsLabel;

	@FXML
	private TextField crawlUrl;

	@FXML
	private CheckBox pictureCK;

	@FXML
	private CheckBox videoCK;

	@FXML
	private CheckBox linkCK;

	@FXML
	private CheckBox docCK;

	@FXML
	private CheckBox othersCK;

	@FXML
	public TextArea htmlContent;

	@FXML
	private Button toogleCrawling;

	@FXML
	private TextField numsTF;

	@FXML
	private TextField depthTF;

	@FXML
	private TextField pagesTF;

	@FXML
	private TextField delayTF;

	@FXML
	private CheckBox proxyCK;

	@FXML
	private TextField proxyServerTF;

	@FXML
	private TextField proxyPortTF;

	@FXML
	private TextField proxyUserTF;

	@FXML
	private PasswordField proxyPassPF;

	@FXML
	private TextField crawlFilterTF;

	@FXML
	private TextField downloadFilterTF;

	@FXML
	private CheckBox repeatCK;

	@FXML
	private TextField visitFilterTF;

	private boolean crawling = false;

	private static MainController mainController = null;

	/**
	 * 获取唯一实例
	 * 
	 * @return
	 */
	public static MainController getInstance() {
		return mainController;
	}

	/**
	 * 初始化，由JavaFX调用
	 */
	@FXML
	private void initialize() {
		logger.info("init main window");
		// 创建唯一实例
		mainController = this;
		App.mainController = mainController;

		// 邦定模型数据
		pictureCK.selectedProperty().bindBidirectional(CrawlConfig.getCrawlImages());
		videoCK.selectedProperty().bindBidirectional(CrawlConfig.getCrawlVideos());
		linkCK.selectedProperty().bindBidirectional(CrawlConfig.getCrawlLinks());
		docCK.selectedProperty().bindBidirectional(CrawlConfig.getCrawlDocs());
		othersCK.selectedProperty().bindBidirectional(CrawlConfig.getCrawlOthers());

		numsTF.textProperty().bindBidirectional(CrawlConfig.getNumberOfCrawlers());
		depthTF.textProperty().bindBidirectional(CrawlConfig.getMaxDepthOfCrawling());
		pagesTF.textProperty().bindBidirectional(CrawlConfig.getMaxPagesToFetch());
		delayTF.textProperty().bindBidirectional(CrawlConfig.getPolitenessDelay());

		proxyCK.selectedProperty().bindBidirectional(CrawlConfig.getTurnOnProxy());
		proxyServerTF.textProperty().bindBidirectional(CrawlConfig.getProxyServer());
		proxyPortTF.textProperty().bindBidirectional(CrawlConfig.getProxyPort());
		proxyUserTF.textProperty().bindBidirectional(CrawlConfig.getProxyUser());
		proxyPassPF.textProperty().bindBidirectional(CrawlConfig.getProxyPass());

		repeatCK.selectedProperty().bindBidirectional(CrawlConfig.getRepeatCrawl());

		downloadFilterTF.textProperty().addListener((ob, oldValue, newValue) -> {
			try {
				App.downloadFilterPattern = Pattern.compile(newValue);
			} catch (Exception e) {
				// do nothing
			}
		});

		crawlFilterTF.textProperty().addListener((ob, oldValue, newValue) -> {
			try {
				App.crawlFilterPattern = Pattern.compile(newValue);
			} catch (Exception e) {
				// do nothing
			}
		});

		visitFilterTF.textProperty().addListener((ob, oldValue, newValue) -> {
			try {
				App.visitFilterPattern = Pattern.compile(newValue);
			} catch (Exception e) {
				// do nothing
			}
		});
	}

	/**
	 * 退出程序
	 */
	public void exit() {
		if (crawling) {
			Optional<ButtonType> result = Alerts.showConfirmation(Values.MAIN_TITLE, Values.EXIT_CRAWLING);
			if (result.get() != ButtonType.OK) {
				return;
			}
		}
		saveLog();
		System.exit(0);
	}

	/**
	 * 保存爬取日志
	 */
	public void saveLog() {
		FileExecutor executor = new FileExecutor();
		String visitingLog = htmlContent.getText();
		if (Checker.isNotEmpty(visitingLog) && visitingLog.contains(Values.VISITING_TIP)) {
			executor.saveFile(App.DOWNLOAD_FOLDER + Values.SEPARATOR + "visiting.log", visitingLog, true);
		}
		String downloadingLog = logOut.getText();
		if (Checker.isNotEmpty(downloadingLog) && downloadingLog.contains(Values.DOWNLOADING_TIP)) {
			executor.saveFile(App.DOWNLOAD_FOLDER + Values.SEPARATOR + "downloading.log", downloadingLog, true);
		}
	}

	/**
	 * 爬取URL
	 */
	public void toCrawl() {
		if (crawling) {
			// 暂停爬虫
			crawling = false;
			toogleCrawling.setText(Values.CRAWLER_START);
			stautsLabel.setText("crawler suspend");
			App.controller.shutdown();
		} else {
			// 开始爬虫
			if (!Checker.isHyperLink(crawlUrl.getText())) {
				String html = htmlContent.getText();
				if (Checker.isNotEmpty(html)) {
					ThreadPool.executor.submit(() -> new Crawler().downloadURL("", html));
				}
				return;
			}
			crawling = true;
			toogleCrawling.setText(Values.CRAWLER_STOP);
			logger.info("start to crawl urls: " + crawlUrl.getText());
			stautsLabel.setText("starting......");
			// 读取爬虫配置
			String[] urls = crawlUrl.getText().split(" ");
			App.domains = new String[urls.length];
			for (int i = 0; i < urls.length; i++) {
				String url = urls[i].replaceAll("https?://", "");
				if (url.contains("/")) {
					App.domains[i] = url.substring(0, url.indexOf("/") + 1);
				} else {
					App.domains[i] = url;
				}
			}
			int numNon = Formatter.stringToInt(CrawlConfig.getNumberOfCrawlers().get());
			int num = numNon < 1 ? DefaultConfigValues.NUMBER_OF_CRAWLERS : numNon;
			int depNon = Formatter.stringToInt(CrawlConfig.getMaxDepthOfCrawling().get());
			int dep = depNon < 1 || depNon > DefaultConfigValues.MAX_DEPTH_OF_CRAWLING
					? DefaultConfigValues.MAX_DEPTH_OF_CRAWLING
					: depNon;
			int pagNon = Formatter.stringToInt(CrawlConfig.getMaxPagesToFetch().get());
			int pag = pagNon < 1 ? Integer.MAX_VALUE : pagNon;
			int delNon = Formatter.stringToInt(CrawlConfig.getPolitenessDelay().get());
			int del = delNon < 1 ? DefaultConfigValues.POLITENESS_DELAY : delNon;
			ThreadPool.executor.submit(() -> {
				// 开启爬虫
				App.controller.init(num, dep, pag, del, urls);
				boolean res = App.controller.start();
				finished(res);
			});
		}
	}

	private void finished(boolean start) {
		Platform.runLater(() -> {
			if (start) {
				stautsLabel.setText("finished");
			} else {
				stautsLabel.setText("start error");
			}
			crawling = false;
			toogleCrawling.setText(Values.CRAWLER_START);
			repeatCrawl();
		});
	}

	private void repeatCrawl() {
		int threads = Formatter.stringToInt(CrawlConfig.getNumberOfCrawlers().get());
		if (threads > 5) {
			threads = 5;
		}
		String[] urls = htmlContent.getText().replaceAll(Values.VISITING_TIP, "").split("\n");
		Crawler crawler = new Crawler();
		for (int i = 0; i < threads; i++) {
			ThreadPool.executor.submit(() -> {
				while (CrawlConfig.getRepeatCrawl().get()) {
					for (String url : urls) {
						try {
							crawler.downloadURL(url, Jsoup.connect(url).execute().body());
							logger.info("repeat visiting url: " + url);
							Platform.runLater(() -> stautsLabel.setText("validating url: " + url));
						} catch (IOException e) {
							logger.error("something wrong when repeat crawler, message: " + e.getMessage());
						}
					}
					try {
						Thread.sleep(Formatter.stringToLong(CrawlConfig.getPolitenessDelay().get()));
					} catch (InterruptedException e) {
						logger.error("sleep thread error: " + e.getMessage());
					}
				}
			});
		}
	}

	/**
	 * 重置爬虫
	 */
	public void reset() {
		deleteFrontier();
		App.controller.shutdown();
		ThreadPool.executor.shutdownNow();
		saveLog();
		htmlContent.clear();
		logOut.clear();
		crawlUrl.clear();
		crawling = false;
		toogleCrawling.setText(Values.CRAWLER_START);
		App.initThreadPool();
		App.controller = new VsController();
	}

	private void deleteFrontier() {
		deleteFile(new File(DefaultConfigValues.CRAWL_STORAGE_FOLDER + Values.SEPARATOR + "frontier"));
	}

	/**
	 * 删除文件夹或文件
	 * 
	 * @param file
	 * @return
	 */
	public boolean deleteFile(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String[] children = file.list();
				if (Checker.isNotNull(children)) {
					for (int i = 0; i < children.length; i++) {
						deleteFile(new File(file, children[i]));
					}
				}
			}
			return file.delete();
		} else {
			return false;
		}
	}

	/**
	 * 打开存储目录
	 */
	public void openStorageFolder() {
		Utils.openFile(DefaultConfigValues.CRAWL_STORAGE_FOLDER);
	}

	/**
	 * 回车确认访问链接
	 * 
	 * @param event
	 */
	public void urlEnter(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			toCrawl();
		}
	}
}
