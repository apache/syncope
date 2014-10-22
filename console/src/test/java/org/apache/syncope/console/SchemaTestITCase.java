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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class SchemaTestITCase extends AbstractTest {

    @Test
    public void create() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Schema\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='rschema']/div/div/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@name='name:textField']")));

        Select select = new Select(seleniumDriver.findElement(By.name("type:dropDownChoiceField")));
        select.selectByValue("0");
        WebElement textField = seleniumDriver.findElement(By.name("name:textField"));
        textField.sendKeys("newschema");
        seleniumDriver.findElement(By.name("apply")).click();

        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(seleniumDriver.findElement(By.tagName("body")).getText().contains("newschema"));
    }

    @Test
    public void delete() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Schema\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(
                By.xpath("//div[3]/div/div/div/div/div/span/table/tbody/tr/td[7]/div/span[15]/a")).click();
        
        Alert alert = seleniumDriver.switchTo().alert();
        assertTrue(alert.getText().equals("Do you really want to delete the selected item(s)?"));
        alert.accept();
    }
}
