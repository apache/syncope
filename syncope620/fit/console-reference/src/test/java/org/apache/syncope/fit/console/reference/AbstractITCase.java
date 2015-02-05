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

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractITCase {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractITCase.class);

    public static final String ADMIN = "admin";

    public static final String PASSWORD = "password";

    public static final String BASE_URL = "http://localhost:9080/syncope-console/";

    protected WebDriver seleniumDriver;

    protected WebDriverWait wait;

    @Before
    public void setUp() throws Exception {
        seleniumDriver = new FirefoxDriver();
        seleniumDriver.get(BASE_URL);
        wait = new WebDriverWait(seleniumDriver, 10);

        WebElement element = seleniumDriver.findElement(By.name("userId"));
        element.sendKeys(ADMIN);
        element = seleniumDriver.findElement(By.name("password"));
        element.sendKeys(PASSWORD);
        seleniumDriver.findElement(By.name("p::submit")).click();

        (new WebDriverWait(seleniumDriver, 10))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt='Logout']")));
    }

    @After
    public void tearDown() throws Exception {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Logout\"]")).click();
        seleniumDriver.quit();
    }
}
