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
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//a[contains(text(),'Create new user')]");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("css=a.w_close");
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//tr[3]/td[4]/span/a");

        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        assertEquals("testUsername",
                selenium.getAttribute(
                "//form/div[2]/div/div/span/div[3]/div[2]/span/input@value"));
        assertEquals("Doe",
                selenium.getAttribute(
                "//form/div[2]/div/div/span/div[7]/div[2]/span/input@value"));
        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("css=a.w_close");
    }

    @Test
    public void search() {
        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("link=Search");
        selenium.select("//td[3]/select", "label=MEMBERSHIP");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        selenium.select("//td[4]/select", "label=8 otherchild");
        selenium.click("name=search");
        assertEquals("1", selenium.getText(
                "//div[@id=\"users-contain\"]/span/table/tbody/tr/td/span"));
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//tr[4]/td[5]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        // it depends on the execution order of tests: resources
        // 'ws-target-resource-delete' could have been deleted from
        // ResourceTestITCase#delete
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
    }
}
