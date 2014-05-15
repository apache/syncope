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

public class ReportTestITCase extends AbstractTest {

    @Test
    public void readReportlet() {
        selenium.click("css=img[alt=\"Reports\"]");
        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//table/tbody/tr/td[8]/div/span[9]/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");
        selenium.selectFrame("index=0");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/div/div/span/div/div/div/span\");", "30000");

        selenium.click("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div[2]/div/a");
        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select\");", "30000");

        selenium.select("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select", "testUserReportlet");
        selenium.click("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div[2]/div[2]/a");

        seleniumDriver.switchTo().defaultContent();

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"name=reportletClass:dropDownChoiceField\");", "30000");

        selenium.selectFrame("index=1");

        selenium.select("name=reportletClass:dropDownChoiceField",
                "org.apache.syncope.common.report.StaticReportletConf");

        selenium.click("//div[2]/form/div[3]/input");

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }

    @Test
    public void executeReport() {
        selenium.click("css=img[alt=\"Reports\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//table/tbody/tr/td[8]/div/span[3]/a");

        selenium.waitForCondition("selenium.isTextPresent(\"Operation executed successfully\");", "30000");
    }

    @Test
    public void navigateAudit() {
        selenium.click("css=img[alt=\"Reports\"]");
        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.waitForCondition(
                "selenium.isTextPresent(\"[REST]:[AuthenticationController]:[]:[getEntitlements]:[SUCCESS]\");",
                "30000");

        selenium.select(
                "//select[@name='events:categoryContainer:type:dropDownChoiceField']",
                "label=PROPAGATION");

        selenium.waitForCondition("selenium.isElementPresent(\""
                + "//select[@name='events:categoryContainer:category:dropDownChoiceField']/option[text()='user']\");",
                "30000");

        selenium.select(
                "//select[@name='events:categoryContainer:category:dropDownChoiceField']",
                "label=user");

        selenium.waitForCondition("selenium.isElementPresent(\""
                + "//select[@name='events:categoryContainer:subcategory:dropDownChoiceField']/option[text()='resource-csv']\");",
                "30000");

        selenium.select(
                "//select[@name='events:categoryContainer:subcategory:dropDownChoiceField']",
                "label=resource-csv");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//input[@name='events:eventsContainer:eventsPanel:successGroup']\");",
                "30000");
    }
}
