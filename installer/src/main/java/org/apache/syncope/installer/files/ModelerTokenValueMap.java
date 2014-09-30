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
package org.apache.syncope.installer.files;

public class ModelerTokenValueMap {

    public static final String PATH = "/oryx.debug.js-tokenValueMap.properties";

    public static final String FILE = "# Licensed to the Apache Software Foundation (ASF) under one\n"
            + "# or more contributor license agreements.  See the NOTICE file\n"
            + "# distributed with this work for additional information\n"
            + "# regarding copyright ownership.  The ASF licenses this file\n"
            + "# to you under the Apache License, Version 2.0 (the\n"
            + "# \"License\"); you may not use this file except in compliance\n"
            + "# with the License.  You may obtain a copy of the License at\n" + "#\n"
            + "#   http://www.apache.org/licenses/LICENSE-2.0\n" + "#\n"
            + "# Unless required by applicable law or agreed to in writing,\n"
            + "# software distributed under the License is distributed on an\n"
            + "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n"
            + "# KIND, either express or implied.  See the License for the\n"
            + "# specific language governing permissions and limitations\n" + "# under the License.\n"
            + "ORYX.CONFIG.ROOT_PATH \\=					\"../editor/\"; //TODO: Remove last slash!!=BASE_PATH =                                     window.location.toString().substr(0, window.location.toString().indexOf('/wicket'));\\nORYX.CONFIG.ROOT_PATH =				BASE_PATH + \"/activiti-modeler/editor/\";\n"
            + "ORYX.CONFIG.EXPLORER_PATH \\=				\"../explorer\";=ORYX.CONFIG.EXPLORER_PATH =			BASE_PATH + \"/activiti-modeler/explorer\";\n"
            + "ORYX.CONFIG.LIBS_PATH \\=					\"../libs\";=ORYX.CONFIG.LIBS_PATH =				BASE_PATH + \"/activiti-modeler/libs\";\n"
            + "ORYX.CONFIG.SYNTAXCHECKER_URL \\==ORYX.CONFIG.PLUGINS_CONFIG =			ORYX.CONFIG.ROOT_PATH + \"plugins.xml\";\\nORYX.CONFIG.SYNTAXCHECKER_URL =\n"
            + "this._baseUrl \\= \"../editor/stencilsets/bpmn2.0/\";=this._baseUrl = ORYX.CONFIG.ROOT_PATH + \"stencilsets/bpmn2.0/\";\n"
            + "this._source \\= \"../stencilsets/bpmn2.0/bpmn2.0.json\";=this._source = ORYX.CONFIG.ROOT_PATH + \"stencilsets/bpmn2.0/bpmn2.0.json\";\n"
            + "\"../service/editor/stencilset\"=ORYX.CONFIG.ROOT_PATH + \"stencilset.json\"\n"
            + "ORYX.Editor.createByUrl=modelUrl = BASE_PATH + \"/workflowDefGET\";\\nORYX.Editor.createByUrl\n"
            + "../explorer/src/img/signavio/smoky/logo2.png=\"+ORYX.CONFIG.EXPLORER_PATH+\"/src/img/signavio/smoky/logo2.png\n"
            + "<a href\\=\\\\\"\"+ORYX.CONFIG.WEB_URL+\"\\\\\" target\\=\\\\\"_self\\\\\" title\\=\\\\\"close modeler\\\\\">=<a href=\\\\\"#\\\\\" title=\\\\\"close modeler\\\\\" onclick=\\\\\"window.close();\\\\\">\n"
            + "../editor/images/close_button.png=\"+ORYX.CONFIG.ROOT_PATH+\"images/close_button.png\n"
            + "height:16px;width:16px;margin-bottom:-4px;background: transparent url(../libs/ext-2.0.2/resources/images/default/tree/loading.gif) no-repeat center;=height:16px;width:16px;margin-bottom:-4px;background: transparent url(\"+ORYX.CONFIG.LIBS_PATH+\"/ext-2.0.2/resources/images/default/tree/loading.gif) no-repeat center;\n"
            + "icon: '../editor/images/add.png',=icon: ORYX.CONFIG.ROOT_PATH + 'images/add.png',\n"
            + "icon: '../editor/images/delete.png',=icon: ORYX.CONFIG.ROOT_PATH + 'images/delete.png',\n"
            + "id\\=\"edit_model_title\"=id=\"edit_model_title\" readonly=\"readonly\"\n"
            + "id\\=\"edit_model_summary\"=id=\"edit_model_summary\" readonly=\"readonly\"\n"
            + "\"../service/model/\" + modelMeta.modelId + \"/json\"=BASE_PATH + \"/workflowDefGET\"\n"
            + "// Send the request to the server.=saveUri = BASE_PATH + \"/workflowDefPUT\";\\n// Send the request to the server.\n"
            + "'Accept':\"application/json\", 'Content-Type':'charset\\=UTF-8'='Accept':\"application/json\", 'Content-Type':'application/json'";

}
