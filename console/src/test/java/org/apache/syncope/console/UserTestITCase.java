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

import static junit.framework.TestCase.assertTrue;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class UserTestITCase extends AbstractTest {

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseCreateModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs-1']/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'Attributes')]")));

        seleniumDriver.findElement(By.xpath("//div/form/div[3]/div[1]/span[2]/div/div[2]/span")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[6]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseEditModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        //Edit vivaldi
        seleniumDriver.findElement(By.xpath("//*[@id=\"users-contain\"]//*[div=3]/../td[5]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='Antonio Vivaldi']")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='Vivaldi']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[6]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void search() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//span[contains(text(),'Search')]")).click();
        Select select = new Select(seleniumDriver.findElement(By.xpath("//td[2]/select")));
        select.selectByVisibleText("MEMBERSHIP");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[3]/select[option='3 citizen']")));

        select = new Select(seleniumDriver.findElement(By.xpath("//td[3]/select")));
        select.selectByVisibleText("3 citizen");
        seleniumDriver.findElement(By.xpath("//form/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='users-contain']//*[div=2]")));
    }

    @Test
    public void delete() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//*[@id=\"users-contain\"]//*[div=4]/../td[5]/div/span[15]/a")).click();

        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='propagation']/span")));

        seleniumDriver.findElement(By.xpath("//*[@id=\"users-contain\"]/a")).click();

        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void browseProvisioningFeatures() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        //Edit vivaldi
        seleniumDriver.findElement(By.xpath("//*[@id=\"users-contain\"]//*[div=3]/../td[5]/div/span[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[div='ws-target-resource-1']")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[div='resource-testdb']")));

        seleniumDriver.findElement(By.xpath("//div[@class='navigator']/div/span[4]/a")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[div='resource-ldap']")));

        seleniumDriver.findElement(By.xpath("//div[@class='navigator']/div/span/a")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[div='ws-target-resource-1']")));

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void issueSYNCOPE495() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//*[@id=\"users-contain\"]//*[div=3]/../td[5]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='Antonio Vivaldi']")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='Vivaldi']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[6]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//span/div/form/div[3]/div[6]/span/div[1]/div/div/div/a")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//span/div/form/div[3]/div[6]/span/div[1]/div/div/div[2]/div[1]/div/a")));
        seleniumDriver.findElement(By.xpath("//span/div/form/div[3]/div[6]/span/div[1]/div/div/div[2]/div[1]/div/a")).
                click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[2]/a/span")));
                seleniumDriver.findElement(By.xpath("//span[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='infolabel']")));
        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }
}
