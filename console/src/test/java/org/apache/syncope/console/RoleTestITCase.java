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

import static junit.framework.TestCase.assertTrue;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class RoleTestITCase extends AbstractTest {

    @Test
    public void createRootNodeModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/div/span[2]/span/div/p/span/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span[2]/span/div/p/span/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'Attributes')]")));

        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void browseCreateModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div/div/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div[2]/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt='create icon']")));

        seleniumDriver.findElement(By.xpath("//img[@alt='create icon']")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[3]/div/ul/li[1]/a/span")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[1]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[6]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void browseEditModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div/div/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div[2]/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt='edit icon']")));

        seleniumDriver.findElement(By.xpath("//img[@alt='edit icon']")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[3]/div/ul/li[1]/a/span")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[5]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[6]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/ul/li[7]/a/span")).click();

        seleniumDriver.switchTo().defaultContent();

        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void checkSecurityTab() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div/div/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div[2]/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/form/div[2]/ul/li[7]/a")));

        seleniumDriver.findElement(By.xpath("//div/form/div[2]/ul/li[7]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[2]/div/div[8]/span/div/div/div/label")));
    }

    @Test
    public void browseUserEditModal() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div/div/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div[2]/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a")).click();

        seleniumDriver.findElement(By.xpath("//input[@name=\"userListContainer:search\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table/tbody/tr/td[5]/div/span[13]/a")));

        seleniumDriver.findElement(By.xpath("//table/tbody/tr/td[5]/div/span[13]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//form/div[3]/div/span/div/div/div[contains(text(),'Username')]")));

        seleniumDriver.switchTo().defaultContent();
        seleniumDriver.findElement(By.xpath("//a[@class='w_close']")).click();
    }

    @Test
    public void searchUsers() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));

        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div/div/span/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span/div/div/div/div/div[2]/div/div/span/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a")));

        seleniumDriver.findElement(By.xpath("//div/div/span[2]/span/span/div/form/div[2]/ul/li[9]/a")).click();

        seleniumDriver.findElement(By.xpath("//input[@name=\"userListContainer:search\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[15]/a")));
    }

    @Test
    public void deleteRole() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='navigationPane']")));
        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div[2]/div[2]/div/a")));
        seleniumDriver.findElement(By.xpath("//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div[2]/div[2]/div/a")).
                click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div[2]/div[2]/div[2]/div/div/a")));

        seleniumDriver.findElement(By.xpath(
                "//div[3]/div[1]/div[1]/span[1]/div/div/div/div/div[2]/div[2]/div[2]/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div[2]/div[2]/div/div[2]/div[3]/div/span[2]/a/span")));

        seleniumDriver.findElement(By.xpath(
                "//div/div/span/div/div/div/div/div[2]/div[2]/div[2]/div/div[2]/div[3]/div/span[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt='delete icon']")));

        seleniumDriver.findElement(By.xpath("//img[@alt='delete icon']")).click();

        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();
    }

    @Test
    public void issueSYNCOPE510() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'Search')]")));

        seleniumDriver.findElement(By.xpath("//span[contains(text(),'Search')]")).click();
        Select select = new Select(seleniumDriver.findElement(By.xpath("//td[2]/select")));
        select.selectByVisibleText("RESOURCE");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[3]/select[option='ws-target-resource-2']")));

        select = new Select(seleniumDriver.findElement(By.xpath("//td[3]/select")));
        select.selectByVisibleText("ws-target-resource-2");
        seleniumDriver.findElement(By.xpath("//form/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[3]/div[2]/div[2]/span/div[1]/span[1]/span/form/span/table/tbody/tr/td[3]/div")));
    }
}
