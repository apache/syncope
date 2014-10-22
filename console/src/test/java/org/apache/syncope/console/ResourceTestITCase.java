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
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ResourceTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div/a")).click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[3]/div/span/div/div/div/label[text()='Name']")));

        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void browseEditModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//td[6]/div/span[13]/a")).click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//form/div[3]/div/span/div/div/div/label[text()='Name']")));

        seleniumDriver.findElement(By.xpath("//li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tbody/tr[2]/td/input")));

        seleniumDriver.findElement(By.xpath("//tbody/tr[2]/td/input")).click();

        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();

        seleniumDriver.findElement(By.xpath("//div[4]/input")).click();

        seleniumDriver.switchTo().defaultContent();
    }

    @Test
    public void delete() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//tr[3]/td[6]/div/span[15]/a")).click();

        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();
    }

    @Test
    public void checkSecurityTab() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//td[6]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[3]/div/span/div/div/div/label[text()='Name']")));        

        seleniumDriver.findElement(By.xpath("//li[4]/a")).click();

        assertTrue(seleniumDriver.findElements(By.xpath("//label[@for='passwordPolicy']")).size()>0);

        seleniumDriver.findElement(By.xpath("//li[1]/a")).click();
        seleniumDriver.findElement(By.xpath("//li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//li[3]/a")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void checkConnection() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(
                By.xpath("//*[@id=\"users-contain\"]//*[div=\"ws-target-resource-delete\"]/../td[6]/div/span[13]/a"))
                .click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[3]/div/span/div/div/div/label[text()='Name']")));        

        seleniumDriver.findElement(By.xpath("//li[4]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='endpoint']")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div[4]/span/span/div[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div/ul/li/span[contains(text(), 'Successful connection')]")));

        seleniumDriver.switchTo().defaultContent();
    }
}
