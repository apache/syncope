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
'use strict';

angular.module('self')
        .directive('formProperty', ['$rootScope', function ($rootScope) {
            return {
              restrict: 'E',
              templateUrl: 'views/formProperty.html',
              scope: {
                property: "="
              },
              controller: function ($scope) {
                $scope.initAttribute = function () {
                  switch ($scope.property.type) {
                    case "Long":
                      $scope.property.value = Number($scope.property.value) || undefined;
                      break;
                    case "Enum":
                      if ($scope.property.required !== "true") {
                        $scope.property.enumValues.empty = "";
                      }
                      $scope.enumKeys = Object.keys($scope.property.enumValues);
                      $scope.property.value = $scope.property.value || $scope.property.enumValues.empty;
                      break;
                    case "Dropdown":
                      if ($scope.property.required !== "true") {
                        $scope.property.dropdownValues.empty = "";
                      }
                      $scope.dropdownKeys = Object.keys($scope.property.dropdownValues);
                      $scope.property.value = $scope.property.value || $scope.property.dropdownValues.empty;
                      break;
                    case "Date":
                      $scope.getType = function (x) {
                        return typeof x;
                      };
                      $scope.isDate = function (x) {
                        return x instanceof Date;
                      };
                      $scope.languageid = $rootScope.languages.selectedLanguage.id;
                      $scope.isDateOnly = $scope.property.datePattern.indexOf("H") === -1
                              && $scope.property.datePattern.indexOf("h") === -1;
                      $scope.languageFormat = $scope.isDateOnly
                              ? $rootScope.languages.selectedLanguage.format.replace(" HH:mm", "")
                              : $rootScope.languages.selectedLanguage.format;
                      $scope.languageCulture = $rootScope.languages.selectedLanguage.culture;
                      // read date in milliseconds
                      $scope.selectedDate = $scope.property.value === null
                              ? undefined
                              : new Date($scope.property.value * 1);
                      $scope.bindDateToModel = function (selectedDate, extendedDate) {
                        if (selectedDate) {
                          // save date in milliseconds
                          $scope.property.value = new Date(extendedDate).getTime();
                        }
                      };
                      break;
                    case "Boolean":
                      $scope.property.value = $scope.property.value === "true" ? "true" : "false";
                      break;

                  }
                };

              }
            };
          }]);