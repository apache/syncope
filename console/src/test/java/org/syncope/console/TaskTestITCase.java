/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console;

import org.junit.Test;

public class TaskTestITCase extends AbstractTest {

    @Test
    public void execute() {
        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[4]/a");
        selenium.click("//table/tbody/tr/td[7]/span/span[3]/a");

        selenium.waitForCondition("selenium.isTextPresent("
                + "\"Operation executed successfully\");",
                "30000");

        selenium.click("//table/tbody/tr/td[7]/span/span[7]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div/span/div/div/div[2]/span/input\");",
                "30000");

        assertTrue(selenium.isElementPresent(
                "//form/div[2]/div[2]/span/table/tbody/tr/td"));

        selenium.click("css=a.w_close");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//table/tbody/tr[4]/td[7]/span/span[8]/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));

        selenium.waitForCondition(
                "selenium.isTextPresent(\"Operation executed successfully\");",
                "30000");
    }
}
