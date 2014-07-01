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

public class Commands {
            
    public static final String createArchetypeCommand
            = "mvn archetype:generate "
            + "-DarchetypeGroupId=org.apache.syncope "
            + "-DarchetypeArtifactId=syncope-archetype "
            + "-DarchetypeRepository=http://repo1.maven.org/maven2 "
            + "-DarchetypeVersion=%s "
            + "-DgroupId=%s -DartifactId=%s -DsecretKey=%s -DanonymousKey=%s -DinteractiveMode=false";

    public static final String compileCommand = "mvn clean package -Dlog.directory=%s -Dbundles.directory=%s ";

    public static final String createDirectory = "mkdir -p %s";

}
