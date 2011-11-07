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
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Tasks\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[4]/a");
        selenium.click("//div[3]/div[4]/span/div/span/"
                + "table/tbody/tr/td[6]/span/a");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
        selenium.click("//div[3]/div[4]/span/div/span/"
                + "table/tbody/tr/td[5]/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent("//form/div[2]/div/div/span/"
                        + "div/div/div[2]/span/input")) {

                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        assertTrue(selenium.isElementPresent(
                "//form/div[2]/div[3]/span/table/tbody/tr/td"));
        selenium.click("css=a.w_close");
    }

    @Test
    public void delete() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Tasks\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[3]/div[3]/span/div/span/"
                + "table/tbody/tr/td[8]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
    }
}
