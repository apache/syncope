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
        .directive('dynamicPlainAttributes', function () {
            var getTemplateUrl = function () {
              return 'views/dynamicPlainAttributes.html';
            };
            return {
              restrict: 'E',
              templateUrl: getTemplateUrl(),
              scope: {
                dynamicForm: "=form",
                user: "="
              },
              controller: function ($scope) {
                
                $scope.getByGroup = function (schema) {
                  var currentPlainSchemas = new Array();
                  for (var i = 0; i < $scope.dynamicForm.plainSchemas.length; i++) {
                    if (schema == "own" && $scope.dynamicForm.plainSchemas[i].key.indexOf('#') == -1) {
                      var attr = $scope.dynamicForm.plainSchemas[i];
                      attr.simpleKey = $scope.dynamicForm.plainSchemas[i].key;
                      currentPlainSchemas.push($scope.dynamicForm.plainSchemas[i]);
                    }
                    if ($scope.dynamicForm.plainSchemas[i].key.indexOf(schema) > -1) {
                      var attr = $scope.dynamicForm.plainSchemas[i];
                      attr.simpleKey = $scope.dynamicForm.plainSchemas[i].key.substring($scope.dynamicForm.plainSchemas[i].key.indexOf("#") + 1);
                      currentPlainSchemas.push(attr);
                    }
                  }
                  return currentPlainSchemas;
                };

                $scope.addAttributeField = function (plainSchemaKey) {
                  console.debug("Add PLAIN value:", plainSchemaKey);
                  console.debug(" ", ($scope.dynamicForm.attributeTable[plainSchemaKey].fields.length));
                  $scope.dynamicForm.attributeTable[plainSchemaKey].fields.push(plainSchemaKey + "_" + ($scope.dynamicForm.attributeTable[plainSchemaKey].fields.length));
                };

                $scope.removeAttributeField = function (plainSchemaKey, index) {
                  console.debug("Remove PLAIN value:", plainSchemaKey);
                  console.debug("attribute index:", index);
                  $scope.dynamicForm.attributeTable[plainSchemaKey].fields.splice(index, 1);
                  // clean user model
                  $scope.user.plainAttrs[plainSchemaKey].values.splice(index, 1);
                };

                $scope.getTemplateUrl = function () {
                  return "views/dynamicPlainAttributes.html";
                };
              }
            };
          });
