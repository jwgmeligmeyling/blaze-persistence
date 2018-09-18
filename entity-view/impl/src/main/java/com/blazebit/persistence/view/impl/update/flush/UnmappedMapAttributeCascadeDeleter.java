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

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.DeleteCriteriaBuilder;
import com.blazebit.persistence.spi.ExtendedAttribute;
import com.blazebit.persistence.view.impl.EntityViewManagerImpl;
import com.blazebit.persistence.view.impl.update.UpdateContext;

import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.util.*;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class UnmappedMapAttributeCascadeDeleter extends AbstractUnmappedAttributeCascadeDeleter {

    private final Class<?> ownerEntityClass;
    private final Set<String> ownerIdAttributeNames;
    private final boolean jpaProviderDeletesCollection;
    private final UnmappedBasicAttributeCascadeDeleter elementDeleter;

    public UnmappedMapAttributeCascadeDeleter(EntityViewManagerImpl evm, String attributeName, ExtendedAttribute<?, ?> attribute, Class<?> ownerEntityClass, Set<String> ownerIdAttributeNames, boolean disallowCycle) {
        super(evm, attributeName, attribute);
        this.ownerEntityClass = ownerEntityClass;
        this.ownerIdAttributeNames = ownerIdAttributeNames;
        if (elementIdAttributeNames != null && !elementIdAttributeNames.isEmpty()) {
            this.jpaProviderDeletesCollection = evm.getJpaProvider().supportsJoinTableCleanupOnDelete();
            if (cascadeDeleteElement) {
                this.elementDeleter = new UnmappedBasicAttributeCascadeDeleter(
                        evm,
                        "",
                        attribute,
                        null,
                        disallowCycle
                );
            } else {
                this.elementDeleter = null;
            }
        } else {
            this.jpaProviderDeletesCollection = evm.getJpaProvider().supportsCollectionTableCleanupOnDelete();
            this.elementDeleter = null;
        }
    }

    private UnmappedMapAttributeCascadeDeleter(UnmappedMapAttributeCascadeDeleter original, boolean jpaProviderDeletesCollection) {
        super(original);
        this.ownerEntityClass = original.ownerEntityClass;
        this.ownerIdAttributeNames = original.ownerIdAttributeNames;
        this.jpaProviderDeletesCollection = jpaProviderDeletesCollection;
        this.elementDeleter = original.elementDeleter;
    }

    @Override
    public boolean requiresDeleteCascadeAfterRemove() {
        return false;
    }

    @Override
    public void removeById(UpdateContext context, Object id) {
        throw new UnsupportedOperationException("Can't delete collection attribute by id!");
    }

    @Override
    public void removeByOwnerId(UpdateContext context, Object ownerId) {
        Set<String> ownerIds = new HashSet<>();
        for (Field field : Arrays.asList(ownerId.getClass().getFields())){
            ownerIds.add(field.getName());
        }
        EntityViewManagerImpl evm = context.getEntityViewManager();
        if (cascadeDeleteElement) {
            List<Object> elementIds;
            if (evm.getDbmsDialect().supportsReturningColumns()) {
                DeleteCriteriaBuilder cb1 = evm.getCriteriaBuilderFactory().deleteCollection(context.getEntityManager(), ownerEntityClass, "e", attributeName);
                for(String ownerIdAttributeName : ownerIdAttributeNames) {
                    //TODO: add exception for when the set ownerIdAttributeNames and ownerIds do not match in size!
                    cb1.where(ownerIdAttributeName).in(ownerIds);
                }

                List<String> returningAttributes = new ArrayList<>();
                for (String elementIdAttributeName : elementIdAttributeNames){
                    returningAttributes.add(attributeName + "." + elementIdAttributeName);
                }
                List<Tuple> tuples = cb1.executeWithReturning((String[]) returningAttributes.toArray()).getResultList();
                elementIds = new ArrayList<>(tuples.size());
                for (Tuple tuple : tuples) {
                    elementIds.add(tuple.get(0));
                }
            } else {
                CriteriaBuilder cb = evm.getCriteriaBuilderFactory().create(context.getEntityManager(), ownerEntityClass, "e");
                for(String ownerIdAttributeName : ownerIdAttributeNames) {
                    cb.where(ownerIdAttributeName).in(ownerIds);
                }
                for (String elementIdAttributeName : elementIdAttributeNames){
                    cb.select("e." + attributeName + "." +elementIdAttributeName);
                }
                elementIds = (List<Object>) cb.getResultList();
                if (!elementIds.isEmpty()) {
                    // We must always delete this, otherwise we might get a constraint violation because of the cascading delete
                    DeleteCriteriaBuilder<?> cb2 = evm.getCriteriaBuilderFactory().deleteCollection(context.getEntityManager(), ownerEntityClass, "e", attributeName);
                    for(String ownerIdAttributeName : ownerIdAttributeNames){
                        //TODO: add exception for when the set ownerIdAttributeNames and ownerIds do not match in size!
                        cb2.where(ownerIdAttributeName).in(ownerIds);
                    }
                    cb2.executeUpdate();
                }
            }
            for (Object elementId : elementIds) {
                elementDeleter.removeById(context, elementId);
            }
        } else if (!jpaProviderDeletesCollection) {
            DeleteCriteriaBuilder<?> cb = evm.getCriteriaBuilderFactory().deleteCollection(context.getEntityManager(), ownerEntityClass, "e", attributeName);
            for(String ownerIdAttributeName : ownerIdAttributeNames){
                //TODO: add exception for when the set ownerIdAttributeNames and ownerIds do not match in size!
                cb.where(ownerIdAttributeName).in(ownerIds);
            }
            cb.executeUpdate();
        }
    }

    @Override
    public UnmappedAttributeCascadeDeleter createFlusherWiseDeleter() {
        return jpaProviderDeletesCollection ? new UnmappedMapAttributeCascadeDeleter(this, false) : this;
    }
}
