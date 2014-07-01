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
package org.apache.syncope.installer.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;

public class HttpUtils {

    private static final String APPLICATION_JSON = "application/json";

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final String UTF_8 = "UTF-8";

    private final HttpClient httpClient;

    public HttpUtils(final String username, final String password) {
        httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    }

    public int getWithBasicAuth(final String url) {
        final HttpMethod method = new GetMethod(url);
        final List authPrefs = new ArrayList();
        authPrefs.add(AuthPolicy.BASIC);
        httpClient.getParams().setParameter(AuthPolicy.BASIC, authPrefs);
        int status = 0;
        try {
            status = httpClient.executeMethod(method);
        } catch (IOException ex) {
        }
        return status;
    }

    public String postWithDigestAuth(final String url, final String file) {
        String responseBodyAsString = "";
        try {
            final PostMethod addContentPost = new PostMethod(url);
            final PartSource partSource = new FilePartSource(new File(file));

            final String[] tmp = file.split("/");
            final String fileName = tmp[tmp.length - 1].split("\\.")[0];
            final Part[] parts = {new FilePart(fileName, partSource, MULTIPART_FORM_DATA, UTF_8)};
            final MultipartRequestEntity mre = new MultipartRequestEntity(parts, addContentPost.getParams());
            addContentPost.setRequestEntity(mre);
            final List authPrefs = new ArrayList();
            authPrefs.add(AuthPolicy.DIGEST);

            httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
            httpClient.executeMethod(addContentPost);
            responseBodyAsString = addContentPost.getResponseBodyAsString();
        } catch (IOException ioe) {
        }
        return responseBodyAsString;
    }

    public int postWithStringEntity(final String url, final String stringEntity) {
        int status = 0;
        try {
            final StringRequestEntity requestEntity = new StringRequestEntity(stringEntity, APPLICATION_JSON, UTF_8);
            final PostMethod enablePost = new PostMethod(url);
            enablePost.setRequestEntity(requestEntity);
            status = httpClient.executeMethod(enablePost);
        } catch (IOException uee) {
        }
        return status;
    }
}
