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

public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//a[contains(text(),'Create new configuration')]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//input[@name='key:textField']\");",
                "30000");

        selenium.type("name=key:textField", "test1");
        selenium.type("name=value:textField", "value1");
        selenium.click("name=apply");

        selenium.waitForCondition(
                "selenium.isTextPresent(\"Operation executed successfully\");",
                "30000");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//tr/td[3]/span/span[7]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//input[@name='key:textField']\");",
                "30000");

        assertEquals("connid.bundles.directory",
                selenium.getAttribute("//input[@name='key:textField']@value"));

        selenium.click("css=a.w_close");

    }

    @Test
    public void browsePasswordPolicy() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//div[3]/div[2]/span/div/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//input[@name='id:textField']\");",
                "30000");

        selenium.type("name=description:textField", "new description");
        selenium.click("//div[2]/form/div[3]/input");

        selenium.waitForCondition(
                "selenium.isTextPresent(\"new description\");",
                "30000");
    }

    @Test
    public void browseAccountPolicy() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[3]/a");
        selenium.click("//div[3]/div[3]/span/div/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//input[@name='id:textField']\");",
                "30000");

        selenium.type("name=description:textField", "new description");
        selenium.click("//div[2]/form/div[3]/input");

        selenium.waitForCondition(
                "selenium.isTextPresent(\"new description\");",
                "30000");
    }

    @Test
    public void browseWorkflowDef() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//*[@id='workflowDefArea']\");",
                "30000");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//tr[7]/td[3]/span/span[8]/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));

        selenium.waitForCondition("selenium.isTextPresent("
                + "\"Operation executed successfully\");",
                "30000");
    }

    @Test
    public void setLogLevel() {
        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[7]/a/span");

        selenium.select(
                "//div[3]/div[7]/div/span/table/tbody/tr/td[2]/select",
                "label=ERROR");

        selenium.waitForCondition(
                "selenium.isTextPresent(\"Operation executed successfully\");",
                "30000");
    }
}
