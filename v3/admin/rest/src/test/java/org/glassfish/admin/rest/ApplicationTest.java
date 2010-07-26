/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.admin.rest;

import com.sun.jersey.api.client.ClientResponse;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jasonlee
 */
public class ApplicationTest extends RestTestBase {
    public static final String URL_APPLICATION_DEPLOY = "/domain/applications/application";
    public static final String URL_CREATE_INSTANCE = "/domain/create-instance";
    public static final String URL_SUB_COMPONENTS = "/domain/applications/application/list-sub-components";

    @Test
    public void testApplicationDeployment() {
        final String appName = "testApp" + generateRandomString();
        Map<String, Object> newApp = new HashMap<String, Object>() {{
                put("id", new File("src/test/resources/test.war"));
                put("contextroot", appName);
                put("name", appName);
        }};

        Map<String, String> deployedApp = deployApp(newApp);
        assertEquals(appName, deployedApp.get("name"));

        assertEquals("/" + appName, deployedApp.get("contextRoot"));

        undeployApp(newApp);
    }

    @Test
    public void testApplicationDisableEnable() {
        final String appName = "testApp" + generateRandomString();
        Map<String, Object> newApp = new HashMap<String, Object>() {{
            put("id", new File("src/test/resources/test.war"));
            put("contextroot", appName);
            put("name", appName);
        }};

        Map<String, String> deployedApp = deployApp(newApp);
        assertEquals(appName, deployedApp.get("name"));

        assertEquals("/" + appName, deployedApp.get("contextRoot"));

        try {
            String appUrl = "http://localhost:" + getParameter("instance.port", "8080") + "/" + appName;
            ClientResponse response = get(appUrl);
            assertEquals ("Test", response.getEntity(String.class).trim());

            response = post(URL_APPLICATION_DEPLOY + "/" + newApp.get("name") + "/disable");
            assertTrue("Response was " + response.getStatus(), isSuccess(response));

            response = get(appUrl);
            assertFalse("Response was " + response.getStatus(), isSuccess(response));

            response = post(URL_APPLICATION_DEPLOY + "/" + newApp.get("name") + "/enable");
            assertTrue("Response was " + response.getStatus(), isSuccess(response));

            response = get(appUrl);
            assertEquals ("Test", response.getEntity(String.class).trim());
        } finally {
            undeployApp(newApp);
        }
    }

    @Test
    public void listSubComponents() {
        final String appName = "testApp" + generateRandomString();
        Map<String, Object> newApp = new HashMap<String, Object>() {{
                put("id", new File("src/test/resources/stateless-simple.ear"));
                put("contextroot", appName);
                put("name", appName);
        }};

        try {
            deployApp(newApp);
            ClientResponse response = get(URL_SUB_COMPONENTS + "?id=" + appName);
            assertTrue(isSuccess(response));
            String subComponents = response.getEntity(String.class);
            assertTrue(subComponents.contains("stateless-simple.war"));

            response = get(URL_SUB_COMPONENTS + "?id=stateless-simple.war&appname=" + appName);
            assertTrue(isSuccess(response));
            subComponents = response.getEntity(String.class);
            assertTrue(subComponents.contains("GreeterServlet"));
        } finally {
            undeployApp(newApp);
        }
    }

    @Test
    public void testCreatingAndDeletingApplicationRefs() {
        final String instanceName = "instance_" + generateRandomString();
        final String appName = "testApp" + generateRandomString();
        final String appRefUrl = "/domain/servers/server/" + instanceName + "/application-ref";
        Map<String, Object> newApp = new HashMap<String, Object>() {{
            put("id", new File("src/test/resources/test.war"));
            put("contextroot", appName);
            put("name", appName);
        }};
        Map<String, String> newInstance = new HashMap<String, String>() {{
            put("id", instanceName);
            put("node", "localhost");
        }};
        Map<String, String> applicationRef = new HashMap<String, String>() {{
            put("ref", appName);
        }};

        try {
            ClientResponse response = post(URL_CREATE_INSTANCE, newInstance);
            assertTrue(isSuccess(response));

            deployApp(newApp);

            response = post (appRefUrl, applicationRef);
            assertTrue(isSuccess(response));

            response = get(appRefUrl + "/" + appName);
            assertTrue(isSuccess(response));
            
            response = delete(appRefUrl + "/" + appName, new HashMap<String, String>() {{ put("target", instanceName); }});
            assertTrue(isSuccess(response));
        } finally {
            ClientResponse response = delete("/domain/servers/server/" + instanceName + "/delete-instance");
            assertTrue(isSuccess(response));
            response = get("/domain/servers/server/" + instanceName);
            assertFalse(isSuccess(response));
            undeployApp(newApp);
        }
    }

    @Test
    public void testGetContextRoot() {
        final String appName = "testApp" + generateRandomString();
        Map<String, Object> newApp = new HashMap<String, Object>() {{
            put("id", new File("src/test/resources/stateless-simple.ear"));
            put("contextroot", appName);
            put("name", appName);
        }};

        try {
            Map<String, String> deployedApp = deployApp(newApp);
            assertEquals(appName, deployedApp.get("name"));
            Map<String, String> contextRootPayload = new HashMap<String, String>() {{
                put("appname", appName);
                put("modulename", "stateless-simple.war");
            }};

            ClientResponse response = get("/domain/applications/application/get-context-root", contextRootPayload);
            assertTrue(isSuccess(response));
            assertTrue(response.getEntity(String.class).contains("property name=\"contextRoot\" value=\""));
        } finally {
            undeployApp(newApp);
        }
    }

    protected Map<String, String> deployApp(Map<String, Object> app) {
        ClientResponse response = postWithUpload(URL_APPLICATION_DEPLOY, app);
        assertTrue("Response was " + response.getStatus(), isSuccess(response));

        return getEntityValues(get(URL_APPLICATION_DEPLOY + "/" + app.get("name")));
    }

    protected void undeployApp(Map<String, Object> app) {
        ClientResponse response = delete(URL_APPLICATION_DEPLOY + "/" + app.get("name"));
        assertTrue("Response was " + response.getStatus(), isSuccess(response));
    }
}
