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

angular.module('SyncopeEnduserApp').
        directive('validationMessage', function () {
          return {
            templateUrl: 'views/validationMessage.html',
            scope: {
              "name": "@",
              "template": "@"
            },
            require: '^?form',
            replace: false,
            link: function (scope, el, attrs, formCtrl) {
              var popupMessage = "Data are invalid: please correct accordingly";
              //listener
              scope.$on(attrs.name, function (el, errors) {
                scope.errors = errors;
                //hide popup error message if there aren't errors
                if ($.isEmptyObject(formCtrl.$error)) {
                  scope.$emit("hideErrorMessage", popupMessage);
                }
              });
            }
          };
        });


angular.module('SyncopeEnduserApp').
        directive('customMessage', function ($templateRequest, $compile) {
          return {
            scope: {
              "customMessage": "=",
              "fieldName": "=",
              "key": "="
            },
            replace: false,
            link: function (scope, el, attrs) {
              var ngswitch = $compile("<div ng-switch='key'></div>")(scope);
              if (scope.customMessage) {
                var templates = scope.customMessage.split(","), default_path = "views/";
                for (var i = 0; i < templates.length; i++) {
                  var templateUrl = default_path + templates[i] + ".html";
                  $templateRequest(templateUrl).then(function (html) {
                    // Convert the html to an actual DOM node                                       
                    ngswitch.prepend(html);
                    //defining default message
                    ngswitch.append("<div ng-switch-default>{{key}} validation rule is not satisfied</div>")
                    $compile(ngswitch)(scope);
                  });
                }
              }
              else {
                ngswitch.append("<div ng-switch-default>{{key}} validation rule is not satisfied</div>")
              }
              $compile(ngswitch)(scope);
              el.append(ngswitch)
            }
          };
        });