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
if (typeof copyToClipboard === 'undefined') {
  copyToClipboard = function (element, tag_value_to_copy, fake_textarea_ID, feedback_selector) {
    var $elem = $(element);

    // creating new textarea element and giveing it an ID
    var temp = document.createElement('textarea');
    temp.id = fake_textarea_ID;
    temp.style.display = 'block';

    // Make less noise in the page
    temp.style.height = 0;

    // Append it to the page somewhere, in this case <body>
    $elem.append(temp);

    // Copy whatever is in the div to our new textarea
    temp.value = $elem.attr(tag_value_to_copy);
    $(temp).text(temp.value);

    // Copy whatever inside the textarea to clipboard
    $(temp).focus().select();
    document.execCommand('SelectAll');
    document.execCommand("copy", false, null);

    var matched, browser;

    matched = jQuery.uaMatch( navigator.userAgent );
    browser = {};

    if ( matched.browser ) {
      browser[ matched.browser ] = true;
      browser.version = matched.version;
    }


    if (browser.mozilla && !browser.chrome) {
      try {
        var range = document.createRange();
        range.selectNodeContents(temp);
        var selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);

        document.execCommand("Copy", false, null);
      } catch (e) {
      }
    }

    // Remove the textarea
    $(temp).remove();

    $elem.siblings(feedback_selector).fadeIn();

    // Remove feedback element
    window.setTimeout(function () {
      $elem.siblings(feedback_selector).fadeOut();
    }, 1000);
  };

  function doCopyToClipboard(el) {
    var feedback_selector = '.copy-clipboard-feedback';
    var fake_textarea_selector = 'tttt';
    var tag_value_to_copy = 'data-value';

    if (!$(feedback_selector + ":visible").length) {
      copyToClipboard(el, tag_value_to_copy, fake_textarea_selector, feedback_selector);
    }
  }

  jQuery.uaMatch = function( ua ) {
    ua = ua.toLowerCase();

    var match = /(chrome)[ \/]([\w.]+)/.exec( ua ) ||
        /(webkit)[ \/]([\w.]+)/.exec( ua ) ||
        /(opera)(?:.*version|)[ \/]([\w.]+)/.exec( ua ) ||
        /(msie)[\s?]([\w.]+)/.exec( ua ) ||
        /(trident)(?:.*? rv:([\w.]+)|)/.exec( ua ) ||
        ua.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec( ua ) ||
        [];

    return {
      browser: match[ 1 ] || "",
      version: match[ 2 ] || "0"
    };
  };
}


