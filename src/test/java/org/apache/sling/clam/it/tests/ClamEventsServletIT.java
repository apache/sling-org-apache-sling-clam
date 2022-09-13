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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.sling.clam.jcr.NodeDescendingJcrPropertyDigger;
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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ClamEventsServletIT extends ClamTestSupport {

    @Inject
    @Filter(value = "(path=/content/starter/img/sling-logo.svg)", timeout = 300000)
    private ResourcePresence resourcePresence;

    @Inject
    @Filter(value = "(service.description=Apache Sling Clam Events Servlet)")
    private JcrPropertyScanResultHandler jcrPropertyScanResultHandler;

    @Inject
    private NodeDescendingJcrPropertyDigger nodeDescendingJcrPropertyDigger;

    private static final String URL_TEMPLATE = "http://localhost:%s/system/clam-events";

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration(),
            clamdConfiguration(),
            slingResourcePresence(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/starter/img/sling-logo.svg")
                .asOption(),
            slingStarterContent(),
            // ok io/http/eventsource
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.okio").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.okhttp").versionAsInProject(),
            wrappedBundle(mavenBundle().groupId("com.launchdarkly").artifactId("okhttp-eventsource").versionAsInProject())
        );
    }

    @Test
    public void testAuthentication() throws Exception {
        final String url = String.format(URL_TEMPLATE, httpPort());
        given()
            .when()
            .get(url)
            .then()
            .statusCode(401);
    }

    @Test
    public void testContentType() throws Exception {
        final String url = String.format(URL_TEMPLATE, httpPort());
        given()
            .when()
            .auth()
            .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
            .get(url)
            .then()
            .contentType("text/event-stream;charset=utf-8");
    }

    @Test
    public void testEvents() throws Exception {
        final RecordingEventHandler recordingEventHandler = new RecordingEventHandler();

        final String url = String.format(URL_TEMPLATE, httpPort());
        final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new BasicAuthInterceptor(ADMIN_USERNAME, ADMIN_PASSWORD))
            .build();
        final EventSource eventSource = new EventSource.Builder(recordingEventHandler, URI.create(url))
            .client(client)
            .maxReconnectTimeMs(0)
            .reconnectTimeMs(0)
            .build();
        eventSource.start();

        digBinaries(nodeDescendingJcrPropertyDigger, "/content/starter");
        with().
            pollInterval(10, SECONDS).
            then().
            await().
            alias("counting results").
            atMost(1, MINUTES).
            until(() -> recordingEventHandler.countEvents() == 8);
    }

    static class BasicAuthInterceptor implements Interceptor {

        private final String credentials;

        BasicAuthInterceptor(final String user, final String password) {
            this.credentials = Credentials.basic(user, password);
        }

        @Override
        public Response intercept(final Chain chain) throws IOException {
            final Request request = chain.request()
                .newBuilder()
                .header("Authorization", credentials)
                .build();
            return chain.proceed(request);
        }

    }

    static class RecordingEventHandler implements EventHandler {

        private final List<String> events = new ArrayList<>();

        List<String> getEvents() {
            return events;
        }

        int countEvents() {
            return events.size();
        }

        @Override
        public void onOpen() throws Exception {
        }

        @Override
        public void onClosed() throws Exception {
        }

        @Override
        public void onMessage(final String event, final MessageEvent messageEvent) throws Exception {
            events.add(messageEvent.getData());
        }

        @Override
        public void onComment(final String comment) throws Exception {
        }

        @Override
        public void onError(final Throwable throwable) {
        }

    }

}
