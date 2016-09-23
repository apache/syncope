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
exports.goHome = function () {
  var home = 'http://localhost:9080/syncope-enduser/app/';
  browser.get(home);
};

exports.doNext = function () {
  element.all(by.id('next')).last().click();
};

exports.doCancel = function () {
  element.all(by.id('cancel')).last().click();
};

exports.doSave = function () {
  element.all(by.id('save')).last().click();
};

exports.waitSpinner = function () {
  element.all(by.css('treasure-overlay-spinner')).isDisplayed().then(function (result) {
    if (result) {
      browser.driver.sleep(3000);
    }
  });
}
;