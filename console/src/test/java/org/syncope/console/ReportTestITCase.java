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

public class ReportTestITCase extends AbstractTest {

    @Test
    public void readReportlet() {
        selenium.click("css=img[alt=\"Reports\"]");
        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//table/tbody/tr/td[6]/span/span[7]/a");
        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div/span/div/div[5]/div[2]/span/div[2]/div/a\");", "30000");

        selenium.click("css=img[alt=\"plus icon\"]");
        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div/div[2]/div[2]/span/select\");", "30000");

        selenium.select("//form/div[2]/div/div/div[2]/div[2]/span/select",
                "label=org.syncope.client.report.UserReportletConf");
        selenium.waitForCondition("selenium.isElementPresent(\"//form/div[2]/div[2]/div/span/div/div/span\")", "30000");

        selenium.click("css=a.w_close");
        selenium.click("css=a.w_close");
    }

    @Test
    public void execute() {
        selenium.click("css=img[alt=\"Reports\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//table/tbody/tr/td[6]/span/span[3]/a");

        selenium.waitForCondition("selenium.isTextPresent(\"Operation executed successfully\");", "30000");
    }
}
