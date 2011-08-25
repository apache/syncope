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

public class ResourceTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//form/fieldset/label[text()='Name']")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//td/input");
        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//td[2]/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//form/fieldset/label[text()='Name']")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//tr[2]/td/input");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
        selenium.click("name=:submit");

    }

    @Test
    public void delete() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//tr[4]/td[3]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }
}
