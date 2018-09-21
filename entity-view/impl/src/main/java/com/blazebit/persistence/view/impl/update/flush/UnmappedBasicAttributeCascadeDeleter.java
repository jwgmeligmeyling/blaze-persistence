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
import com.blazebit.persistence.ReturningResult;
import com.blazebit.persistence.parser.EntityMetamodel;
import com.blazebit.persistence.parser.util.JpaMetamodelUtils;
import com.blazebit.persistence.spi.ExtendedAttribute;
import com.blazebit.persistence.spi.ExtendedManagedType;
import com.blazebit.persistence.view.impl.EntityViewManagerImpl;
import com.blazebit.persistence.view.impl.update.UpdateContext;
import com.blazebit.reflection.ReflectionUtils;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.util.*;


/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class UnmappedBasicAttributeCascadeDeleter extends AbstractUnmappedAttributeCascadeDeleter {

    private final Set<String> ownerIdAttributeNames;
    private final String deleteQuery;
    private final String deleteByOwnerIdQuery;
    private final boolean requiresDeleteCascadeAfterRemove;
    private final boolean requiresDeleteAsEntity;
    private final UnmappedAttributeCascadeDeleter[] unmappedPreRemoveCascadeDeleters;
    private final UnmappedAttributeCascadeDeleter[] unmappedPostRemoveCascadeDeleters;

    //NOTE: if I understand this correctly then here we put in a single attribute to delete. This element can be a key (then the deleteQuery is used), or not (then the ownerQuery is used).
    //TODO create support for the single attribute to be removed to also be a composite key; as of now we use getIdAttribute, assuming that indeed it has only one idAttribute.
    public UnmappedBasicAttributeCascadeDeleter(EntityViewManagerImpl evm, String attributeName, ExtendedAttribute<?, ?> attribute, Set<String> ownerIdAttributeNames, boolean disallowCycle) {
        super(evm, attributeName, attribute);
        EntityMetamodel entityMetamodel = evm.getMetamodel().getEntityMetamodel();
        ExtendedManagedType extendedManagedType = entityMetamodel.getManagedType(ExtendedManagedType.class, elementEntityClass);
        EntityType<?> entityType = (EntityType<?>) extendedManagedType.getType();
        this.requiresDeleteCascadeAfterRemove = !attribute.isForeignJoinColumn();
        this.ownerIdAttributeNames = ownerIdAttributeNames;
        StringBuilder deleteByIdQuery = new StringBuilder("DELETE FROM " + entityType.getName() );
        for(String elementIdAttributeName : elementIdAttributeNames){
            deleteByIdQuery.append(" e WHERE e." + elementIdAttributeName +" = :" + elementIdAttributeName.replace(".","_") + "_variable" + " AND");
        }
        deleteByIdQuery.setLength(deleteByIdQuery.length() - " AND".length());
        deleteQuery = deleteByIdQuery.toString();

        StringBuilder deleteByOwnerIdQueryBuilder = new StringBuilder("DELETE FROM " + entityType.getName());
        int i = 1;
        for(String ownerIdAttributeName : ownerIdAttributeNames){
            deleteByOwnerIdQueryBuilder.append(" e WHERE e." + ownerIdAttributeName + " = :" + ownerIdAttributeName.replace(".","_") + "_variable" + " AND".replaceAll(".","_"));
        }
        deleteByOwnerIdQueryBuilder.setLength(deleteByOwnerIdQueryBuilder.length() - " AND".length());
        deleteByOwnerIdQuery = deleteByOwnerIdQueryBuilder.toString();

        if (elementIdAttributeNames == null || elementIdAttributeNames.isEmpty()) {
            this.requiresDeleteAsEntity = false;
            this.unmappedPreRemoveCascadeDeleters = this.unmappedPostRemoveCascadeDeleters = EMPTY;
        } else {
            // If the attribute introduces a cycle, we can't construct pre- and post-deleters. We must do entity deletion, otherwise we'd get a stack overflow
            if (disallowCycle && attribute.hasCascadingDeleteCycle()) {
                this.requiresDeleteAsEntity = true;
                this.unmappedPreRemoveCascadeDeleters = this.unmappedPostRemoveCascadeDeleters = EMPTY;
            } else {
                List<UnmappedAttributeCascadeDeleter> unmappedCascadeDeleters = UnmappedAttributeCascadeDeleterUtil.createUnmappedCascadeDeleters(evm, elementEntityClass, elementIdAttributeNames);
                List<UnmappedAttributeCascadeDeleter> unmappedPreRemoveCascadeDeleters = new ArrayList<>(unmappedCascadeDeleters.size());
                List<UnmappedAttributeCascadeDeleter> unmappedPostRemoveCascadeDeleters = new ArrayList<>(unmappedCascadeDeleters.size());
                for (UnmappedAttributeCascadeDeleter deleter : unmappedCascadeDeleters) {
                    if (deleter.requiresDeleteCascadeAfterRemove()) {
                        unmappedPostRemoveCascadeDeleters.add(deleter);
                    } else {
                        unmappedPreRemoveCascadeDeleters.add(deleter);
                    }
                }

                this.requiresDeleteAsEntity = false;
                this.unmappedPreRemoveCascadeDeleters = unmappedPreRemoveCascadeDeleters.toArray(new UnmappedAttributeCascadeDeleter[unmappedPreRemoveCascadeDeleters.size()]);
                this.unmappedPostRemoveCascadeDeleters = unmappedPostRemoveCascadeDeleters.toArray(new UnmappedAttributeCascadeDeleter[unmappedPostRemoveCascadeDeleters.size()]);
            }
        }
    }

    @Override
    public boolean requiresDeleteCascadeAfterRemove() {
        return requiresDeleteCascadeAfterRemove;
    }

    @Override
    public void removeById(UpdateContext context, Object id) {
        for (int i = 0; i < unmappedPreRemoveCascadeDeleters.length; i++) {
            unmappedPreRemoveCascadeDeleters[i].removeByOwnerId(context, id);
        }
        removeWithoutPreCascadeDelete(context, null, null, id);
    }

    @Override
    public void removeByOwnerId(UpdateContext context, Object ownerId) {
        Object[] returnedValues = null;
        Object id = null;
        //TODO: figure out whether I should use ownerIdAttributeNames or elementIdAttributeNames below, and whether I should use the elementEntityClass or attempt to find the ownerEntityClass.
        Map<String, Object> ownerIds = JpaMetamodelUtils.getIdNameValueMap(elementEntityClass,ownerId,context.getEntityManager().getMetamodel(),elementIdAttributeNames);
        if (requiresDeleteAsEntity) {
            CriteriaBuilder<?> cb = context.getEntityViewManager().getCriteriaBuilderFactory().create(context.getEntityManager(), elementEntityClass);
            //TODO: add exception for when the set ownerIdAttributeNames and ownerIds.keySet() do not match in size!
            for(String ownerIdAttributeName : ownerIdAttributeNames){
                cb.where(ownerIdAttributeName).eq(ownerIds.get(ownerIdAttributeName));
            }
            context.getEntityManager().remove(cb.getSingleResult());
            // We need to flush here, otherwise the deletion will be deferred and might cause a constraint violation
            context.getEntityManager().flush();
        } else {
            if (unmappedPreRemoveCascadeDeleters.length != 0) {
                // If we have pre remove cascade deleters, we need to query the id first so we can remove these elements
                List<String> returningAttributes = new ArrayList<>();
                for (int i = 0; i < unmappedPostRemoveCascadeDeleters.length; i++) {
                    returningAttributes.add(unmappedPostRemoveCascadeDeleters[i].getAttributeValuePath());
                }

                CriteriaBuilder<Object[]> cb = context.getEntityViewManager().getCriteriaBuilderFactory().create(context.getEntityManager(), Object[].class);
                cb.from(elementEntityClass);
                //TODO: add exception for when the set ownerIdAttributeNames and ownerIds.keySet() do not match in size!
                for(String ownerIdAttributeName : ownerIdAttributeNames){
                    cb.where(ownerIdAttributeName).eq(ownerIds.get(ownerIdAttributeName));
                }                for (String attribute : returningAttributes) {
                    cb.select(attribute);
                }
                for(String elementIdAttributeName : elementIdAttributeNames){
                    cb.select(elementIdAttributeName);
                }
                returnedValues = cb.getSingleResult();
                id = returnedValues[returnedValues.length - 1];

                for (int i = 0; i < unmappedPreRemoveCascadeDeleters.length; i++) {
                    unmappedPreRemoveCascadeDeleters[i].removeByOwnerId(context, id);
                }
            }
            removeWithoutPreCascadeDelete(context, ownerId, returnedValues, id);
        }
    }

    private void removeWithoutPreCascadeDelete(UpdateContext context, Object ownerId, Object[] returnedValues, Object id) {
        boolean doDelete = true;
        //TODO: figure out whether I should use ownerIdAttributeNames or elementIdAttributeNames below, and whether I should use the elementEntityClass or attempt to find the ownerEntityClass.
        Map<String, Object> ownerIdFieldValueMap = new HashMap<>();
        getDeclaredFieldsFromId(ownerId, ownerIdFieldValueMap,ownerIdAttributeNames);

        Map<String, Object> idFieldValueMap = new HashMap<>();
        getDeclaredFieldsFromId(id, idFieldValueMap,elementIdAttributeNames);

        Map<String, Object> ownerIdValueMap = new HashMap<>();
        for (String ownerIdAttributeName : ownerIdAttributeNames){
            ownerIdValueMap.put(ownerIdAttributeName,ownerIdFieldValueMap.get(ownerIdAttributeName));
        }

        Map<String, Object> elementIdValueMap = new HashMap<>();
        for (String elementIdAttributeName : elementIdAttributeNames){
            elementIdValueMap.put(elementIdAttributeName,ownerIdFieldValueMap.get(elementIdAttributeName));
        }


        // need to "return" the values from the delete query for the post deleters since the values aren't available after executing the delete query
        if (unmappedPostRemoveCascadeDeleters.length != 0 && returnedValues == null) {
            List<String> returningAttributes = new ArrayList<>();
            for (int i = 0; i < unmappedPostRemoveCascadeDeleters.length; i++) {
                returningAttributes.add(unmappedPostRemoveCascadeDeleters[i].getAttributeValuePath());
            }

            EntityViewManagerImpl evm = context.getEntityViewManager();
            // If the dbms supports it, we use the returning feature to do this
            if (evm.getDbmsDialect().supportsReturningColumns()) {
                DeleteCriteriaBuilder<?> cb = evm.getCriteriaBuilderFactory().delete(context.getEntityManager(), elementEntityClass);
                //TODO: add exception for when the set ownerIdAttributeNames and ownerIds.keySet() do not match in size!
                if (id == null) {
                    for(String ownerIdAttributeName : ownerIdAttributeNames){
                        cb.where(ownerIdAttributeName).eq(ownerIdValueMap.get(ownerIdAttributeName));
                    }
                    //TODO: I believe here we assume that the attribute to be considered only consists of one id; but maybe this will go through fine if id is a composite id as well. Edit: it does not.
                } else {
                    for(String elementIdAttributeName : elementIdAttributeNames){
                        cb.where(elementIdAttributeName).eq(elementIdValueMap.get(elementIdAttributeName));
                    }
                }

                ReturningResult<Tuple> result = cb.executeWithReturning(returningAttributes.toArray(new String[returningAttributes.size()]));
                returnedValues = result.getLastResult().toArray();
                doDelete = false;
            } else {
                // Otherwise we query the attributes
                CriteriaBuilder<Object[]> cb = evm.getCriteriaBuilderFactory().create(context.getEntityManager(), Object[].class);
                cb.from(elementEntityClass);
                if (id == null) {
                    for(String ownerIdAttributeName : ownerIdAttributeNames){
                        cb.where(ownerIdAttributeName).eq(ownerIdValueMap.get(ownerIdAttributeName));
                    }
                } else {
                    for(String elementIdAttributeName : elementIdAttributeNames){
                        cb.where(elementIdAttributeName).eq(elementIdValueMap.get(elementIdAttributeName));
                    }
                }
                for (String attribute : returningAttributes) {
                    cb.select(attribute);
                }
                for(String elementIdAttributeName : elementIdAttributeNames){
                    cb.select(elementIdAttributeName);
                }
                returnedValues = cb.getSingleResult();
                id = returnedValues[returnedValues.length - 1];
            }
        }

        if (doDelete) {
            if (requiresDeleteAsEntity) {
                if (id == null) {
                    throw new UnsupportedOperationException("Delete by owner id should not be invoked!");
                }
                context.getEntityManager().remove(context.getEntityManager().getReference(elementEntityClass, id));
            } else {
                if (id == null) {
                    Query query = context.getEntityManager().createQuery(deleteByOwnerIdQuery);
                    for (String ownerIdAttributeName : ownerIdAttributeNames)
                    query.setParameter(ownerIdAttributeName.replace(".","_")+"_variable",ownerIdValueMap.get(ownerIdAttributeName));
                    query.executeUpdate();
                } else {
                    Query query = context.getEntityManager().createQuery(deleteQuery);
                    for (String elementIdAttributeName : elementIdAttributeNames){
                        query.setParameter(elementIdAttributeName.replace(".","_") + "_variable",elementIdValueMap.get(elementIdAttributeName));
                    }
                    query.executeUpdate();
                }
            }
        }

        for (int i = 0; i < unmappedPostRemoveCascadeDeleters.length; i++) {
            if (returnedValues[i] != null) {
                unmappedPostRemoveCascadeDeleters[i].removeById(context, returnedValues[i]);
            }
        }
    }
//TODO: make this thing recursive so as to deal with embedded Id's as well.
    private void getDeclaredFieldsFromId(Object id, Map<String, Object> idFieldValueMap, Set<String> idAttributes) {
        if (idAttributes.size()==1) {
            Iterator iterator = idAttributes.iterator();
            idFieldValueMap.put((String) iterator.next(), id);
        } else {
            for (Field field : id.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(Boolean.TRUE);
                    idFieldValueMap.put(field.getName(), field.get(id));
                } catch (IllegalAccessException e) {
                    new IllegalAccessException("Unable to access field" + field.getName() + ".");
                }
            }
        }
    }

    @Override
    public UnmappedAttributeCascadeDeleter createFlusherWiseDeleter() {
        return this;
    }
}
