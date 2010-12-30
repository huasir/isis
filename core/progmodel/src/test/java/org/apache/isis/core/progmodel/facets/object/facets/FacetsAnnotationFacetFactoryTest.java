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

package org.apache.isis.core.progmodel.facets.object.facets;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.isis.applib.annotation.Facets;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetFactory;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facetapi.MethodRemover;
import org.apache.isis.core.metamodel.facets.facets.FacetsFacet;
import org.apache.isis.core.progmodel.facets.AbstractFacetFactoryTest;

public class FacetsAnnotationFacetFactoryTest extends AbstractFacetFactoryTest {

    private FacetsAnnotationFacetFactory facetFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        facetFactory = new FacetsAnnotationFacetFactory();
    }

    @Override
    protected void tearDown() throws Exception {
        facetFactory = null;
        super.tearDown();
    }

    @Override
    public void testFeatureTypes() {
        final List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertTrue(contains(featureTypes, FeatureType.OBJECT));
        assertFalse(contains(featureTypes, FeatureType.PROPERTY));
        assertFalse(contains(featureTypes, FeatureType.COLLECTION));
        assertFalse(contains(featureTypes, FeatureType.ACTION));
        assertFalse(contains(featureTypes, FeatureType.ACTION_PARAMETER));
    }

    public static class CustomerFacetFactory implements FacetFactory {
        @Override
        public List<FeatureType> getFeatureTypes() {
            return null;
        }

        @Override
        public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder holder) {
            return false;
        }

        @Override
        public boolean process(Class<?> cls, final Method method, final MethodRemover methodRemover,
            final FacetHolder holder) {
            return false;
        }

        @Override
        public boolean processParams(final Method method, final int paramNum, final FacetHolder holder) {
            return false;
        }
    }

    public static class CustomerFacetFactory2 implements FacetFactory {
        @Override
        public List<FeatureType> getFeatureTypes() {
            return null;
        }

        @Override
        public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder holder) {
            return false;
        }

        @Override
        public boolean process(Class<?> cls, final Method method, final MethodRemover methodRemover,
            final FacetHolder holder) {
            return false;
        }

        @Override
        public boolean processParams(final Method method, final int paramNum, final FacetHolder holder) {
            return false;
        }
    }

    public void testFacetsFactoryNames() {
        @Facets(facetFactoryNames = {
            "org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest$CustomerFacetFactory",
            "org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest$CustomerNotAFacetFactory" })
        class Customer {
        }

        facetFactory.process(Customer.class, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(FacetsFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof FacetsFacetAnnotation);
        final FacetsFacetAnnotation facetsFacet = (FacetsFacetAnnotation) facet;
        final Class<? extends FacetFactory>[] facetFactories = facetsFacet.facetFactories();
        assertEquals(1, facetFactories.length);
        assertEquals(CustomerFacetFactory.class, facetFactories[0]);

        assertNoMethodsRemoved();
    }

    public void testFacetsFactoryClass() {
        @Facets(facetFactoryClasses = {
            org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest.CustomerFacetFactory.class,
            org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest.CustomerNotAFacetFactory.class })
        class Customer {
        }

        facetFactory.process(Customer.class, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(FacetsFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof FacetsFacetAnnotation);
        final FacetsFacetAnnotation facetsFacet = (FacetsFacetAnnotation) facet;
        final Class<? extends FacetFactory>[] facetFactories = facetsFacet.facetFactories();
        assertEquals(1, facetFactories.length);
        assertEquals(CustomerFacetFactory.class, facetFactories[0]);
    }

    public static class CustomerNotAFacetFactory {
    }

    public void testFacetsFactoryNameAndClass() {
        @Facets(facetFactoryNames = { "org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest$CustomerFacetFactory" }, facetFactoryClasses = { org.apache.isis.core.progmodel.facets.object.facets.FacetsAnnotationFacetFactoryTest.CustomerFacetFactory2.class })
        class Customer {
        }

        facetFactory.process(Customer.class, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(FacetsFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof FacetsFacetAnnotation);
        final FacetsFacetAnnotation facetsFacet = (FacetsFacetAnnotation) facet;
        final Class<? extends FacetFactory>[] facetFactories = facetsFacet.facetFactories();
        assertEquals(2, facetFactories.length);
        assertEquals(CustomerFacetFactory.class, facetFactories[0]);
        assertEquals(CustomerFacetFactory2.class, facetFactories[1]);
    }

    public void testFacetsFactoryNoop() {
        @Facets
        class Customer {
        }

        facetFactory.process(Customer.class, methodRemover, facetHolder);

        final Facet facet = facetHolder.getFacet(FacetsFacet.class);
        assertNull(facet);
    }

}
