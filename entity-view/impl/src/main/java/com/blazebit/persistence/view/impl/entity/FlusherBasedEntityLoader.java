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

package com.blazebit.persistence.view.impl.entity;

import com.blazebit.persistence.view.impl.accessor.AttributeAccessor;
import com.blazebit.persistence.view.impl.update.UpdateContext;
import com.blazebit.persistence.view.impl.update.flush.DirtyAttributeFlusher;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.util.*;
/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class FlusherBasedEntityLoader extends AbstractEntityLoader {

    private final DirtyAttributeFlusher<?, Object, Object>[] flushers;
    private volatile String queryString;

    public FlusherBasedEntityLoader(Class<?> entityClass, Set<SingularAttribute<?, ?>> jpaIdAttributes, ViewToEntityMapper viewIdMapper, AttributeAccessor entityIdAccessor, DirtyAttributeFlusher<?, Object, Object>[] flushers) {
        super(entityClass, jpaIdAttributes, viewIdMapper, entityIdAccessor);
        this.flushers = flushers;
        // TODO: optimize by copying more from existing loaders and avoid object allocations
        // TODO: consider constructing query eagerly,
    }

    private String getQueryString() {
        String query = queryString;
        if (query != null) {
            return query;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("SELECT e FROM ").append(entityClass.getName()).append(" e");
        for (int i = 0; i < flushers.length; i++) {
            if (flushers[i] != null) {
                flushers[i].appendFetchJoinQueryFragment("e", sb);
            }
        }
        for (String idAttributeName : idAttributeNames){
            sb.append(" WHERE e.").append(idAttributeName).append(" = :"+idAttributeName+"_variable"+" AND");
        }
        sb.setLength(sb.length() - " AND".length());
        query = sb.toString();
        queryString = query;
        return query;
    }

    @Override
    public Object toEntity(UpdateContext context, Object id) {
        if (id == null || entityIdAccessor == null) {
            return createEntity();
        }

        return getReferenceOrLoad(context, id);
    }

    @Override
    protected Object queryEntity(EntityManager em, Object id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> ownerIds = new HashMap<>();
        Type<?> idType = em.getMetamodel().entity(entityClass).getIdType();
        if(idType.getPersistenceType().equals(Type.PersistenceType.BASIC)){
            Iterator iterator = idAttributeNames.iterator();
            if(!iterator.hasNext()){
                throw new RuntimeException("The entity type" + entityClass.getName() + "does not have an Id!");
            }

            //I believe this approach also works for BigDecimal and the like? I have added that possibility in the check above.
            ownerIds.put((String) iterator.next(),id);

            if (iterator.hasNext()){
                throw new RuntimeException("Could not match the given entityId to the entity type specified in the view: the entity type"
                        + entityClass.getName() + "has more than one Id field!");
            }

        } else {
            for (Field field : Arrays.asList(id.getClass().getFields())){
                try {
                    ownerIds.put(field.getName(),field.get(id));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot access id class property: " + e.getMessage(), e);
                }

            }
        }
        Query query = em.createQuery(getQueryString());
        for(String idAttributeName : idAttributeNames){
            query.setParameter(idAttributeName+"_variable",ownerIds.get(idAttributeName));
        }
        //TODO: check whether the result list is actually of the form assumed in the test below.
        List<Object> list = query.getResultList();
        for(Map.Entry<String,Object> entry : ownerIds.entrySet()){
            if (!list.contains(entry.getValue())) {
                throw new EntityNotFoundException("Required entity '" + entityClass.getName() + "' with id '" + entry.getKey() + "' couldn't be found!");
            }
        }

        return list.get(0);
    }
}
