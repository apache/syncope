/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if (typeof(Syncope) == "undefined") Syncope = { };
if (typeof(Syncope.SingleColumnPalette) == "undefined") Syncope.SingleColumnPalette = { };

Syncope.SingleColumnPalette.choicesOnFocus=function(choicesId, selectionId, recorderId) {
    var selection = Wicket.Palette.$(selectionId);
    var selected;
    for (var i = 0; i < selection.options.length; i++) {
        if (selection.options[i].selected) {
            selected = selection.options[i].value;
        }
    }

    if (typeof(selected) == "undefined") {
        alert("Please select an item first");
    } else {
        var recorder = Wicket.Palette.$(recorderId);  
        
        recorder.value=selected + "|";
        for (var i = 0; i < selection.options.length; i++) {
            recorder.value=recorder.value+selection.options[i].value;
            if (i+1<selection.options.length) {
                recorder.value=recorder.value+",";
            }
        }

        if (recorder.onselect != null) {
            recorder.onselect();
        }
    }
}
