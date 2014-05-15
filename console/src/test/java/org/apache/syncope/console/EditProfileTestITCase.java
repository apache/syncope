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

import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;

public class EditProfileTestITCase extends AbstractTest {

    @Override
    @Before
    public void setUp() throws Exception {
        seleniumDriver = new FirefoxDriver();
        selenium = new WebDriverBackedSelenium(seleniumDriver, BASE_URL);

        selenium.open("/syncope-console/");
    }

    @Test
    public void selfRegistration() {
        selenium.click("//div/span/span/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//span[contains(text(),'Attributes')]\");", "30000");

        selenium.click("css=a.w_close");

        // only to have some "Logout" available for @After
        selenium.type("name=userId", "rossini");
        selenium.type("name=password", "password");
        selenium.click("name=:submit");

        selenium.waitForPageToLoad("30000");
    }

    @Test
    public void editUserProfile() {
        selenium.type("name=userId", "rossini");
        selenium.type("name=password", "password");
        selenium.click("name=:submit");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"Schema\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("//div/ul/li[10]/div/div/a/span");

        selenium.waitForCondition("selenium.isElementPresent(\"//span[contains(text(),'Attributes')]\");", "30000");
        selenium.waitForCondition("selenium.isElementPresent(\"//input[@value='rossini']\");", "30000");

        selenium.click("css=a.w_close");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        selenium.stop();
    }
}
