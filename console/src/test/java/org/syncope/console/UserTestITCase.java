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

public class UserTestITCase extends AbstractTest {

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Users\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//a[contains(text(),'Create new user')]");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//span[contains(text(),'Attributes')]\");",
                "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");
        selenium.click("css=a.w_close");
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Users\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        //Edit user3
        selenium.click(
                "//*[@id=\"users-contain\"]//*[span=3]/../td[4]/span/span[7]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//input[@value='testUsername']\");",
                "30000");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//input[@value='Doe']\");",
                "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");
        selenium.click("css=a.w_close");
    }

    @Test
    public void search() {
        selenium.click("css=img[alt=\"Users\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("link=Search");
        selenium.select("//td[3]/select", "label=MEMBERSHIP");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//td[4]/select[option='8 otherchild']\");",
                "30000");

        selenium.select("//td[4]/select", "label=8 otherchild");
        selenium.click("name=search");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//*[@id='users-contain']//*[span=1]\");",
                "30000");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Users\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click(
                "//*[@id=\"users-contain\"]//*[span=4]/../td[4]/span/span[8]/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));

        // it depends on the execution order of tests: resources
        // 'ws-target-resource-delete' could have been deleted from
        // ResourceTestITCase#delete
        selenium.waitForCondition("selenium.isTextPresent("
                + "\"Operation executed successfully\");",
                "30000");
    }
}
