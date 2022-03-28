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

import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.clam.it.support.ClamContainerFactory;
import org.apache.sling.clam.jcr.NodeDescendingJcrPropertyDigger;
import org.apache.sling.commons.clam.ClamService;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.options.OptionalCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.osgi.framework.BundleContext;

import static org.apache.sling.testing.paxexam.SlingOptions.awaitility;
import static org.apache.sling.testing.paxexam.SlingOptions.restassured;
import static org.apache.sling.testing.paxexam.SlingOptions.slingEvent;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.testcontainers;
import static org.apache.sling.testing.paxexam.SlingVersionResolver.SLING_GROUP_ID;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public abstract class ClamTestSupport extends TestSupport {

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected SlingRepository slingRepository;

    @Inject
    protected ClamService clamService;

    @Inject
    protected JobManager jobManager;

    static final String ADMIN_USERNAME = "admin";

    static final String ADMIN_PASSWORD = "admin";

    protected ModifiableCompositeOption baseConfiguration() {
        SlingOptions.versionResolver.setVersionFromProject(SLING_GROUP_ID, "org.apache.sling.commons.threads");
        return composite(
            // Truth first to prevent version issues with Guava
            wrappedBundle(mavenBundle().groupId("com.google.truth").artifactId("truth").versionAsInProject()),
            mavenBundle().groupId("com.google.guava").artifactId("guava").versionAsInProject(),
            mavenBundle().groupId("com.google.guava").artifactId("failureaccess").versionAsInProject(),
            mavenBundle().groupId("com.googlecode.java-diff-utils").artifactId("diffutils").versionAsInProject(),
            super.baseConfiguration(),
            quickstart(),
            // Sling Clam
            testBundle("bundle.filename"),
            factoryConfiguration("org.apache.sling.jcr.repoinit.RepositoryInitializer")
                .put("scripts", new String[]{"create service user sling-clam\ncreate path (sling:Folder) /var/clam/results\nset ACL for sling-clam\nallow jcr:all on /var/clam\nend"})
                .asOption(),
            factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                .put("user.mapping", new String[]{"org.apache.sling.clam=sling-clam", "org.apache.sling.clam:result-writer=sling-clam"})
                .asOption(),
            // Sling Commons Clam
            mavenBundle().groupId(SLING_GROUP_ID).artifactId("org.apache.sling.commons.clam").versionAsInProject(),
            // testing
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "PAXEXAM-PROBE-.*")
                .asOption(),
            junitBundles(),
            awaitility(),
            restassured(),
            testcontainers(),
            jacoco() // remove with Testing PaxExam 4.0
        );
    }

    // remove with Testing PaxExam 4.0
    protected OptionalCompositeOption jacoco() {
        final String jacocoCommand = System.getProperty("jacoco.command");
        final VMOption option = Objects.nonNull(jacocoCommand) && !jacocoCommand.trim().isEmpty() ? vmOption(jacocoCommand) : null;
        return when(Objects.nonNull(option)).useOptions(option);
    }

    protected Option quickstart() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort),
            slingEvent()
        );
    }

    protected Option clamdConfiguration() {
        final boolean testcontainer = Boolean.parseBoolean(System.getProperty("clamd.testcontainer", "true"));
        final String host;
        final Integer port;
        if (testcontainer) {
            ClamContainerFactory.startContainer();
            host = ClamContainerFactory.container.getContainerIpAddress();
            port = ClamContainerFactory.container.getFirstMappedPort();
        } else {
            host = System.getProperty("clamd.host", "localhost");
            port = Integer.parseInt(System.getProperty("clamd.port", "3310"));
        }
        return newConfiguration("org.apache.sling.commons.clam.internal.ClamdService")
            .put("clamd.host", host)
            .put("clamd.port", port)
            .asOption();
    }

    protected Session session() throws RepositoryException {
        return slingRepository.loginAdministrative(null);
    }

    protected void digBinaries(final NodeDescendingJcrPropertyDigger digger, final String path) throws Exception {
        Session session = null;
        try {
            session = session();
            final Node starter = session.getNode(path);
            final Pattern pattern = Pattern.compile("^/.*$");
            digger.dig(starter, pattern, Collections.singleton(PropertyType.BINARY), -1, -1);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
