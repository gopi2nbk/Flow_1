package FlyModules;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import flows.J9_Flow;
import pageObjects.Database;
import pageObjects.PageUtils;

public class Jazeera extends J9_Flow  {
	
	static String Depdate=null;
	static String Currency=null;
	static String Departdate=null;
	static String Year =null;
	
	private static String getCurrentDateFormatted(String format) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return currentDate.format(formatter);
    }

    // Helper method to get the date after a certain number of days in the specified format
    private static String getDateAfterDaysFormatted(int days, String format) {
        LocalDate futureDate = LocalDate.now().plusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return futureDate.format(formatter);
    }
    
    public static String extractCurrencyType(String text) {
        // Assuming currency type is always three uppercase letters
        for (int i = 0; i < text.length() - 2; i++) {
            if (Character.isUpperCase(text.charAt(i)) && Character.isUpperCase(text.charAt(i + 1)) && Character.isUpperCase(text.charAt(i + 2))) {
                return text.substring(i, i + 3);
            }
        }
        return "KWD";
    }

    public static void FlightDetails2(WebDriver driver, Database PnrDetails) throws Exception {
        String date;
        
        
         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25)); // Set the maximum wait time to 60 seconds
		boolean isPageLoaded = false;
		int maxAttempts = 3;
		int attempt = 1;

		while (!isPageLoaded && attempt <= maxAttempts) {
		    try {
		        // Wait for the page to load completely
		        isPageLoaded = wait.until(ExpectedConditions.urlContains("https://booking.jazeeraairways.com/en/select-flight"));
		    } catch (Exception e) {
		        // Timeout occurred, handle the situation
		        System.out.println("Page didn't load within 60 seconds on attempt " + attempt + ". Clearing cookies...");
		        // Refresh the page
		        driver.get(J9ApiUrl);
		        Thread.sleep(4000);
		        System.out.println("Cookies deleted. Page refreshed.");
		    }

		    attempt++;
		}
        
        PageUtils.isElementLocated(driver, By.xpath("//strong[contains(text(),'Choose Your Departing Flight')]"));
        Thread.sleep(1000);
        if ((PnrDetails.From.contains("MAA"))){
        	Currency="INR";
        }
        else if ((PnrDetails.From.contains("CMB"))){
        	Currency="LKR";
        }
        else {
        String CurrencyType = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='From'])[8]/following::div[1]")).getText().replaceAll(" ", "");
        Currency = extractCurrencyType(CurrencyType);
        System.out.println("Currency : " + Currency);
        }
        
    
        try {
           
        	 //driver.findElement(By.xpath("//div[@class='select-date-range prev-date-range']")).click();
             //Thread.sleep(2000);
            for (int weekOffset = 0; weekOffset < 5; weekOffset++) {
                for (int dayOffset = 1; dayOffset <= 7; dayOffset++) {
                    int totalOffset = weekOffset * 7 + dayOffset;

                    if (totalOffset > 35) {
                        break; // Exit the loop if the total days processed exceed 70
                    }

                   
                    String DepDate=driver.findElement(By.xpath("//app-journey-one-way/section/app-trip-one-way/div/div[1]/div[2]/div["+dayOffset+"]/div/strong")).getText();
                    String[] dateParts = DepDate.split("\\W+");
                    String day = dateParts[0];
                    String monthAbbreviation = dateParts[1];
                    
                    if (monthAbbreviation.equals("Nov") || monthAbbreviation.equals("Dec")) {
                    	Year = "2024";
                    } else {
                    	Year = "2025";
                    }
                    Depdate = String.format("%s %s %s", day, monthAbbreviation, Year);

                    System.out.println("SRP Date: " + Depdate);
                    
                    String FlightsAvailable=driver.findElement(By.xpath("//app-trip-one-way/div/div[1]/div[2]/div["+dayOffset+"]")).getText().replaceAll("[\r\n]+", " ");
                    //System.out.println(FlightsAvailable); 
                    boolean isFlightsAvailable = FlightsAvailable.contains("From");
                    if (isFlightsAvailable) {
                        int dayInt = Integer.parseInt(day);
                        driver.findElement(By.xpath("//app-trip-one-way/div/div[1]/div[2]/div[" + dayOffset + "]")).click();
                        Thread.sleep(1000);

                        try {
                            boolean isDisplayed = driver.findElement(By.xpath("//div[contains(text(),'" + dayInt + " " + monthAbbreviation + "')]")).isDisplayed();
                            if (isDisplayed) {
                                FlightDetailsSending(driver, PnrDetails);
                            } else {
                            	driver.findElement(By.xpath("//app-trip-one-way/div/div[1]/div[2]/div[" + dayOffset + "]")).click();
                                Thread.sleep(1000);
                                driver.findElement(By.xpath("//div[contains(text(),'" + dayInt + " " + monthAbbreviation + "')]")).isDisplayed();
                                FlightDetailsSending(driver, PnrDetails);
                            }
                        } catch (NoSuchElementException e) {
                        	driver.findElement(By.xpath("//app-trip-one-way/div/div[1]/div[2]/div[" + dayOffset + "]")).click();
                            Thread.sleep(1000);
                            driver.findElement(By.xpath("//div[contains(text(),'" + dayInt + " " + monthAbbreviation + "')]")).isDisplayed();
                            FlightDetailsSending(driver, PnrDetails);
                        }
                    }
                    else {
                    	System.out.println("No Flights");
                    	String From = PnrDetails.From;
                        String To = PnrDetails.To;
                    	List<FadFlightDetails> finalList = new ArrayList<FadFlightDetails>();
                        ApiMethods.sendResults(Currency, From, To, Depdate, finalList);
                    }
                    	 

                    // If it's the last iteration of the inner loop and not the last week, click on the "Next" button
                    if (dayOffset == 7 && weekOffset < 4) {
                        driver.findElement(By.cssSelector("div.select-date-range.next-date-range")).click();
                        Thread.sleep(3000);
                    }
                }
            }
        } catch (Exception e) {
            // Handle exceptions
            
        }
    }


	
    public static void FlightDetailsSending(WebDriver driver, Database PnrDetails) throws Exception {
        String DataChange = null;
        String date = null;
        String month = null;
        String year = null;
        String FlightNum = null;
        String JournyTimeHours = null;
        String JournyTimeMin = null;
        String EndTime = null;
        String From = PnrDetails.From;
        String To = PnrDetails.To;
        String flySeatNum = "99";
        String flyFare = null;
        String FlyplusFare = null;
        String StartTerminal = null;
        String EndTerminal = null;
        String str1 = null;
        String flyPlusSeatNum = "99";
        String Sold = null;
        List<FadFlightDetails> finalList = new ArrayList<FadFlightDetails>();
        //PageUtils.isElementLocated(driver, By.xpath("//app-trip-one-way[1]/app-savers-journey-fare-detail[1]/div[1]/div"));
        String flightstab = driver.findElement(By.xpath("//app-trip-one-way[1]/app-savers-journey-fare-detail[1]/div[1]/div")).getText().replaceAll(" ", "");
        boolean isNoflights = flightstab.contains("Oops");
        if (isNoflights) {
            System.out.println("No Flights");
            //ApiMethods.sendResults(Currency, From, To, Depdate, finalList);
        } else {
            //PageUtils.isElementLocated(driver, By.xpath("//app-trip-one-way[1]/app-savers-journey-fare-detail[1]/div[1]/div"));
            try {
                String ele = null;
                List<WebElement> element = driver.findElements(By.xpath("//div[@class='flight-search-result-wrap savers_wrap_new ng-star-inserted']"));
                for (WebElement e1 : element) {
                    ele = e1.getText();
                    FadFlightDetails currentFlightFly = new FadFlightDetails();
                    FadFlightDetails currentFlightFlyPlus = new FadFlightDetails();
                    str1 = ele.replaceAll("[\r\n]+", " ").replace(",", "").replace("J9 ", "J9").replaceAll("Select Price.*$", "").trim();
                    String s = str1.replaceAll("View Flight Details ", "").replaceAll("Regular Price", "").replaceAll("Total: ", "").replaceAll("From ", "").replaceAll("Sharm ElSheikh", "SharmElSheikh").replaceAll("Sphinx Cairo", "Sphinx").replaceAll("Istanbul Grand", "IstanbulGrand").replaceAll("Istanbul Sabiha", "IstanbulSabiha").replaceAll("  ", " ");
                    String Str = new String(s);
                    String[] flightDetails = s.split("\n");
                    for (String flightDetail : flightDetails) {
                        // Process only Sphinx flights if From or To is SPX
                        if ((PnrDetails.From.contains("SPX") || PnrDetails.To.contains("SPX")) && !flightDetail.contains("Sphinx")) {
                            continue; // Skip non-Sphinx flights if From or To is SPX
                        } else if ((PnrDetails.From.contains("IST") || PnrDetails.To.contains("IST")) && !flightDetail.contains("IstanbulGrand")) {
                            continue; // Skip non-IstanbulGrand flights if From or To is IST
                        } else if (!(PnrDetails.From.contains("SPX") || PnrDetails.To.contains("SPX") || PnrDetails.From.contains("IST") || PnrDetails.To.contains("IST")) && (flightDetail.contains("Sphinx") || flightDetail.contains("IstanbulGrand"))) {
                            continue; // Skip Sphinx or IstanbulGrand flights if From and To are neither SPX nor IST
                        } else if (flightDetail.contains("Sold")) {
                            continue; // Skip sold out flights
                        }
                        System.out.println(s);

                        String day = s.split(" ")[1];
                        String Endday = s.split(" ")[13];
                        day = String.format("%02d", Integer.parseInt(day));
                        Endday = String.format("%02d", Integer.parseInt(Endday));
                        String monthAbbreviation = s.split(" ")[2];
                        String Year = s.split(" ")[3];
                        Departdate = String.format("%s %s %s", day, monthAbbreviation, Year);

                        String StartTime = s.split(" ")[0];
                        FlightNum = s.split(" ")[5];
                        EndTime = s.split(" ")[12];
                        boolean isDayChange = !day.equals(Endday);
                        boolean isSoldOut = s.contains("Sold Out");

                        JournyTimeHours = s.split(" ")[6].replace("hrs", "");
                        JournyTimeMin = s.split(" ")[8].replace("min", "");
                        flyFare = isSoldOut ? "Sold Out" : s.split(" ")[s.split(" ").length - 1];

                        if (isDayChange) {
                            DataChange = "1";
                        } else {
                            DataChange = null;
                        }

                        if (isSoldOut) {
                            flyFare = "00.0";
                        }

                        double adultFare = Double.parseDouble(flyFare);
                        double infantFare = adultFare * 0.4;
                        String InfantPrice = String.format("%.2f", infantFare);

                        int Hours = Integer.parseInt(JournyTimeHours);
                        int TotalMin = Hours * 60;

                        int Min = Integer.parseInt(JournyTimeMin);
                        int Total = TotalMin + Min;
                        String JournyTimeTotal = Integer.toString(Total);

                        currentFlightFlyPlus.FareType = currentFlightFly.FareType = "fly";
                        currentFlightFlyPlus.Class = currentFlightFly.Class = "Economy";
                        currentFlightFlyPlus.StartAirp = currentFlightFly.StartAirp = From;
                        currentFlightFlyPlus.EndAirp = currentFlightFly.EndAirp = To;
                        currentFlightFlyPlus.StartDt = currentFlightFly.StartDt = Departdate;
                        currentFlightFlyPlus.ADTBG = currentFlightFly.ADTBG = "";
                        currentFlightFlyPlus.CHDBG = currentFlightFly.CHDBG = "";
                        currentFlightFlyPlus.INFBG = currentFlightFly.INFBG = "";
                        currentFlightFlyPlus.DayChg = currentFlightFly.DayChg = DataChange;
                        currentFlightFlyPlus.Fltnum = currentFlightFly.Fltnum = FlightNum;
                        currentFlightFlyPlus.JrnyTm = currentFlightFly.JrnyTm = JournyTimeTotal;
                        currentFlightFlyPlus.StartTm = currentFlightFly.StartTm = StartTime;
                        currentFlightFlyPlus.EndTm = currentFlightFly.EndTm = EndTime;
                        currentFlightFlyPlus.NoOfSeats = currentFlightFly.NoOfSeats = "99";
                        currentFlightFlyPlus.StartTerminal = currentFlightFly.StartTerminal = StartTerminal;
                        currentFlightFlyPlus.EndTerminal = currentFlightFly.EndTerminal = EndTerminal;
                        currentFlightFlyPlus.AdultBasePrice = currentFlightFly.AdultBasePrice = flyFare.replace(",", "");
                        currentFlightFlyPlus.AdultTaxes = currentFlightFly.AdultTaxes = "";
                        currentFlightFlyPlus.ChildBasePrice = currentFlightFly.ChildBasePrice = flyFare.replace(",", "");
                        currentFlightFlyPlus.ChildTaxes = currentFlightFly.ChildTaxes = "";
                        currentFlightFlyPlus.InfantBasePrice = currentFlightFly.InfantBasePrice = InfantPrice;
                        currentFlightFlyPlus.InfantTaxes = currentFlightFly.InfantTaxes = "";
                        currentFlightFlyPlus.TotalApiFare = currentFlightFly.TotalApiFare = "";

                        finalList.add(currentFlightFly);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ApiMethods.sendResults(Currency, From, To, Depdate, finalList);
            str1 = null;
        }
    }
}





