/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class ReportTestITCase extends AbstractTest {

    @Test
    public void readReportlet() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Reports\"]")).click();        

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));
        seleniumDriver.findElement(By.xpath("//table/tbody/tr/td[9]/div/span[13]/a")).click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div/div/span/div/div/div/span")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div[2]/div/a"))
                .click();
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select")));

        Select select = new Select(seleniumDriver.findElement(
                By.xpath("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select")));
        select.selectByVisibleText("testUserReportlet");
        
        seleniumDriver.findElement(
                By.xpath("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div[2]/div[2]/a")).click();

        seleniumDriver.switchTo().defaultContent();
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                By.xpath("//div[7]/form/div/div[2]/div/div/div/div[2]/div/div/iframe")));       
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("reportletClass:dropDownChoiceField")));

        select = new Select(seleniumDriver.findElement(By.name("reportletClass:dropDownChoiceField")));
        select.selectByVisibleText("org.apache.syncope.common.report.StaticReportletConf");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void executeReport() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Reports\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//table/tbody/tr/td[9]/div/span[6]/a")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void navigateAudit() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Reports\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//option[contains(text(),'[REST]:[EntitlementController]:[]:[getOwn]:[SUCCESS]')]")));        

        Select select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='events:categoryContainer:type:dropDownChoiceField']")));
        select.selectByVisibleText("PROPAGATION");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='events:categoryContainer:category:dropDownChoiceField']/option[text()='user']")));                

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='events:categoryContainer:category:dropDownChoiceField']")));
        select.selectByVisibleText("user");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='events:categoryContainer:subcategory:dropDownChoiceField']"
                        + "/option[text()='resource-csv']")));               

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='events:categoryContainer:subcategory:dropDownChoiceField']")));
        select.selectByVisibleText("resource-csv");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//input[@name='events:eventsContainer:eventsPanel:successGroup']")));
    }
}
