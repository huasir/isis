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

package org.apache.isis.runtimes.dflt.runtime.testsystem;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.isis.core.commons.config.ConfigurationConstants;
import org.apache.isis.core.commons.ensure.Assert;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.ObjectAdapterFactory;
import org.apache.isis.core.metamodel.adapter.ResolveState;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.facets.typeof.TypeOfFacetDefaultToObject;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.specloader.ObjectReflector;
import org.apache.isis.core.metamodel.testspec.TestProxySpecification;
import org.apache.isis.core.runtime.authentication.AuthenticationManager;
import org.apache.isis.core.runtime.authorization.AuthorizationManager;
import org.apache.isis.core.runtime.imageloader.TemplateImageLoader;
import org.apache.isis.core.runtime.imageloader.noop.TemplateImageLoaderNoop;
import org.apache.isis.core.runtime.userprofile.UserProfileLoader;
import org.apache.isis.core.runtime.userprofile.UserProfileStore;
import org.apache.isis.runtimes.dflt.runtime.persistence.adaptermanager.AdapterManagerPersist;
import org.apache.isis.runtimes.dflt.runtime.persistence.adaptermanager.AdapterManagerTestSupport;
import org.apache.isis.runtimes.dflt.runtime.persistence.internal.RuntimeContextFromSession;
import org.apache.isis.runtimes.dflt.runtime.system.DeploymentType;
import org.apache.isis.runtimes.dflt.runtime.system.context.IsisContext;
import org.apache.isis.runtimes.dflt.runtime.system.context.IsisContextStatic;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.AdapterManager;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.OidGenerator;
import org.apache.isis.runtimes.dflt.runtime.system.persistence.PersistenceSession;
import org.apache.isis.runtimes.dflt.runtime.system.session.IsisSessionDefault;
import org.apache.isis.runtimes.dflt.runtime.system.session.IsisSessionFactory;
import org.apache.isis.runtimes.dflt.runtime.system.session.IsisSessionFactoryDefault;
import org.apache.isis.runtimes.dflt.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.runtimes.dflt.runtime.system.transaction.UpdateNotifier;
import org.apache.isis.runtimes.dflt.runtime.transaction.updatenotifier.UpdateNotifierDefault;
import org.apache.isis.runtimes.dflt.runtime.userprofile.UserProfileLoaderDefault;
import org.apache.isis.runtimes.dflt.runtime.userprofile.UserProfileLoaderDefault.Mode;

//TODO replace with TestProxySystemII
public class TestProxySystem {

    private int nextId = 1;
    private final TestProxyConfiguration configuration;
    private IsisContext context;

    private final UserProfileLoader userProfileLoader;
    private final UserProfileStore userprofileStore;
    private final TestProxyPersistenceSessionFactory persistenceSessionFactory;

    private PersistenceSession persistenceSession;
    private final TestProxyReflector reflector;
    private final UpdateNotifierDefault updateNotifier;
    private final TemplateImageLoader noopTemplateImageLoader;
    protected AuthenticationManager authenticationManager;
    protected AuthorizationManager authorizationManager;
    private final List<Object> servicesList;

    public TestProxySystem() {
        noopTemplateImageLoader = new TemplateImageLoaderNoop();
        reflector = new TestProxyReflector();

        servicesList = Collections.emptyList();

        // all a bit hacky...
        persistenceSessionFactory = new TestProxyPersistenceSessionFactory();
        userprofileStore = new TestUserProfileStore();
        userProfileLoader = new UserProfileLoaderDefault(userprofileStore, Mode.RELAXED);
        persistenceSession = new TestProxyPersistenceSession(persistenceSessionFactory);
        persistenceSessionFactory.setPersistenceSessionToCreate(persistenceSession);

        configuration = new TestProxyConfiguration();
        configuration.add(ConfigurationConstants.ROOT + "locale", "en_GB");
        authenticationManager = new AuthenticationManagerNoop();
        authorizationManager = new AuthorizationManagerNoop();
        updateNotifier = new UpdateNotifierDefault();
    }

    public TestProxyAdapter createAdapterForTransient(final Object associate) {
        final TestProxyAdapter testProxyObjectAdapter = new TestProxyAdapter();
        testProxyObjectAdapter.setupObject(associate);
        testProxyObjectAdapter.setupSpecification(getSpecification(associate.getClass()));
        testProxyObjectAdapter.setupResolveState(ResolveState.TRANSIENT);
        testProxyObjectAdapter.setupOid(new TestProxyOid(nextId++));
        return testProxyObjectAdapter;
    }

    public void init() {
        reflector.setRuntimeContext(new RuntimeContextFromSession());

        final IsisSessionFactory sessionFactory = new IsisSessionFactoryDefault(DeploymentType.EXPLORATION, configuration, noopTemplateImageLoader, reflector, authenticationManager, authorizationManager, userProfileLoader, persistenceSessionFactory, servicesList);

        persistenceSession.setSpecificationLoader(reflector);
        // this implementation of persistenceSession will automatically inject
        // its own transaction manager into itself.

        sessionFactory.init();
        context = IsisContextStatic.createRelaxedInstance(sessionFactory);

        // commented out cos think now redundant since calling
        // openExecutionContext below
        // persistor.open();

        IsisContext.openSession(new TestProxySession());

    }

    public void shutdown() {
        IsisContext.closeAllSessions();
    }

    public void resetLoader() {
        persistenceSession.testReset();
    }

    public ObjectAdapter createPersistentTestObject() {
        final TestPojo pojo = new TestPojo();
        return createPersistentTestObject(pojo);
    }

    public ObjectAdapter createPersistentTestObject(final Object domainObject) {
        final ObjectAdapter adapter = createTransientTestObject(domainObject);

        // similar to object store implementation
        getAdapterManagerPersist().remapAsPersistent(adapter);

        // would be done by the object store, we must do ourselves.
        adapter.setOptimisticLock(new TestProxyVersion(1));

        return adapter;
    }

    public void makePersistent(final TestProxyAdapter adapter) {
        final Oid oid = adapter.getOid();
        getOidGenerator().convertTransientToPersistentOid(oid);
        adapter.setupOid(oid);
        persistenceSession.makePersistent(adapter);
    }

    // commented out since never used locally
    // private void setUpSpecification(final TestPojo pojo, final
    // TestProxyAdapter adapter) {
    // adapter.setupSpecification(reflector.loadSpecification(pojo.getClass()));
    // }

    // commented out since never used locally
    // private void addAdapterToIdentityMap(final Object domainObject, final
    // ObjectAdapter adapter) {
    // ((PersistenceSessionSpy) persistor).addAdapter(domainObject, adapter);
    // }

    public ObjectAdapter createTransientTestObject() {
        final TestPojo pojo = new TestPojo();
        return createTransientTestObject(pojo);
    }

    public ObjectAdapter createTransientTestObject(final Object domainObject) {
        final TestProxyOid oid = new TestProxyOid(nextId++, false);
        final ObjectAdapter adapterFor = getAdapterManagerTestSupport().testCreateTransient(domainObject, oid);
        Assert.assertEquals("", ResolveState.TRANSIENT, adapterFor.getResolveState());
        return adapterFor;
    }

    public TestProxySpecification getSpecification(final Class<?> type) {
        return (TestProxySpecification) reflector.loadSpecification(type);
    }

    public void setPersistenceSession(final PersistenceSession persistor) {
        this.persistenceSession = persistor;
        if (context != null) {
            final IsisSessionDefault current = (IsisSessionDefault) context.getSessionInstance();
            current.testSetObjectPersistor(persistor);
        }
    }

    /**
     * 
     */
    public TestProxyCollectionAdapter createPersistentTestCollection() {
        final TestProxyCollectionAdapter collection = new TestProxyCollectionAdapter(new Vector());
        final TestProxySpecification specification = getSpecification(Vector.class);
        final TestProxySpecification elementSpecification = getSpecification(Object.class);
        specification.addFacet(new TestProxyCollectionFacet());
        specification.addFacet(new TypeOfFacetDefaultToObject(elementSpecification, reflector) {
        });
        collection.setupSpecification(specification);
        return collection;
    }

    public TestProxySpecification getSpecification(final ObjectAdapter object) {
        return (TestProxySpecification) object.getSpecification();
    }

    public void addSpecification(final ObjectSpecification specification) {
        reflector.addSpecification(specification);
    }

    public void addConfiguration(final String name, final String value) {
        configuration.add(name, value);
    }

    public UpdateNotifier getUpdateNotifer() {
        return updateNotifier;
    }

    public ObjectReflector getReflector() {
        return reflector;
    }

    public PersistenceSession getPersistenceSession() {
        return persistenceSession;
    }

    public TestProxyConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Created automatically by the persistor.
     */
    public ObjectAdapterFactory getAdapterFactory() {
        return persistenceSession.getAdapterFactory();
    }

    /**
     * Created automatically by the persistor.
     */
    public AdapterManager getAdapterManager() {
        return persistenceSession.getAdapterManager();
    }

    public AdapterManagerTestSupport getAdapterManagerTestSupport() {
        return (AdapterManagerTestSupport) persistenceSession.getAdapterManager();
    }

    public AdapterManagerPersist getAdapterManagerPersist() {
        return (AdapterManagerPersist) persistenceSession.getAdapterManager();
    }

    private IsisTransactionManager getTransactionManager() {
        return persistenceSession.getTransactionManager();
    }

    private OidGenerator getOidGenerator() {
        return persistenceSession.getOidGenerator();
    }

}
