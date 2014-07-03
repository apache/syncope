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

public class RoleTestITCase extends AbstractTest {

    @Test
    public void createRootNodeModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div/span/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/div/span[2]/span/div/p/span/span/a\");", "30000");

        selenium.click("//div/div/span[2]/span/div/p/span/span/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//span[contains(text(),'Attributes')]\");", "30000");

        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div[2]/div/div/span/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//img[@alt='create icon']\");", "30000");

        selenium.click("//img[@alt='create icon']");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");

        selenium.selectFrame("index=0");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[2]/form/div[3]/div/ul/li[1]/a/span\");", "30000");

        selenium.click("//div[2]/form/div[3]/div/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[6]/a/span");

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div[2]/div/div/span/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//img[@alt='edit icon']\");", "30000");

        selenium.click("//img[@alt='edit icon']");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");

        selenium.selectFrame("index=0");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[2]/form/div[3]/div/ul/li[1]/a/span\");", "30000");

        selenium.click("//div[2]/form/div[3]/div/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[6]/a/span");
        selenium.click("//div[2]/form/div[3]/div/ul/li[7]/a/span");

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }

    @Test
    public void checkSecurityTab() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div[2]/div/div/span/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//div/form/div[2]/ul/li[7]/a\");", "30000");

        selenium.click("//div/form/div[2]/ul/li[7]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div[8]/span/div/div/div/label\");", "30000");
    }

    @Test
    public void browseUserEditModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div[2]/div/div/span/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a\");", "30000");

        selenium.click("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//table/tbody/tr/td[5]/div/span[13]/a\");", "30000");

        selenium.click("//table/tbody/tr/td[5]/div/span[13]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[3]/div/span/div/div/div[contains(text(),'Username')]\");", "30000");

        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void searchUsers() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click("//div/div/span/div/div/div/div/div[2]/div/div/span/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a\");", "30000");

        selenium.click("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//span[15]/a\");", "30000");
    }

    @Test
    public void deleteRole() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='navigationPane']\");", "30000");

        selenium.click(
                "//div/div/span/div/div/div/div/div[2]/div[2]/div[2]/div/div[2]/div[3]/div/span[2]/a/span");

        selenium.waitForCondition("selenium.isElementPresent(\"//img[@alt='delete icon']\");", "30000");

        selenium.click("//img[@alt='delete icon']");

        assertTrue(selenium.getConfirmation().equals("Do you really want to delete the selected item(s)?"));
    }

    @Test
    public void issueSYNCOPE510() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"link=Search\");", "30000");

        selenium.click("link=Search");
        selenium.select("//td[2]/select", "label=RESOURCE");

        selenium.waitForCondition("selenium.isElementPresent(\"//td[3]/select[option='ws-target-resource-2']\");",
                "30000");

        selenium.select("//td[3]/select", "label=ws-target-resource-2");
        selenium.click("//form/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[3]/div[2]/div[2]/span/div[1]/span[1]/span/form/span/table/tbody/"
                + "tr/td[3]/div\");", "30000");
    }
}
