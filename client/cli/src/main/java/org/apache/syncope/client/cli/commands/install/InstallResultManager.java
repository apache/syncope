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
package org.apache.syncope.client.cli.commands.install;

import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.ws.rs.ProcessingException;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.commands.CommonsResultManager;

public class InstallResultManager extends CommonsResultManager {

    public void printWelcome() {
        System.out.println("");
        System.out.println("###############################################");
        System.out.println("#                                             #");
        System.out.println("# Welcome to Syncope CLI installation process #");
        System.out.println("#                                             #");
        System.out.println("###############################################");
        System.out.println("");
    }

    public void installationSuccessful(final String version) {
        System.out.println("Installation parameters checked on Syncope core version: " + version);
        System.out.println("");
        System.out.println("###############################################");
        System.out.println("#                                             #");
        System.out.println("#           Installation successful           #");
        System.out.println("#     now you can use Syncope CLI client      #");
        System.out.println("#                                             #");
        System.out.println("###############################################");
        System.out.println("");
    }

    public void manageProcessingException(final ProcessingException ex) {
        if (ex.getCause() instanceof UnknownHostException) {
            final String unknownHost = ex.getCause().getMessage().split(":")[3];
            System.out.println("");
            System.out.println("Provided host:" + unknownHost);
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#            Provided unknown host!           #");
            System.out.println("#        START the installation AGAIN!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        } else if (ex.getCause() instanceof ConnectException) {
            System.out.println("");
            System.out.println("Provided address :" + SyncopeServices.getAddress());
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#       Provided address is unreachable!      #");
            System.out.println("#         Check it and if it is wrong         #");
            System.out.println("#        START the installation AGAIN!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        }
    }

    public void manageException(final Exception e) {
        if (e.getMessage().contains("not authenticated")) {
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#   Username or password provided are wrong   #");
            System.out.println("#        START the installation AGAIN!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        } else {
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#                Something wrong              #");
            System.out.println("#        START the installation AGAIN!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
            System.out.println(e.getMessage());
            System.out.println("");
        }
    }
}
