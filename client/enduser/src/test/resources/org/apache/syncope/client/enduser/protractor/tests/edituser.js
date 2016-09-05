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


describe('syncope enduser user edit', function () {

  function next() {
    element.all(by.id('next')).last().click();
  };


  function fillDropDownMenu(modelId, option) {
    this.item = element(by.model(modelId));
    this.item.click();
    element.all(by.id(option)).first().click();
    browser.driver.sleep(1500);
  }
  ;


  it('should edit user credentials', function () {
    browser.get('http://localhost:9080/syncope-enduser/app/');

    //login
    element(by.model('credentials.username')).sendKeys('bellini');
    element(by.model('credentials.password')).sendKeys('password');
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).then(function (language) {
      expect(language.length).toBe(3);
    });
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).get(0).click();
    element(by.id('login-btn')).click();

//    credential
//    element(by.model('user.username')).sendKeys('bellini123');
//    element(by.model('user.password')).sendKeys('password123');
//    element(by.model('confirmPassword.value')).sendKeys('password123');
    next();


    fillDropDownMenu('dynamicForm.selectedGroups', 'additional');
//    fillDropDownMenu('dynamicForm.selectedAuxClasses','csv');

  });
});

