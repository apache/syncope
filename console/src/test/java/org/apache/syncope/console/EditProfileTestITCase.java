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

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EditProfileTestITCase extends AbstractTest {

    @Override
    @Before
    public void setUp() throws Exception {
        seleniumDriver = new FirefoxDriver();
        //selenium = new WebDriverBackedSelenium(seleniumDriver, BASE_URL);
        seleniumDriver.get(BASE_URL);
        wait = new WebDriverWait(seleniumDriver, 6);

    }

    @Test
    public void selfRegistration() {
        seleniumDriver.findElement(By.xpath("//div[1]/div[2]/div[1]/span/a")).click();                                             

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'Attributes')]")));
        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();

        // only to have some "Logout" available for @After
        seleniumDriver.get(BASE_URL);
        seleniumDriver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        WebElement element = seleniumDriver.findElement(By.name("userId"));
        element.sendKeys(ADMIN);
        element = seleniumDriver.findElement(By.name("password"));
        element.sendKeys(PASSWORD);
        seleniumDriver.findElement(By.name("p::submit")).click();
        seleniumDriver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
    }

    @Test
    public void editUserProfile() {
        WebElement element = seleniumDriver.findElement(By.name("userId"));
        element.sendKeys("rossini");
        element = seleniumDriver.findElement(By.name("password"));
        element.sendKeys("password");
        seleniumDriver.findElement(By.name("p::submit")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='username']/a")));
        seleniumDriver.findElement(By.xpath("//div[@id='username']/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'Attributes')]")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@value='rossini']")));
        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        //seleniumDriver.stop();
    }
}
