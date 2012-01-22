/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.runtimes.dflt.runtime.context;

import java.util.Collections;
import java.util.List;

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.commons.config.IsisConfigurationDefault;
import org.apache.isis.core.metamodel.specloader.ObjectReflector;
import org.apache.isis.core.runtime.authentication.AuthenticationManager;
import org.apache.isis.core.runtime.authorization.AuthorizationManager;
import org.apache.isis.core.runtime.imageloader.TemplateImageLoader;
import org.apache.isis.core.runtime.userprofile.UserProfile;
import org.apache.isis.core.runtime.userprofile.UserProfileLoader;
import org.apache.isis.runtimes.dflt.runtime.persistence.internal.RuntimeContextFromSession;
import org.apache.isis.runtimes.dflt.runtime.system.DeploymentType;
import org.apache.isis.runtimes.dflt.runtime.system.context.IsisContext;
import org.apache.isis.runtimes.dflt.runtime.system.context.IsisContextStatic;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.PersistenceSession;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.PersistenceSessionFactory;
import org.apache.isis.runtimes.dflt.runtime.system.session.IsisSessionFactory;
import org.apache.isis.runtimes.dflt.runtime.system.session.IsisSessionFactoryDefault;
import org.apache.isis.runtimes.dflt.runtime.testsystem.TestProxyPersistenceSession;
import org.apache.isis.runtimes.dflt.runtime.testsystem.TestProxyReflector;
import org.apache.isis.runtimes.dflt.runtime.testsystem.TestProxySession;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class IsisContextTest {

    private final Mockery mockery = new JUnit4Mockery();

    private IsisConfiguration configuration;
    private PersistenceSession persistenceSession;
    private ObjectReflector reflector;
    private TestProxySession session;

    protected TemplateImageLoader mockTemplateImageLoader;
    protected PersistenceSessionFactory mockPersistenceSessionFactory;
    private UserProfileLoader mockUserProfileLoader;
    protected AuthenticationManager mockAuthenticationManager;
    protected AuthorizationManager mockAuthorizationManager;

    private List<Object> servicesList;

    @Before
    public void setUp() throws Exception {
        IsisContext.testReset();

        servicesList = Collections.emptyList();

        mockTemplateImageLoader = mockery.mock(TemplateImageLoader.class);
        mockPersistenceSessionFactory = mockery.mock(PersistenceSessionFactory.class);
        mockUserProfileLoader = mockery.mock(UserProfileLoader.class);
        mockAuthenticationManager = mockery.mock(AuthenticationManager.class);
        mockAuthorizationManager = mockery.mock(AuthorizationManager.class);

        configuration = new IsisConfigurationDefault();
        reflector = new TestProxyReflector();
        persistenceSession = new TestProxyPersistenceSession(mockPersistenceSessionFactory);

        mockery.checking(new Expectations() {
            {
                one(mockPersistenceSessionFactory).createPersistenceSession();
                will(returnValue(persistenceSession));

                ignoring(mockPersistenceSessionFactory);

                one(mockUserProfileLoader).getProfile(with(any(AuthenticationSession.class)));
                will(returnValue(new UserProfile()));

                ignoring(mockUserProfileLoader);

                ignoring(mockAuthenticationManager);

                ignoring(mockAuthorizationManager);

                ignoring(mockTemplateImageLoader);
            }
        });

        reflector.setRuntimeContext(new RuntimeContextFromSession());

        IsisContext.setConfiguration(configuration);

        final IsisSessionFactory sessionFactory = new IsisSessionFactoryDefault(DeploymentType.EXPLORATION, configuration, mockTemplateImageLoader, reflector, mockAuthenticationManager, mockAuthorizationManager, mockUserProfileLoader, mockPersistenceSessionFactory, servicesList);
        IsisContextStatic.createRelaxedInstance(sessionFactory);
        sessionFactory.init();

        session = new TestProxySession();
        IsisContext.openSession(session);
    }

    @Test
    public void testConfiguration() {
        Assert.assertEquals(configuration, IsisContext.getConfiguration());
    }

    @Test
    public void testObjectPersistor() {
        Assert.assertEquals(persistenceSession, IsisContext.getPersistenceSession());
    }

    @Test
    public void testSpecificationLoader() {
        Assert.assertEquals(reflector, IsisContext.getSpecificationLoader());
    }

    @Test
    public void testSession() {
        Assert.assertEquals(session, IsisContext.getAuthenticationSession());
    }
}
