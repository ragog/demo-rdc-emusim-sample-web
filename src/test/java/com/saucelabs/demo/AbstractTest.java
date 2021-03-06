package com.saucelabs.demo;

import com.saucelabs.demo.util.ResultReporter;
import com.saucelabs.saucerest.SauceREST;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by grago on 27.09.17.
 */
public class AbstractTest {

    private AppiumDriver driver;
    protected SwagCloset swagCloset;
    private ResultReporter reporter;

    private String sauceURI = "@ondemand.saucelabs.com:443";
    private String buildTag = System.getenv("BUILD_TAG");
    private String username = System.getenv("SAUCE_USERNAME");
    private String accesskey = System.getenv("SAUCE_ACCESS_KEY");
    private String rdcApiKey = System.getenv("RDC_API_KEY");
    private String extendedDebugging = System.getenv("EXT_DEBUGGING");
    private SauceREST sauceRESTClient = new SauceREST(username, accesskey);

    @BeforeMethod
    @Parameters({ "deviceType", "platformName", "platformVersion", "deviceName" })
    public void setup(String deviceType, String platformName, String platformVersion, String deviceName, Method method) throws MalformedURLException {

        String testName = method.getName();

        String testId = UUID.randomUUID().toString();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        String gridEndpoint = "";

        if (deviceType.equals("real")) {

            capabilities.setCapability("testobject_api_key", rdcApiKey);
            capabilities.setCapability("phoneOnly", "true");
            if (!deviceName.isEmpty()) {
                capabilities.setCapability("deviceName", deviceName);
            }
            gridEndpoint = "https://eu1.appium.testobject.com/wd/hub";

        } else if (deviceType.equals("virtual")) {

            if (buildTag != null) {
                capabilities.setCapability("build", buildTag);
            }

            capabilities.setCapability("deviceName", deviceName);
            capabilities.setCapability("browserName", "chrome");
            capabilities.setCapability("extendedDebugging", extendedDebugging);

            gridEndpoint = "https://" + username + ":" + accesskey + sauceURI + "/wd/hub";

        }

        capabilities.setCapability("platformName", platformName);
        capabilities.setCapability("platformVersion", platformVersion);
        capabilities.setCapability("phoneOnly", "true");
        capabilities.setCapability("name", testName);

        capabilities.setCapability("uuid", testId);

        driver = new AndroidDriver(new URL(gridEndpoint),
                capabilities);

        swagCloset = new SwagCloset(driver);
        reporter = new ResultReporter();

    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver != null) {
            String sessionId = driver.getSessionId().toString();
            boolean status = result.isSuccess();
            boolean isTOTest = driver.getCapabilities().getCapability("testobject_api_key") != null;

            if (isTOTest) {
                // TestObject REST API
                reporter = new ResultReporter();
                reporter.saveTestStatus(sessionId, status);

            } else { // test was run on Sauce
                // Sauce REST API (updateJob)
                Map<String, Object> updates = new HashMap<String, Object>();
                updates.put("passed", status);
                sauceRESTClient.updateJobInfo(sessionId, updates);
            }

            driver.quit();
        }

    }

}
