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
        .directive('dynamicVirtualAttributes', function () {
            var getTemplateUrl = function () {
              return 'views/dynamicVirtualAttributes.html';
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
                  var currentVirSchemas = new Array();
                  for (var i = 0; i < $scope.dynamicForm.virSchemas.length; i++) {
                    if (schema == "own" && $scope.dynamicForm.virSchemas[i].key.indexOf('#') == -1) {
                      var attr = $scope.dynamicForm.virSchemas[i];
                      attr.simpleKey = $scope.dynamicForm.virSchemas[i].key;
                      currentVirSchemas.push($scope.dynamicForm.virSchemas[i]);
                    }
                    if ($scope.dynamicForm.virSchemas[i].key.indexOf(schema) > -1) {
                      var attr = $scope.dynamicForm.virSchemas[i];
                      attr.simpleKey = $scope.dynamicForm.virSchemas[i].key.substring($scope.dynamicForm.virSchemas[i].key.indexOf("#") + 1);
                      currentVirSchemas.push(attr);
                    }
                  }
                  return currentVirSchemas;
                };

                $scope.addVirtualAttributeField = function (virSchemaKey) {
                  console.debug("Add VIRTUAL value:", virSchemaKey);
                  console.debug(" ", ($scope.dynamicForm.virtualAttributeTable[virSchemaKey].fields.length));
                  $scope.dynamicForm.virtualAttributeTable[virSchemaKey].fields.push(virSchemaKey + "_" + ($scope.dynamicForm.virtualAttributeTable[virSchemaKey].fields.length));
                };

                $scope.removeVirtualAttributeField = function (virSchemaKey, index) {
                  console.debug("Remove VIRTUAL value: ", virSchemaKey);
                  console.debug("Remove VIRTUAL value: ", index);
                  $scope.dynamicForm.virtualAttributeTable[virSchemaKey].fields.splice(index, 1);
                  // clean user model
                  $scope.user.virAttrs[virSchemaKey].values.splice(index, 1);
                };
              }
            };
          });
