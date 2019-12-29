/*
 * Copyright 2014 - 2019 Blazebit.
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

package com.blazebit.persistence.criteria.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.FullSelectCTECriteriaBuilder;
import com.blazebit.persistence.LeafOngoingSetOperationCTECriteriaBuilder;
import com.blazebit.persistence.SelectBaseCTECriteriaBuilder;
import com.blazebit.persistence.SetOperationBuilder;
import com.blazebit.persistence.StartOngoingSetOperationBuilder;
import com.blazebit.persistence.criteria.BlazeFullSelectCTECriteria;
import com.blazebit.persistence.spi.SetOperationType;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlazeFullSelectCTECriteriaImpl<T> extends AbstractBlazeSelectBaseCTECriteria<T> implements BlazeFullSelectCTECriteria<T> {

    private final LinkedHashMap<SetOperationType, BlazeFullSelectCTECriteriaImpl<T>> setFragments = new LinkedHashMap<>();

    public BlazeFullSelectCTECriteriaImpl(BlazeCriteriaBuilderImpl criteriaBuilder, Class<T> returnType) {
        super(criteriaBuilder, returnType);
    }

    @Override
    public BlazeFullSelectCTECriteria<T> union() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.UNION, fragment);
        return fragment;
    }

    @Override
    public BlazeFullSelectCTECriteria<T> unionAll() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.UNION_ALL, fragment);
        return fragment;
    }

    @Override
    public BlazeFullSelectCTECriteria<T> intersect() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.INTERSECT, fragment);
        return fragment;
    }

    @Override
    public BlazeFullSelectCTECriteria<T> intersectAll() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.INTERSECT_ALL, fragment);
        return fragment;
    }

    @Override
    public BlazeFullSelectCTECriteria<T> except() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.EXCEPT, fragment);
        return fragment;
    }

    @Override
    public BlazeFullSelectCTECriteria<T> exceptAll() {
        BlazeFullSelectCTECriteriaImpl<T> fragment = new BlazeFullSelectCTECriteriaImpl<>(criteriaBuilder, returnType);
        setFragments.put(SetOperationType.EXCEPT_ALL, fragment);
        return fragment;
    }

    @Override
    public <X> CriteriaBuilder<X> render(CriteriaBuilder<X> cbs) {
        return super.render(cbs);
    }

    @Override
    protected void render(SelectBaseCTECriteriaBuilder<?> fullSelectCTECriteriaBuilder, RenderContextImpl context) {
        super.render(fullSelectCTECriteriaBuilder, context);
        if (fullSelectCTECriteriaBuilder instanceof FullSelectCTECriteriaBuilder) {
            renderSetFragments((FullSelectCTECriteriaBuilder) fullSelectCTECriteriaBuilder, context);
        }
    }

    private void renderSetFragments(FullSelectCTECriteriaBuilder<?> fullSelectCTECriteriaBuilder, RenderContextImpl context) {
        LeafOngoingSetOperationCTECriteriaBuilder<?> cb = null;
        for (Map.Entry<SetOperationType, BlazeFullSelectCTECriteriaImpl<T>> entry : setFragments.entrySet()) {
            SetOperationType type = entry.getKey();
            BlazeFullSelectCTECriteriaImpl<T> value = entry.getValue();
            switch (type) {
                case UNION:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).union();
                    break;
                case UNION_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).unionAll();
                    break;
                case INTERSECT:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).intersect();
                    break;
                case INTERSECT_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).intersectAll();
                    break;
                case EXCEPT:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).except();
                    break;
                case EXCEPT_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).exceptAll();
                    break;
            }

            value.render((SelectBaseCTECriteriaBuilder) cb, context);
        }
        if (cb instanceof StartOngoingSetOperationBuilder) {
            ((StartOngoingSetOperationBuilder) cb).endSet();
        }
    }

}
