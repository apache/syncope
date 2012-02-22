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
package org.syncope.console;

import org.junit.Test;

public class RoleTestITCase extends AbstractTest {

    @Test
    public void createRootNodeModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table"
                + "/tbody/tr/td[2]/table/tbody/tr/td[2]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[3]/span[2]/span/div/p/span/span/a\");",
                "30000");

        selenium.click("//div[3]/span[2]/span/div/p/span/span/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//span[contains(text(),'Attributes')]\");",
                "30000");

        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[3]/span[2]/span/span/div/p/span[2]/span/a\");",
                "30000");

        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//iframe\");", "30000");

        selenium.selectFrame("relative=up");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/ul/li[1]/a/span\");", "30000");

        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[6]/a/span");

        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[3]/span[2]/span/span/div/p/span[2]/span/a[2]\");",
                "30000");

        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a[2]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//iframe\");", "30000");

        selenium.selectFrame("relative=up");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/ul/li[1]/a/span\");", "30000");

        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[6]/a/span");

        selenium.click("css=a.w_close");
    }

    @Test
    public void checkSecurityTab() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/form/div[2]/ul/li[6]/a\");",
                "30000");

        selenium.click("//div/form/div[2]/ul/li[6]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div[6]/span/div/div[2]/div/label\");",
                "30000");
    }

    @Test
    public void browseUserEditModal() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[2]/tbody/tr/td[3]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/form/div[2]/ul/li[7]/a\");",
                "30000");

        selenium.click("//div/form/div[2]/ul/li[7]/a");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//table/tbody/tr/td[4]/span/span[7]/a\");",
                "30000");

        selenium.click(
                "//div[3]/span[2]/span/span/div/form/div"
                + "[2]/div[2]/div/div/span/div/table/tbody/tr/td[4]/span/span[7]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent("
                + "\"//form/div[2]/div/span/div/div/div[contains(text(),'Username')]\");",
                "30000");

        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void deleteUser() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[2]/tbody/tr/td[3]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/form/div[2]/ul/li[7]/a\");",
                "30000");

        selenium.click("//div/form/div[2]/ul/li[7]/a");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//span[8]/a\");", "30000");

        selenium.click(
                "//div[3]/span[2]/span/span/div/form/div"
                + "[2]/div[2]/div/div/span/div/table/tbody/tr[2]/td[4]/span/span[8]/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }

    @Test
    public void deleteRole() {
        selenium.click("css=img[alt=\"Roles\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='navigationPane']\");",
                "30000");

        selenium.click("//div[3]/span/div/div/table[10]/tbody/tr/td[6]/"
                + "table/tbody/tr/td[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/p/span[2]/span/a[3]\");",
                "30000");

        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a[3]");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }
}