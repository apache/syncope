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
            controller: function ($scope, $element, $window) {
              $scope.initAttribute = function (schema, index) {
                switch (schema.type) {
                  case "Long":
                  case "Double":
                    $scope.user.plainAttrs[schema.key].values[index] = Number($scope.user.plainAttrs[schema.key]
                            .values[index]) || undefined;
                    break;
                  case "Enum":
                    $scope.enumerationValues = [];

                    //SYNCOPE-911 empty value option on non required attributes 
                    if (schema.mandatoryCondition !== "true") {
                      $scope.enumerationValues.push("");
                    }
                    var enumerationValuesSplitted = schema.enumerationValues.toString().split(";");
                    for (var i = 0; i < enumerationValuesSplitted.length; i++) {
                      $scope.enumerationValues.push(enumerationValuesSplitted[i]);
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
                    var dateInMs = $scope.user.plainAttrs[schema.key].values[index];
                    if (dateInMs) {
                      var temporaryDate = new Date(dateInMs * 1);
                      $scope.selectedDate = temporaryDate;
                      $scope.selectedTime = temporaryDate;
                    }

                    $scope.bindDateToModel = function (selectedDate, selectedTime) {
                      if (selectedDate && selectedTime) {
                        var extractedDate = selectedDate.toString().substring(0, 15);
                        var extractedTime = selectedTime.toString().substring(16);
                        var resultDate = extractedDate + ' ' + extractedTime;
                        var tmpdate = new Date(resultDate);
                        var milliseconds = tmpdate.getTime();
                        $scope.user.plainAttrs[schema.key].values[index] = milliseconds;
                      }
                    };

                    $scope.clear = function () {
                      $scope.user.plainAttrs[schema.key].values[index] = null;
                    };

                    // Disable weekend selection
                    $scope.disabled = function (date, mode) {
                      // if you want to disable weekends:
                      // return (mode === 'day' && (date.getDay() === 0 || date.getDay() === 6));
                      return false;
                    };

                    $scope.toggleMin = function () {
                      $scope.minDate = $scope.minDate ? null : new Date();
                    };

                    $scope.maxDate = new Date(2050, 5, 22);

                    $scope.open = function ($event) {
                      $scope.status.opened = true;
                    };

                    $scope.setDate = function (year, month, day) {
                      $scope.user.plainAttrs[schema.key].values[index] = new Date(year, month, day);
                    };

                    $scope.dateOptions = {
                      startingDay: 1
                    };

                    $scope.status = {
                      opened: false
                    };

                    var tomorrow = new Date();
                    tomorrow.setDate(tomorrow.getDate() + 1);
                    var afterTomorrow = new Date();
                    afterTomorrow.setDate(tomorrow.getDate() + 2);
                    $scope.events =
                            [
                              {
                                date: tomorrow,
                                status: 'full'
                              },
                              {
                                date: afterTomorrow,
                                status: 'partially'
                              }
                            ];

                    $scope.getDayClass = function (date, mode) {
                      if (mode === 'day') {
                        var dayToCheck = new Date(date).setHours(0, 0, 0, 0);

                        for (var i = 0; i < $scope.events.length; i++) {
                          var currentDay = new Date($scope.events[i].date).setHours(0, 0, 0, 0);

                          if (dayToCheck === currentDay) {
                            return $scope.events[i].status;
                          }
                        }
                      }

                    };

                    //TIME PICKER
                    $scope.selectedTime = $scope.selectedDate;
                    $scope.hstep = 1;
                    $scope.mstep = 1;

                    $scope.options = {
                      hstep: [1, 2, 3],
                      mstep: [1, 5, 10, 15, 25, 30]
                    };

                    $scope.ismeridian = true;
                    $scope.toggleMode = function () {
                      $scope.ismeridian = !$scope.ismeridian;
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
