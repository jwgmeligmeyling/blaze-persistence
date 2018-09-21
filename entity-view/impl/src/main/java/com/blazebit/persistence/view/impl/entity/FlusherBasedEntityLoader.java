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

import com.blazebit.persistence.parser.util.JpaMetamodelUtils;
import com.blazebit.persistence.view.impl.accessor.AttributeAccessor;
import com.blazebit.persistence.view.impl.update.UpdateContext;
import com.blazebit.persistence.view.impl.update.flush.DirtyAttributeFlusher;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            sb.append(" WHERE e.").append(idAttributeName).append(" = :"+idAttributeName.replace(".","_")+"_variable"+" AND");
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
        Map<String, Object> ownerIds = JpaMetamodelUtils.getIdNameValueMap(entityClass,id,em.getMetamodel());
        //TODO: create IdClass test, as idType will probably return null for that case.
        EntityType<?> entityType = em.getMetamodel().entity(entityClass);
        Query query = em.createQuery(getQueryString());
        for(String idAttributeName : idAttributeNames){
            query.setParameter(idAttributeName.replace(".","_")+"_variable",ownerIds.get(idAttributeName));
        }

        List<Object> list = query.getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Required entity '" + entityClass.getName() + "' with id '" + id + "' couldn't be found!");
        }
        //TODO check whether this does not only return one element in the case of an IdClass.
        return list.get(0);
    }
}
