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
        .directive('dynamicPlainAttribute', function () {
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicPlainAttribute.html',
            scope: {
              schema: "=",
              index: "=",
              user: "="
            },
            controller: function ($scope, $rootScope, $element, $window) {
              $scope.schemaType = function (schema) {
                return schema.type == "Encrypted" && schema.conversionPattern == 'ENCRYPTED_DECODE_CONVERSION_PATTERN'
                        ? "String"
                        : schema.type;
              };
              $scope.initAttribute = function (schema, index) {
                switch (schema.type) {
                  case "Long":
                  case "Double":
                    $scope.user.plainAttrs[schema.key].values[index] = Number($scope.user.plainAttrs[schema.key]
                            .values[index]) || undefined;
                    break;
                  case "Enum":
                    $scope.enumerationValues = [];
                    $scope.enumerationKeys = [];

                    //SYNCOPE-911 empty value option on non required attributes 
                    if (schema.mandatoryCondition !== "true") {
                      $scope.enumerationValues.push("");
                      $scope.enumerationKeys.push("");
                    }
                    var enumerationValuesSplitted = schema.enumerationValues.toString().split(";");
                    for (var i = 0; i < enumerationValuesSplitted.length; i++) {
                      $scope.enumerationValues.push(enumerationValuesSplitted[i]);
                    }
                    //SYNCOPE-1024 enumeration keys mgmt
                    if (schema.enumerationKeys) {
                      var enumerationKeysSplitted = schema.enumerationKeys.toString().split(";");
                      for (var i = 0; i < enumerationKeysSplitted.length; i++) {
                        $scope.enumerationKeys.push(enumerationKeysSplitted[i]);
                      }
                    }
                    $scope.user.plainAttrs[schema.key].values[index] = $scope.user.plainAttrs[schema.key].values[index]
                            || $scope.enumerationValues[0];
                    break;

                  case "Binary":
                    $scope.userFile = $scope.userFile || '';
                    $element.bind("change", function (changeEvent) {
                      $scope.$apply(function () {
                        var reader = new FileReader();
                        var file = changeEvent.target.files[0];
                        $scope.userFile = file.name;
                        reader.onload = function (readerEvt) {
                          var binaryString = readerEvt.target.result;
                          $scope.user.plainAttrs[schema.key].values[index] = btoa(binaryString);
                        };
                        reader.readAsBinaryString(file);
                      });
                    });

                    $scope.download = function () {
                      var byteString = atob($scope.user.plainAttrs[schema.key].values[index]);
                      var ab = new ArrayBuffer(byteString.length);
                      var ia = new Uint8Array(ab);
                      for (var i = 0; i < byteString.length; i++) {
                        ia[i] = byteString.charCodeAt(i);
                      }
                      var blob = new Blob([ia], {type: schema.mimeType});
                      saveAs(blob, schema.key);
                    };

                    //file upload and preview                    
                    $('#fileInput').on('fileclear', function () {
                      $scope.user.plainAttrs[schema.key].values.splice(index, 1);
                    });
                    $scope.previewImg = $scope.user.plainAttrs[schema.key].values[index];
                    break;

                  case "Date":
                    $scope.getType = function (x) {
                      return typeof x;
                    };
                    $scope.isDate = function (x) {
                      return x instanceof Date;
                    };
                    $scope.languageid = $rootScope.languages.selectedLanguage.id;
                    $scope.isDateOnly = schema.conversionPattern.indexOf("H") === -1
                            && schema.conversionPattern.indexOf("h") === -1;
                    $scope.languageFormat = $scope.isDateOnly
                            ? $rootScope.languages.selectedLanguage.format.replace(" HH:mm", "")
                            : $rootScope.languages.selectedLanguage.format;
                    $scope.languageCulture = $rootScope.languages.selectedLanguage.culture;
                    // read date in milliseconds
                    $scope.selectedDate = new Date($scope.user.plainAttrs[schema.key].values[index] * 1);

                    $scope.bindDateToModel = function (selectedDate, extendedDate) {
                      if (selectedDate) {
                        // save date in milliseconds
                        $scope.user.plainAttrs[schema.key].values[index] = new Date(extendedDate).getTime();
                      }
                    };
                    break;

                  case "Boolean":
                    $scope.user.plainAttrs[schema.key].values[index] =
                            $scope.user.plainAttrs[schema.key].values[index] === "true" ? "true" : "false";
                    break;

                }
              };

              $scope.customReadonly = function (schemaKey) {
                return  $rootScope.customFormAttributes
                        && $rootScope.customFormAttributes["PLAIN"]
                        && $rootScope.customFormAttributes["PLAIN"]["attributes"]
                        && $rootScope.customFormAttributes["PLAIN"]["attributes"][schemaKey]
                        && $rootScope.customFormAttributes["PLAIN"]["attributes"][schemaKey].readonly;
              };

              $scope.$watch(function () {
                return $scope.user.plainAttrs[$scope.schema.key].values[$scope.index];
              }, function (newValue, oldValue) {
                $scope.user.plainAttrs[$scope.schema.key].values = $scope.user.plainAttrs[$scope.schema.key].values
                        .filter(function (n) {
                          return (n !== undefined && n !== "");
                        });
              });
            }
          };
        });
