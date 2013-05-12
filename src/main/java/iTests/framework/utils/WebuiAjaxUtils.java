package iTests.framework.utils;

import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.util.concurrent.TimeUnit;

public class WebuiAjaxUtils {

	public WebElement find(By by, WebDriver driver) {
		return find(by, driver, 30 * 1000, 500);
	}

	public WebElement find(final By by, WebDriver driver, long timeout, long pollEvery) {
		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(timeout, TimeUnit.MILLISECONDS)
			.pollingEvery(pollEvery, TimeUnit.MILLISECONDS)
			.ignoring(NoSuchElementException.class);

		WebElement foo = wait.until(new Function<WebDriver, WebElement>() {
			public WebElement apply(WebDriver driver) {
				return driver.findElement(by);
			}
		});
		return foo;
	}
}
