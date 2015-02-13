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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class TaskITCase extends AbstractITCase {

    @Test
    public void execute() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[1]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tr[4]/td[10]/div/span[6]/a/img")));

        seleniumDriver.findElement(By.xpath("//tr[4]/td[10]/div/span[6]/a/img")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(
                By.tagName("body")).getText().contains("Operation executed successfully"));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tr[4]/td[10]/div/span[13]/a/img")));

        seleniumDriver.findElement(By.xpath("//tr[4]/td[10]/div/span[13]/a/img")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[2]/ul/li[2]/a")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[2]/ul/li[2]/a"));

        assertTrue(seleniumDriver.findElements(
                By.xpath("//div[2]/form/div[2]/div[2]/span/table/tbody/tr/td[4]")).size() > 0);

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void delete() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//img[@alt='delete icon'])[6]")));

        seleniumDriver.findElement(By.xpath("(//img[@alt='delete icon'])[6]")).click();
        seleniumDriver.switchTo().alert().accept();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(
                By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void issueSYNCOPE148() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//a[contains(text(),'Create')]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[2]/div/div/span/div/div[2]/div/label")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input[2]")).click();

        seleniumDriver.switchTo().defaultContent();

        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("Key"));
    }

    @Test
    public void issueSYNCOPE473() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[5]/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        assertFalse(seleniumDriver.findElements(By.xpath("//div[@id='userFilter']")).isEmpty());

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }
}
