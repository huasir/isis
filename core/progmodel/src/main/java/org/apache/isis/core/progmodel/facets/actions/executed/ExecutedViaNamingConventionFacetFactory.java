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


package org.apache.isis.core.progmodel.facets.actions.executed;

import java.lang.reflect.Method;

import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facetapi.MethodRemover;
import org.apache.isis.core.metamodel.facets.actions.executed.ExecutedFacet;
import org.apache.isis.core.metamodel.facets.naming.named.NamedFacetInferred;
import org.apache.isis.core.metamodel.spec.FacetFactoryAbstract;
import org.apache.isis.core.progmodel.facets.actions.ActionMethodsFacetFactory;
import org.apache.isis.core.progmodel.facets.actions.ExecutedFacetViaNamingConvention;


/**
 * Creates an {@link ExecutedFacet} based on the prefix of the action's name.
 * 
 * <p>
 * TODO: think that this prefix is handled by the {@link ActionMethodsFacetFactory}.
 */
public class ExecutedViaNamingConventionFacetFactory extends FacetFactoryAbstract {

    private static final String LOCAL_PREFIX = "Local";

    public ExecutedViaNamingConventionFacetFactory() {
        super(FeatureType.ACTIONS_ONLY);
    }

    @Override
    public boolean process(Class<?> cls, final Method method, final MethodRemover methodRemover, final FacetHolder holder) {
        final String fullMethodName = method.getName();
        final String capitalizedName = fullMethodName.substring(0, 1).toUpperCase() + fullMethodName.substring(1);

        if (!capitalizedName.startsWith(LOCAL_PREFIX)) {
            return false;
        }

        return FacetUtil.addFacets(new Facet[] { new ExecutedFacetViaNamingConvention(ExecutedFacet.Where.LOCALLY, holder),
                new NamedFacetInferred(capitalizedName.substring(5), holder) });
    }

    public boolean recognizes(final Method method) {
        return method.getName().startsWith(LOCAL_PREFIX);
    }

}
