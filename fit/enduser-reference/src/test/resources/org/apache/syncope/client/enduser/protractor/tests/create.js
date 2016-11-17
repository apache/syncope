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

var abstract = require('./abstract.js');
describe('syncope enduser user create', function () {
  it('should create user donizetti', function () {
    console.log("");
    console.log("user create");
    abstract.goHome();
    browser.wait(element(by.id('register')).isPresent());
    element(by.id('register')).click();

    //creadentials
    browser.wait(element(by.id('user.username')).isPresent());
    element(by.model('user.username')).sendKeys('donizetti');
    element(by.model('user.password')).sendKeys('password123');
    element(by.model('confirmPassword.value')).sendKeys('password123');
    element(by.cssContainingText('option', 'What\'s your mother\'s maiden name?')).click();
    browser.wait(element(by.id('user.securityAnswer')).isPresent());
    element(by.model('user.securityAnswer')).sendKeys('Domenica Oliva Nava');
    element.all(by.id('next')).first().click();

    //groups
    browser.wait(element(by.model('user.realm')).isPresent());
    element(by.model('user.realm')).click();
    element.all(by.repeater('realm in availableRealms')).get(1).click();
    var group = element(by.model('dynamicForm.selectedGroups'));
    var selectedGroup = group.element(by.css('.ui-select-search'));
    group.click();
    //adds group root
    selectedGroup.sendKeys('root');
    element.all(by.css('.ui-select-choices-row-inner span')).first().click();
    abstract.waitSpinner();
    element.all(by.id('next')).first().click();

    //plainSchemas
    abstract.waitSpinner();
    browser.wait(element(by.name('fullname')).isPresent());
    element.all(by.name('fullname')).first().sendKeys('Gaetano Donizetti');
    element.all(by.name('userId')).first().sendKeys('donizetti@apache.org');
    element.all(by.name('firstname')).first().sendKeys('Gaetano');
    element.all(by.name('surname')).first().sendKeys('Donizetti');
    element.all(by.id('next')).first().click();

    //derivedSchemas,virtualSchemas,resources
    for (var i = 0; i < 3; i++) {
      element.all(by.id('next')).first().click();
    }
    //finish: breadcrumb should be clickable, testing navigation
    for (var i = 0; i < 5; i++) {
      element.all(by.repeater('(key, value) in wizard')).get(i).click();
      browser.wait(element(by.id('finish')).isPresent());
      element.all(by.id('finish')).last().click();
    }
    element.all(by.id('save')).last().click();
  });
});
