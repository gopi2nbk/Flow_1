package flows;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.*;
import com.google.gson.Gson;

import FlyModules.BrowserContants;
import FlyModules.Jazeera;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import pageObjects.BaseClass;
import pageObjects.Database;


public class J9_Flow{
	static WebDriver driver;
	private int iTestCaseRow;
	boolean status;
	private Database PnrDetails;
	public static String J9ApiUrl;
	
	@BeforeMethod
	public void setup() throws InterruptedException {
		        FirefoxOptions options = new
				FirefoxOptions();  
				options.addPreference("layout.css.devPixelsPerPx", "0.3");
				options.addPreference("permissions.default.image", 2);
				options.addArguments("--headless");
				driver = new FirefoxDriver(options);
				driver.manage().window().maximize();
				driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
				driver.manage().deleteAllCookies();

	}

	
	@Test 
	public void test() throws Exception {
		 
		if (BrowserContants.ENV.equals("PRD")) {
			RestAssured.baseURI = BrowserContants.PRD_API_URL;
			System.out.println(BrowserContants.PRD_API_URL);
			
		} else if (BrowserContants.ENV.equals("STG")) {
			RestAssured.baseURI = BrowserContants.STG_API_URL;
			System.out.println(BrowserContants.STG_API_URL);
		}
		
        LocalTime currentTime = LocalTime.now();
        
        int days;
        int skipdays;
        
        LocalTime startTime = LocalTime.of(23, 30); // 11 PM
        LocalTime endTime = LocalTime.of(5, 30); // 5 AM
        
        if ((currentTime.isAfter(startTime) && currentTime.isBefore(LocalTime.MIDNIGHT)) ||
            (currentTime.isAfter(LocalTime.MIDNIGHT) && currentTime.isBefore(endTime)) ||
            currentTime.equals(startTime) || currentTime.equals(endTime)) {
            days = 2;
            skipdays = 1;
        } else {
            days = 1;
            skipdays = 0;
        }
	    
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "text/json");
		Response response = request.get("/GetJ9Routes?days="+days+"&group=2&skipdays="+skipdays+"");
		System.out.println("Response body: " + response.body().asString());
		String s=response.body().asString();
		System.out.println(s);
		int statusCode = response.getStatusCode();
		System.out.println("The status code recieved: " + statusCode);
		
		Gson g = new Gson();
		Database[] mcArray = g.fromJson(s, Database[].class);
		List<Database> p = Arrays.asList(mcArray);
		for(Database data:p){
			
			
			
			try{ 
				Date depDate=new SimpleDateFormat("dd MMM yyyy").parse(data.DepartureDate);
            	SimpleDateFormat yearMonthFormatter = new SimpleDateFormat("yyyy-MM");
                String yearMonth = yearMonthFormatter.format(depDate);
                SimpleDateFormat dayFormatter = new SimpleDateFormat("dd");
                String date = dayFormatter.format(depDate);
				System.out.println("strDate :"+yearMonth+"-"+date);
				J9ApiUrl = "https://booking.jazeeraairways.com/en/search-flight?culture=en-kw&querystring=true&RadioButtonMarketStructure=OneWay&Origin1="+data.From+"&Destination1="+data.To+"&Day1="+date+"&MonthYear1="+yearMonth+"&ADT=1&INFANT=0&CHD=0&promoCode=&isflexible=false&EXTRASEAT=0&PAXDIS=";
                System.out.println("API URL: " + J9ApiUrl);
                PnrDetails = data;
               
                driver.get(J9ApiUrl);
				Thread.sleep(3000);
				new BaseClass(driver);
				Jazeera.FlightDetails2(driver,PnrDetails);
				//driver.quit();
				
				}
			
				catch(Exception e)
				{
					
				}
			}
		
		}
	
	

	 @AfterMethod
     public void stop() throws Exception
      {
		 
          if (driver != null) {
		        driver.quit();
		    }
  }

}	
	

		
		


				
				
		
		
			