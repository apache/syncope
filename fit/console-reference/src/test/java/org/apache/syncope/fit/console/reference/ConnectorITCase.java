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
package org.apache.syncope.fit.console.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class ConnectorITCase extends AbstractITCase {

    @Test
    public void browseCreateModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[3]/div[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(("//iframe"))));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[2]/div/div/div[3]/div[2]/span/select")));

        Select select = new Select(
                seleniumDriver.findElement(By.xpath("//form/div[2]/div[1]/div[1]/div[2]/div[2]/span/select")));

        select.selectByValue("1");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='connectorName:dropDownChoiceField']/option[3]")));

        select = new Select(
                seleniumDriver.findElement(By.xpath("//form/div[2]/div[1]/div[1]/div[3]/div[2]/span/select")));
        select.selectByVisibleText("net.tirasa.connid.bundles.soap");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[1]/a/span")).click();

        assertTrue(seleniumDriver.findElement(By.xpath("//form/div[2]/div/div/div[3]/div[2]")).isDisplayed());

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void browseEditModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[3]/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//tr[4]/td[7]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        assertEquals("ConnInstance103",
                seleniumDriver.findElement(By.xpath("//input[@name='displayName:textField']")).getAttribute("value"));

        assertEquals("net.tirasa.connid.bundles.soap",(new Select(seleniumDriver.findElement(
                By.xpath("//select[@name='connectorName:dropDownChoiceField']")))).getFirstSelectedOption().getText());

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void delete() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[3]/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//tr[4]/td[7]/div/span[15]/a")).click();

        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("Error: "));
    }

    @Test
    public void checkConnection() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[3]/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//tr[2]/td[7]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='version:dropDownChoiceField']")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div[2]/div/span/div[2]/div[30]/a")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/div[2]/div/span/div[2]/div[30]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div/ul/li/span[contains(text(),'Successful connection')]")));

        seleniumDriver.switchTo().defaultContent();
    }

    @Test
    public void issueSYNCOPE506() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//tr[4]/td[3]/div/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='version:dropDownChoiceField']")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        seleniumDriver.findElement(By.xpath(
                "//div[2]/form/div[2]/div[2]/div/span/div[2]/div[30]/div[3]/span/div/div/span/a[2]/span/span")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input")).click();

        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));

        seleniumDriver.findElement(By.xpath("//tr[4]/td[3]/div/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='version:dropDownChoiceField']")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='99']")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void issueSyncope605() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[3]/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//tr[8]/td[7]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        assertEquals("H2-testsync",
                seleniumDriver.findElement(By.xpath("//input[@name='displayName:textField']")).getAttribute("value"));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[3]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/div[3]/span/input[7]")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input")).click();

        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        
        seleniumDriver.findElement(By.xpath("//tr[8]/td[7]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[3]/a")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[2]/div[3]/span/input[7]")));

        assertFalse(seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/div[3]/span/input[7]")).isSelected());
        seleniumDriver.switchTo().defaultContent();
    }
}
