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

        selenium.click("//table/tbody/tr/td[8]/div/span[8]/a");
        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/div/div/span/div/div/div/span\");", "30000");

        selenium.click("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div[2]/div/a");
        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select\");", "30000");

        selenium.select("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div/div/select", "testUserReportlet");
        selenium.click("//div[2]/form/div[2]/div/div/span/div/div[5]/div[2]/div[2]/div[2]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[2]/form/div[2]/div/div/div/div/label\");", "30000");

        selenium.select("//form/div[2]/div/div/div[2]/div[2]/span/select",
                "label=org.apache.syncope.client.report.UserReportletConf");

        selenium.click("css=a.w_close");
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
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[2]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[3]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[4]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[5]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[6]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[7]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[8]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[9]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[10]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[11]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[12]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[13]/a");
        selenium.click("//div[3]/div[2]/span/form/div[2]/div/span/ul/li[14]/a");
    }
}
