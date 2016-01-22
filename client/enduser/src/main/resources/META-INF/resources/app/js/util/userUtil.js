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

angular.module('SyncopeEnduserApp')
        .factory('UserUtil', ['$resource', '$q',
          function ($resource, $q) {

            var userUtil = {};

            //Prepare the user object passed as parameter to be compliant to the REST API.
            //return a clone of the initial user with those modifications.
            userUtil.getWrappedUser = function (user) {

              var wrappedUser = $.extend(true, {}, user);

              wrappedUser.plainAttrs = $.map(user.plainAttrs, function (el) {
                return el
              });
              wrappedUser.derAttrs = $.map(user.derAttrs, function (el) {
                return el
              });
              wrappedUser.virAttrs = $.map(user.virAttrs, function (el) {
                return el
              });
              wrappedUser["@class"] = "org.apache.syncope.common.lib.to.UserTO";

              return wrappedUser;
            };

            //Converts all the user attributes from an array of object to an associatice
            userUtil.getUnwrappedUser = function (user) {

              var unwrappedUser = $.extend(true, {}, user);

              unwrappedUser.plainAttrs = {}
              for (var i in user.plainAttrs) {
                unwrappedUser.plainAttrs[user.plainAttrs[i].schema] = user.plainAttrs[i];
              }
              unwrappedUser.derAttrs = {}
              for (var i in user.derAttrs) {
                unwrappedUser.derAttrs[user.derAttrs[i].schema] = user.derAttrs[i];
              }
              unwrappedUser.virAttrs = {}
              for (var i in user.virAttrs) {
                unwrappedUser.virAttrs[user.virAttrs[i].schema] = user.virAttrs[i];
              }

              return unwrappedUser;
            };

            return userUtil;

          }]);
