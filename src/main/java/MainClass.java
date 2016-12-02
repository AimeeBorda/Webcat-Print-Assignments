import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class MainClass {

    /*
        In order to delete a file-type run this script in your browser (tested with Chrome) changing
        "[./a[contains(text(),'.txt')]]" accordingly.

        $x("//div[@id='contents']/h2[@class='collapsible'][./a[contains(text(),'.txt')]]/following-sibling::div[@class='expboxcontent']").forEach(function (e){ e.parentNode.removeChild(e);});

         You can pass three arguments through the command line (or else just change them here)
         1. The year (this should contain the text in the dropdown)
         2. Assignment Name (once again partial matching is enough)

     */

    public static void main(String[] args){

        String url = args[0];
        String username = args[1];
        String password = args[2];
        String year = args[3];
        String assignment = args[4];
        String browser = args.length > 4 ? args[5] : "chrome";

        //set up driver
        System.setProperty("webdriver."+browser+".driver", browser+"driver");
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver,40);

        //login
        login(driver, wait, username, password, url);

        //go to requested assignment's page
        goToAssignmentPage(driver, wait, year, assignment);

        //get first student
        List<WebElement> nextPage;
        int pageNum = 0;
        do{
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@title,'View, score, or make comments on submission')]"))).click();

            String html = goThroughReports(driver, wait);
            writeToFile(html, assignment +"_"+ year + "_" + pageNum++);

            nextPage = driver.findElements(By.xpath("//table/tfoot/tr/th[@class='pagination']/table/tbody/tr/td[1]/span/span[@class='current'][1]/following-sibling::a"));

            if(nextPage.size() > 0){
                nextPage.get(0).click();
                waitForRefresh(wait);
            }
        }while(nextPage.size() > 0 );

        //finished creating html files and logging out
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("logout"))).click();

        driver.close();
    }

    private static void waitForRefresh(WebDriverWait wait) {
        wait.until(ExpectedConditions.attributeToBe(By.tagName("body"),"class","aristo showprogress"));
        wait.until(ExpectedConditions.attributeToBe(By.tagName("body"),"class","aristo"));
    }

    private static String goThroughReports(WebDriver driver, WebDriverWait wait) {
        String html ="";
        do{
            //go to full printable report page
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"dijit_form_Button_7\"]"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.floatLeft")));

            printStudentNumber(driver.findElement(By.xpath("//h1")).getText());
            html = appendHtml(driver, html);

            //go back to summary page
            driver.findElement(By.xpath("//*[@id=\"contents\"]/form[1]/input")).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"dijit_form_Button_9\"]"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a"))); //wait for the page to load

        }while(inStudentSummaryPage(driver));
        return html;
    }

    private static void goToAssignmentPage(WebDriver driver, WebDriverWait wait, String year, String assignment) {
        // go to Grading > View Submissions
        wait.until(ExpectedConditions.elementToBeClickable(By.id("dijit_form_Button_0")));
        Actions builder = new Actions(driver);
        builder.moveToElement(driver.findElement(By.xpath("//ul[@id='mainMenu']/li[2]"))).moveToElement(driver.findElement(By.xpath("//a[contains(text(), 'View Submissions')]"))).click().build().perform();


        //choose year from dropdown
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table/tbody/tr/td/a[@title='View a different submission']")));
        builder = new Actions(driver);
        builder.moveToElement(driver.findElement(By.className("inlined"))).moveToElement(driver.findElement(By.xpath("//li[contains(text(), '"+year+"')]"))).click().build().perform();

        //choose assignment from dropdow
        waitForRefresh(wait);
        builder = new Actions(driver);
        builder.moveToElement(driver.findElements(By.className("inlined")).get(2)).moveToElement(driver.findElement(By.xpath("//*[contains(@id,'assignmentMenu')]/ul/li[text()[contains(.,'"+assignment+"')]]"))).click().build().perform();

    }

    private static void login(WebDriver driver, WebDriverWait wait, String username, String password, String url) {
        driver.get(url);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("UserName"))).sendKeys(username);
        driver.findElement(By.id("UserPassword")).sendKeys(password);
        driver.findElement(By.id("dijit_form_Button_0_label")).click();
    }

    private static boolean inStudentSummaryPage(WebDriver driver) {
        return driver.findElements(By.xpath("//*[@id=\"dijit_form_Button_7\"]")).size() > 0;
    }

    private static String appendHtml(WebDriver driver, String html) {
        if (html.isEmpty()) {
            html = "<html>" + driver.findElement(By.tagName("html")).getAttribute("innerHTML") + "</html>";
        } else {
            html = html.replaceFirst("</body></html>", driver.findElement(By.tagName("html")).getAttribute("innerHTML") + "</body></html>");
        }
        return html;
    }

    private static void printStudentNumber(String header){
        System.out.println(header.split("\\bby\\b")[1]);
    }

    private static void writeToFile(String html, String fileName){
        try {
            File file = new File(fileName+".html");

            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fop = new FileOutputStream(file);
            fop.write(html.getBytes());
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
