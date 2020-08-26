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

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingStarterContent;
import static org.awaitility.Awaitility.with;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ResourcePersistingScanResultHandlerIT extends ClamTestSupport {

    @Inject
    @Filter("(service.pid=org.apache.sling.clam.result.internal.ResourcePersistingScanResultHandler)")
    private JcrPropertyScanResultHandler jcrPropertyScanResultHandler;

    @Inject
    @Filter(value = "(path=/content/starter)", timeout = 300000)
    private ResourcePresence resourcePresence;

    @Inject
    private NodeDescendingJcrPropertyDigger nodeDescendingJcrPropertyDigger;

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration(),
            clamdConfiguration(),
            slingResourcePresence(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/starter")
                .asOption(),
            newConfiguration("org.apache.sling.clam.result.internal.ResourcePersistingScanResultHandler")
                .put("result.status.ok.persist", true)
                .put("result.root.path", "/var/clam/results")
                .asOption(),
            slingStarterContent()
        );
    }

    @Test
    public void testJcrPropertyScanResultHandler() {
        assertThat(jcrPropertyScanResultHandler).isNotNull();
    }

    @Test
    public void testPersistedResults() throws Exception {
        digBinaries(nodeDescendingJcrPropertyDigger, "/content/starter");
        with().
            pollInterval(10, SECONDS).
            then().
            await().
            alias("counting results").
            atMost(1, MINUTES).
            until(() -> countResult() == 12);
    }

    protected QueryResult queryJcrResults(final Session session) throws RepositoryException {
        final String query = "SELECT * FROM [nt:unstructured] AS result WHERE ISDESCENDANTNODE(result, \"/var/clam/results\") AND [sling:resourceType] = \"sling/clam/jcr/result\"";
        return session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute();
    }

    private long countResult() throws RepositoryException {
        Session session = null;
        try {
            session = session();
            final QueryResult result = queryJcrResults(session);
            return result.getNodes().getSize();
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
