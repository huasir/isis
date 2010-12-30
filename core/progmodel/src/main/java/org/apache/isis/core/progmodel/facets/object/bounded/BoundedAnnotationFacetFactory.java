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


package org.apache.isis.core.progmodel.facets.object.bounded;

import org.apache.isis.applib.annotation.Bounded;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facetapi.MethodRemover;
import org.apache.isis.core.metamodel.facets.AnnotationBasedFacetFactoryAbstract;
import org.apache.isis.core.metamodel.facets.object.bounded.BoundedFacet;


public class BoundedAnnotationFacetFactory extends AnnotationBasedFacetFactoryAbstract {

    public BoundedAnnotationFacetFactory() {
        super(FeatureType.OBJECTS_ONLY);
    }

    @Override
    public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder holder) {
        final Bounded annotation = (Bounded) getAnnotation(cls, Bounded.class);
        return FacetUtil.addFacet(create(annotation, holder));
    }

    private BoundedFacet create(final Bounded annotation, final FacetHolder holder) {
        return annotation == null ? null : new BoundedFacetAnnotation(holder);
    }

}
