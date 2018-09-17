/*
 * Copyright 2014 - 2018 Blazebit.
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

package com.blazebit.persistence.view.impl.update.flush;

import com.blazebit.persistence.parser.EntityMetamodel;
import com.blazebit.persistence.spi.ExtendedAttribute;
import com.blazebit.persistence.spi.ExtendedManagedType;
import com.blazebit.persistence.view.impl.EntityViewManagerImpl;
import com.blazebit.persistence.view.metamodel.SingularAttribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public abstract class AbstractUnmappedAttributeCascadeDeleter implements UnmappedAttributeCascadeDeleter {

    protected static final UnmappedAttributeCascadeDeleter[] EMPTY = new UnmappedAttributeCascadeDeleter[0];
    protected final Class<?> elementEntityClass;
    protected final Set<String> elementIdAttributeNames;
    protected final String attributeName;
    protected final Set<String> attributeValuePaths;
    protected final String attributeValuePath;
    protected final boolean cascadeDeleteElement;

    public AbstractUnmappedAttributeCascadeDeleter(EntityViewManagerImpl evm, String attributeName, ExtendedAttribute<?, ?> attribute) {
        EntityMetamodel entityMetamodel = evm.getMetamodel().getEntityMetamodel();
        this.elementEntityClass = attribute.getElementClass();
        this.attributeName = attributeName;
        if (entityMetamodel.getEntity(elementEntityClass) == null) {
            this.elementIdAttributeNames = null;
            this.attributeValuePaths = Collections.singleton(attributeName);
            this.attributeValuePath = attributeName;
            this.cascadeDeleteElement = false;
        } else {
            ExtendedManagedType extendedManagedType = entityMetamodel.getManagedType(ExtendedManagedType.class, elementEntityClass);
            Set<String> elementIdAttributeNames1 = new HashSet<>();
            Set<String> attributeValuePaths1 = new HashSet<>();
            for(SingularAttribute idAttribute : (Set<SingularAttribute>) extendedManagedType.getIdAttributes()){
                elementIdAttributeNames1.add((idAttribute.getName()));
                attributeValuePaths1.add((attributeName + "." + (SingularAttribute<?,?>)idAttribute).getName());
            }
            this.elementIdAttributeNames = elementIdAttributeNames1;
            this.attributeValuePaths = attributeValuePaths1;
            this.attributeValuePath = attributeName + "." + extendedManagedType.getIdAttribute().getName();
            this.cascadeDeleteElement = attribute.isDeleteCascaded();
        }
    }

    protected AbstractUnmappedAttributeCascadeDeleter(AbstractUnmappedAttributeCascadeDeleter original) {
        this.elementEntityClass = original.elementEntityClass;
        this.elementIdAttributeNames = original.elementIdAttributeNames;
        this.attributeName = original.attributeName;
        this.attributeValuePaths = original.attributeValuePaths;
        this.cascadeDeleteElement = original.cascadeDeleteElement;
    }

    @Override
    public String getAttributeValuePath() {
        return attributeValuePath;
    }

    public Set<String> getAttributeValuePaths() {
        return attributeValuePaths;
    }
}
