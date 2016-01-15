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

/**
 * This filter has been defined in order to have a workaround for this bug: 
 * https://github.com/angular-ui/ui-select/issues/665
 * 
 * When the bug is fixed, we can remove it.
 */

'use strict'

angular.module("self")
        .filter('propsFilter', function () {
          return function (items, props) {
            var out = [];            
            if (items && items.length && props.selected && props.selected.length) {
              var selected = props.selected;              
              for (var i = 0; i < items.length; i++) {
                var item = items[i], itemMisses = true;
                for (var j = 0; j < selected.length; j++) {
                  if (item.rightKey == selected[j].rightKey) {
                    itemMisses = false;
                    break;
                  }
                }
                if(itemMisses){
                  out.push(item);
                }
              }
            }
            else{
              out = items;
            }
            return out;
          };
        });
