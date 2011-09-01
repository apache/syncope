/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.build;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal set-dynamic-properties
 */
public class DynamicPropertyMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}" required="true"
     */
    private transient MavenProject mavenProject;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {

        final Properties projectProps = mavenProject.getProperties();
        try {
            projectProps.put("urlencoded.java.io.tmpdir",
                    URLEncoder.encode(System.getProperty("java.io.tmpdir"),
                    "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            getLog().error("Reverting to non urlencoded", e);
            projectProps.put("urlencoded.java.io.tmpdir",
                    System.getProperty("java.io.tmpdir"));
        }
    }
}
