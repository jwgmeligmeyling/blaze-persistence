/*
 * Copyright 2014 - 2020 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.impl;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.Member;

/**
 * Represents a property attribute of a Hibernate {@code CompositeUserType}.
 * @param <X> Declaring type
 * @param <Y> Property type
 * @author Jan-Willem Gmelig Meyling
 * @since 1.5.0
 */
class FakeAttribute<X, Y> implements Attribute<X, Y> {
    private final String attributeName;
    private final EntityType<X> declaringType;
    private final Class<Y> compositeAttributeType;

    public FakeAttribute(String attributeName, EntityType<X> declaringType, Class<Y> compositeAttributeType) {
        this.attributeName = attributeName;
        this.declaringType = declaringType;
        this.compositeAttributeType = compositeAttributeType;
    }

    @Override
    public String getName() {
        return attributeName;
    }

    @Override
    public PersistentAttributeType getPersistentAttributeType() {
        return PersistentAttributeType.BASIC;
    }

    @Override
    public ManagedType<X> getDeclaringType() {
        return declaringType;
    }

    @Override
    public Class<Y> getJavaType() {
        return compositeAttributeType;
    }

    @Override
    public Member getJavaMember() {
        return null;
    }

    @Override
    public boolean isAssociation() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }
}
