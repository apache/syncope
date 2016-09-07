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

//var abstract = require('./abstract.js');

describe('syncope enduser user edit', function () {

  function next() {
    element.all(by.id('next')).last().click();
  }
  ;

  function cancel() {
    element.all(by.id('cancel')).last().click();
  }
  ;

  it('should edit user credentials', function () {
    browser.get('http://localhost:9080/syncope-enduser/app/');

    //login
    element(by.model('credentials.username')).sendKeys('bellini');
    element(by.model('credentials.password')).sendKeys('password');
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).
            then(function (language) {
              expect(language.length).toBe(3);
            });
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).
            get(0).click();
    element(by.id('login-btn')).click();

//    abstract.doLogin("bellini", "password");

    //credential
    element(by.model('user.username')).clear();
    element(by.model('user.username')).sendKeys('bellini');
    element(by.model('user.password')).clear();
    element(by.model('user.password')).sendKeys('password');
    element(by.model('confirmPassword.value')).sendKeys('password');
    var secQuestion = element(by.model('user.securityQuestion'));
    var selectedSecQuestion = secQuestion.all(by.options
            ('securityQuestion.key as securityQuestion.content for securityQuestion in availableSecurityQuestions'))
            .last();
    selectedSecQuestion.click();
    element(by.model('user.securityAnswer')).sendKeys('Agata Ferlito');
    browser.driver.sleep(1000);
    next();

    //groups
    var group = element(by.model('dynamicForm.selectedGroups'));
    var selectedGroup = group.element(by.css('.ui-select-search'));
    group.click();

    // add "additional", a group with type extensions, and "root".
    selectedGroup.sendKeys('additional');
    element.all(by.css('.ui-select-choices-row-inner span')).first().click();
    selectedGroup.sendKeys('root');
    element.all(by.css('.ui-select-choices-row-inner span')).first().click();
    browser.driver.sleep(1000);
    next();

    //plainSchemas
    //  count groups in plainschemas: "own" and "additional".
    element.all(by.repeater('groupSchema in dynamicForm.groupSchemas')).then(function (groupSchema) {
      expect(groupSchema.length).toBe(2);
    })
    //  fills own fields
    element(by.css('[name="fullname"]')).clear();
    element(by.css('[name="fullname"]')).sendKeys('Vincenzo Bellini');
    element(by.css('[name="userId"]')).clear();
    element(by.css('[name="userId"]')).sendKeys('bellini@apache.org');
    var selectedDate = element(by.model('selectedDate')).click();
    selectedDate.clear();
    selectedDate.sendKeys('2009-06-21');
    element(by.css('[name="firstname"]')).clear();
    element(by.css('[name="firstname"]')).sendKeys('Vincenzo');
    element(by.css('[name="surname"]')).clear();
    element(by.css('[name="surname"]')).sendKeys('Bellini');
    element(by.css('[name="ctype"]')).clear();
    element(by.css('[name="ctype"]')).sendKeys('bellinictype');
    next();
    //derSchemas
    next();
    //virSchemas
    next();
    //Resources
    next();
    browser.driver.sleep(1000);
    element.all(by.id('cancel')).last().click();
    browser.driver.sleep(1000);
  });
});
