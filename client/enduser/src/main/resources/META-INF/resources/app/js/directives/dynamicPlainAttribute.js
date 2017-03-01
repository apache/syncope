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
        .directive('dynamicPlainAttribute', function ($filter) {
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicPlainAttribute.html',
            scope: {
              schema: "=",
              index: "=",
              user: "="
            },
            controller: function ($scope, $rootScope, $element, $window) {
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
                    if ( schema.enumerationKeys ) {
                      var enumerationKeysSplitted = schema.enumerationKeys.toString().split( ";" );
                      for ( var i = 0; i < enumerationKeysSplitted.length; i++ ) {
                        $scope.enumerationKeys.push( enumerationKeysSplitted[i] );
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

                    var dateInMs = $scope.user.plainAttrs[schema.key].values[index];
                    var temporaryDate = new Date(dateInMs * 1);
                    $scope.selectedDate = temporaryDate;
                    $scope.languageid = $rootScope.languages.selectedLanguage.id;
                    $scope.languageFormat = $rootScope.languages.selectedLanguage.format;
                    $scope.languageCulture = $rootScope.languages.selectedLanguage.culture;

                    $scope.bindDateToModel = function (selectedDate, extendedDate) {
                      if (selectedDate) {
                        var tmpdate = new Date(extendedDate);
                        var milliseconds = tmpdate.getTime();
                        $scope.user.plainAttrs[schema.key].values[index] = milliseconds;
                      }
                    };
                    break;

                  case "Boolean":
                    $scope.user.plainAttrs[schema.key].values[index] =
                            $scope.user.plainAttrs[schema.key].values[index] === "true" ? true : false;
                    break;

                }
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
