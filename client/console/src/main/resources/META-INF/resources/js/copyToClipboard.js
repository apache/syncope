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
// Copy to clipboard
if (typeof copyToClipboard === 'undefined') {
  copyToClipboard = function (element, tag_value_to_copy, fake_textarea_ID, feedback_selector) {
    // creating new textarea element and giveing it id 't'
    var temp = document.createElement('textarea');
    temp.id = fake_textarea_ID;

    // Optional step to make less noise in the page, if any!
    temp.style.height = 0;

    // You have to append it to your page somewhere, I chose <body>
    $(document.body).append(temp);

    // Copy whatever is in your div to our new textarea
    temp.value = $(element).attr(tag_value_to_copy);

    // Now copy whatever inside the textarea to clipboard
    var selector = document.querySelector("#" + fake_textarea_ID);
    selector.select();

    document.execCommand('copy');

    // Remove the textarea
    $(temp).remove();

    $(feedback_selector).fadeIn();

    // Remove Message of feedback
    setTimeout(function () {
      $(feedback_selector).fadeOut();
    }, 1000);
  };

  $(document).off('click', '.label-with-key:visible');
  $(document).on('click', '.label-with-key:visible', function (e) {
    var feedback_selector = '.copy-clipboard-feedback';
    var fake_textarea_selector = 'tttt';
    var tag_value_to_copy = 'data-value';

    if (!$(feedback_selector + ":visible").length) {
      copyToClipboard(this, tag_value_to_copy, fake_textarea_selector, feedback_selector);
    }
    e.stopPropagation();
  });
}


