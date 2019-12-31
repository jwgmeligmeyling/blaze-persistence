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

import com.blazebit.persistence.BaseOngoingSetOperationBuilder;
import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.FinalSetOperationCTECriteriaBuilder;
import com.blazebit.persistence.FullSelectCTECriteriaBuilder;
import com.blazebit.persistence.LeafOngoingSetOperationCTECriteriaBuilder;
import com.blazebit.persistence.SelectBaseCTECriteriaBuilder;
import com.blazebit.persistence.SetOperationBuilder;
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
    protected <X> CriteriaBuilder<X> renderAndFinalize(FullSelectCTECriteriaBuilder<CriteriaBuilder<X>> fullSelectCTECriteriaBuilder, RenderContextImpl context) {
        Object render = render(fullSelectCTECriteriaBuilder, context);
        if (! setFragments.isEmpty()) {
            // TODO Unfortunately, in this case we're dealing with a FinalSetOperationCTECriteriaBuilder that
            //  already closed the FullSelectCTECriteriaBuilder
            return (CriteriaBuilder<X>) render;
        }
        return ((FullSelectCTECriteriaBuilder<CriteriaBuilder<X>>) render).end();
    }

    @Override
    protected <CB extends SelectBaseCTECriteriaBuilder<?>> Object render(CB fullSelectCTECriteriaBuilder, RenderContextImpl context) {
        Object render = super.render(fullSelectCTECriteriaBuilder, context);
        if (! setFragments.isEmpty()) {
            return renderSetFragments((SetOperationBuilder) render, context);
        }
        return render;
    }

    // TODO: There is no interface that combines SetOperationBuilder and SelectBaseCTECriteriaBuilder
    // TODO: There is no generic assignment that satisfies all subtypes of SelectBaseCTECriteriaBuilder
    //   (i.e. both FullSelectCTECriteriaBuilderImpl and StartOngoingSetOperationCTECriteriaBuilderImpl)
    // TODO: Should we even pursue a recursive rendering approach based on the existing criteria API?
    //  Perhaps some type changes are required to neatly do this...
    private <X> X renderSetFragments(SetOperationBuilder<?, ?> /* & SelectBaseCTECriteriaBuilder */ fullSelectCTECriteriaBuilder, RenderContextImpl context) {
        System.out.println(fullSelectCTECriteriaBuilder.getClass());
        BaseOngoingSetOperationBuilder cb = null;

        for (Map.Entry<SetOperationType, BlazeFullSelectCTECriteriaImpl<T>> entry : setFragments.entrySet()) {
            SetOperationType type = entry.getKey();
            BlazeFullSelectCTECriteriaImpl<T> value = entry.getValue();

            // Use start-X methods, because sets defined on set queries should take precedence
            switch (type) {
                case UNION:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startUnion();
                    break;
                case UNION_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startUnionAll();
                    break;
                case INTERSECT:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startIntersect();
                    break;
                case INTERSECT_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startIntersectAll();
                    break;
                case EXCEPT:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startExcept();
                    break;
                case EXCEPT_ALL:
                    cb = (cb == null ? fullSelectCTECriteriaBuilder : cb).startExceptAll();
                    break;
            }

            cb = (BaseOngoingSetOperationBuilder) value.render((SelectBaseCTECriteriaBuilder) cb, context);
            cb = (BaseOngoingSetOperationBuilder) cb.endSet();
        }

        Object endSet = cb;

        // TODO: Not the case for the recursive case, only alternative seemingly is to have separate methods for every subtype of SelectBaseCTECriteriaBuilder...
        if (endSet instanceof LeafOngoingSetOperationCTECriteriaBuilder) {
            endSet = ((LeafOngoingSetOperationCTECriteriaBuilder) endSet).endSet();
        }

        if (endSet instanceof FinalSetOperationCTECriteriaBuilder) {
            renderOrderBy((FinalSetOperationCTECriteriaBuilder) endSet, context);
            endSet = ((FinalSetOperationCTECriteriaBuilder) endSet).end();
        }

        return (X) endSet;
    }

}
