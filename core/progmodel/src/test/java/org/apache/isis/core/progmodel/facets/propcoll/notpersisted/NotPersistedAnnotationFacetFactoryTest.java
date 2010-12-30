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


package org.apache.isis.core.progmodel.facets.propcoll.notpersisted;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.apache.isis.applib.annotation.NotPersisted;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facets.propcoll.notpersisted.NotPersistedFacet;
import org.apache.isis.core.progmodel.facets.AbstractFacetFactoryTest;


public class NotPersistedAnnotationFacetFactoryTest extends AbstractFacetFactoryTest {

    private NotPersistedAnnotationFacetFactory facetFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        facetFactory = new NotPersistedAnnotationFacetFactory();
    }

    @Override
    protected void tearDown() throws Exception {
        facetFactory = null;
        super.tearDown();
    }

    @Override
    public void testFeatureTypes() {
        final List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertFalse(contains(featureTypes, FeatureType.OBJECT));
        assertTrue(contains(featureTypes, FeatureType.PROPERTY));
        assertTrue(contains(featureTypes, FeatureType.COLLECTION));
        assertFalse(contains(featureTypes, FeatureType.ACTION));
        assertFalse(contains(featureTypes, FeatureType.ACTION_PARAMETER));
    }

    public void testNotPersistedAnnotationPickedUpOnProperty() {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
        class Customer {
            @SuppressWarnings("unused")
            @NotPersisted()
            public String getFirstName() {
                return null;
            }
        }
        final Method method = findMethod(Customer.class, "getFirstName");

        facetFactory.process(Customer.class, method, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(NotPersistedFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof NotPersistedFacetAnnotation);

        assertNoMethodsRemoved();
    }

    public void testNotPersistedAnnotationPickedUpOnCollection() {
        class Order{}
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
        class Customer {
            @SuppressWarnings("unused")
            @NotPersisted()
            public Collection<Order> getOrders() {
                return null;
            }
        }
        final Method method = findMethod(Customer.class, "getOrders");

        facetFactory.process(Customer.class, method, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(NotPersistedFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof NotPersistedFacetAnnotation);

        assertNoMethodsRemoved();
    }

}

