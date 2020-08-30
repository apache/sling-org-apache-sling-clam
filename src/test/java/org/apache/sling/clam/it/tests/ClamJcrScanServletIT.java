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
package org.apache.sling.clam.it.tests;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.sling.clam.it.support.RecordingJcrPropertyScanResultHandler;
import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.resource.presence.ResourcePresence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingStarterContent;
import static org.awaitility.Awaitility.with;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ClamJcrScanServletIT extends ClamTestSupport {

    @Inject
    @Filter(value = "(path=/content/starter)", timeout = 300000)
    private ResourcePresence resourcePresence;

    private static final String URL_TEMPLATE = "http://localhost:%s/system/clam-jcr-scan";

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration(),
            clamdConfiguration(),
            slingResourcePresence(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/starter")
                .asOption(),
            slingStarterContent()
        );
    }

    @Test
    public void testAuthentication() throws Exception {
        final String url = String.format(URL_TEMPLATE, httpPort());
        given()
            .when()
            .post(url)
            .then()
            .statusCode(401);
    }

    @Test
    public void test() throws IOException {
        final RecordingJcrPropertyScanResultHandler scanResultHandler = new RecordingJcrPropertyScanResultHandler();
        bundleContext.registerService(JcrPropertyScanResultHandler.class, scanResultHandler, null);

        final String url = String.format(URL_TEMPLATE, httpPort());
        given()
            .auth()
            .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
            .param("path", "/content/starter")
            .when()
            .post(url)
            .then()
            .statusCode(200);

        with()
            .pollInterval(10, SECONDS)
            .then()
            .await()
            .alias("counting results")
            .atMost(1, MINUTES)
            .until(() -> scanResultHandler.getResults().size() == 8);
    }

}
