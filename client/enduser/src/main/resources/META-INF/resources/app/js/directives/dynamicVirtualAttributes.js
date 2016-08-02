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
        .directive('dynamicVirtualAttributes', ['$compile', '$templateCache', function ($compile, $templateCache) {
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
              link: function ($scope) {
                //virtual schemas are loaded asyncronously, directive should refresh its template when they're available
                if ($scope.dynamicForm.virSchemas.length === 0) {
                  $scope.$watch('dynamicForm', function (newDynamicForm) {
                    if (newDynamicForm.virSchemas.length > 0) {
                      $compile($templateCache.get(getTemplateUrl()))($scope);

                    }
                  }, true);
                }
              },
              controller: function ($scope) {
                $scope.byGroup = {};

                $scope.splitByGroup = function (schemas) {
                  for (var i = 0; i < schemas.length; i++) {
                    var group;
                    var simpleKey;
                    if (schemas[i].key.indexOf('#') === -1) {
                      group = "own";
                      simpleKey = schemas[i].key;
                    } else {
                      group = schemas[i].key.substr(0, schemas[i].key.indexOf('#'));
                      simpleKey = schemas[i].key.substr(schemas[i].key.indexOf('#') + 1);
                    }
                    if (!$scope.byGroup[group]) {
                      $scope.byGroup[group] = new Array();
                    }
                    $scope.byGroup[group].push(schemas[i]);
                    schemas[i].simpleKey = simpleKey;
                  }
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
          }]);
