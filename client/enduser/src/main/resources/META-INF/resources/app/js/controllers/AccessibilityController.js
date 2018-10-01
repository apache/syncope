/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 **/

'use strict';
angular.module("home").controller("AccessibilityController", ['$scope', 'AssetsManager', '$window',
  function ($scope, AssetsManager, $window) {

    var darkThemeFiles = [
      'css/accessibility/accessibilityHC.css'
    ];

    var fontSizeFiles = [
      'css/accessibility/accessibilityFont.css'
    ];

    $scope.FONT_SIZE_PREF = "font_size_pref";
    $scope.HC_THEME_PREF = "hc_theme_pref";

    var doSwitch = function (check, files) {
      if (!check) {
        for (var i = 0; i < files.length; i++) {
          AssetsManager.remove(files[i], "css");
        }
      } else {
        for (var i = 0; i < files.length; i++) {
          AssetsManager.inject(AssetsManager.accessibilityCssId + "_" + i, files[i], "css");
        }
      }
    };

    var savePreference = function (key, value) {
      $window.localStorage.setItem(key, value);
    };

    var getPreference = function (key) {
      var storageValue = $window.localStorage.getItem(key);
      if (storageValue === null) {
        savePreference(key, "false");
      }
      return storageValue === 'true';
    };

    $scope.isHighContrast = getPreference($scope.HC_THEME_PREF);
    $scope.isIncreasedFont = getPreference($scope.FONT_SIZE_PREF);

    $scope.checkPref = function (pref) {
      switch (pref) {
        case $scope.FONT_SIZE_PREF:
          doSwitch($scope.isIncreasedFont, fontSizeFiles);
          break;

        case $scope.HC_THEME_PREF:
          doSwitch($scope.isHighContrast, darkThemeFiles);
          break;

        default:
          break;
      }
    };

    $scope.switchTheme = function () {
      $scope.isHighContrast = !$scope.isHighContrast;
      doSwitch($scope.isHighContrast, darkThemeFiles);
      savePreference($scope.HC_THEME_PREF, $scope.isHighContrast);
    };

    $scope.switchIncreasedFont = function () {
      $scope.isIncreasedFont = !$scope.isIncreasedFont;
      doSwitch($scope.isIncreasedFont, fontSizeFiles);
      savePreference($scope.FONT_SIZE_PREF, $scope.isIncreasedFont);
    };

  }]);