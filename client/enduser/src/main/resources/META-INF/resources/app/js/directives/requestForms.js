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
        .directive('requestForms', ['UserRequestsService',
          function (UserRequestsService) {
            return {
              restrict: 'E',
              templateUrl: 'views/requestForms.html',
              scope: {
                user: "="
              },
              controller: function ($scope) {
                // Initialization
                $scope.query = {
                  user: $scope.user.username,
                  page: 1,
                  size: 10
                };

                var calculatePages = function () {
                  $scope.totalPages = Math.ceil($scope.forms.totalCount / $scope.query.size);
                  $scope.pages = _.range(1, $scope.totalPages + 1);
                };

                // <Pagination>
                $scope.reloadPage = function (page, size, successMsg) {
                  // update query pagination parameters
                  $scope.query.page = page;
                  $scope.query.size = size;
                  // recalculate pages
                  calculatePages();
                  if (page < 1 || page > $scope.totalPages) {
                    return;
                  }
                  // get current page of items

                  $scope.getUserRequestForms($scope.query, function (forms) {
                    $scope.forms = forms;
                    $scope.$parent.showSuccess(successMsg, $scope.$parent.notification);
                  });
                };
                // </Pagination>

                var init = function () {
                  $scope.getUserRequestForms({
                    user: $scope.user.username,
                    page: 1,
                    size: 10
                  }, function (requestsForms) {
                    $scope.forms = requestsForms;
                    calculatePages();
                    $scope.availableSizes = [{id: 1, value: 10}, {id: 2, value: 25}, {id: 3, value: 50}];
                    $scope.selectedSize = $scope.availableSizes[0];
                  });
                };

                $scope.getUserRequestForms = function (query, callback) {
                  UserRequestsService.getUserRequestForms(query).then(function (response) {
                    callback(response);
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error retrieving User Request Forms: ", errorMessage);
                  });
                };

                init();

                $scope.submit = function (form) {
                  UserRequestsService.submitForm(form).then(function (response) {
                    console.debug("Form successfully submitted");
                    $scope.$parent.showSuccess("Form successfully submitted", $scope.$parent.notification);
                    $scope.reloadPage($scope.query.page, $scope.query.size, "Form successfully submitted");
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error retrieving User Request Forms: ", errorMessage);
                    $scope.$parent.showError("Error: " + (errorMessage || response), $scope.$parent.notification);
                  });

                };
              }
            };
          }]);