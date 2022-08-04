/*
 * Copyright 2014 - 2022 Blazebit.
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

import com.blazebit.persistence.BaseSubqueryBuilder;
import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.FinalSetOperationCriteriaBuilder;
import com.blazebit.persistence.FinalSetOperationSubqueryBuilder;
import com.blazebit.persistence.FromBuilder;
import com.blazebit.persistence.FullQueryBuilder;
import com.blazebit.persistence.GroupByBuilder;
import com.blazebit.persistence.HavingBuilder;
import com.blazebit.persistence.JoinOnBuilder;
import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.LeafOngoingSetOperationCriteriaBuilder;
import com.blazebit.persistence.LeafOngoingSetOperationSubqueryBuilder;
import com.blazebit.persistence.MultipleSubqueryInitiator;
import com.blazebit.persistence.OrderByBuilder;
import com.blazebit.persistence.SelectBuilder;
import com.blazebit.persistence.SelectObjectBuilder;
import com.blazebit.persistence.SubqueryBuilder;
import com.blazebit.persistence.SubqueryInitiator;
import com.blazebit.persistence.WhereBuilder;
import com.blazebit.persistence.criteria.BlazeAbstractQuery;
import com.blazebit.persistence.criteria.BlazeCriteriaSetQuery;
import com.blazebit.persistence.criteria.BlazeJoin;
import com.blazebit.persistence.criteria.BlazeOrder;
import com.blazebit.persistence.criteria.BlazeRoot;
import com.blazebit.persistence.criteria.BlazeSubquery;
import com.blazebit.persistence.criteria.impl.RenderContext.ClauseType;
import com.blazebit.persistence.criteria.impl.expression.AbstractSelection;
import com.blazebit.persistence.criteria.impl.expression.SubqueryExpression;
import com.blazebit.persistence.criteria.impl.path.AbstractFrom;
import com.blazebit.persistence.criteria.impl.path.AbstractJoin;
import com.blazebit.persistence.criteria.impl.path.EntityJoin;
import com.blazebit.persistence.criteria.impl.path.RootImpl;
import com.blazebit.persistence.criteria.impl.path.TreatedPath;

import javax.persistence.Tuple;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Christian Beikov
 * @since 1.2.0
 */
public class InternalSetQuery<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BlazeCriteriaSetQuery<T> owner;
    private final BlazeCriteriaBuilderImpl criteriaBuilder;

    private final BlazeCriteriaQueryImpl<T> lhs, rhs;
    private List<BlazeOrder> orderList = Collections.emptyList();

    public InternalSetQuery(BlazeCriteriaSetQuery<T> owner, BlazeCriteriaBuilderImpl criteriaBuilder, BlazeCriteriaQueryImpl<T> lhs, BlazeCriteriaQueryImpl<T> rhs) {
        this.owner = owner;
        this.criteriaBuilder = criteriaBuilder;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /* Order by */

    public List<BlazeOrder> getBlazeOrderList() {
        return orderList;
    }

    public void setBlazeOrderList(List<BlazeOrder> orderList) {
        this.orderList = orderList;
    }

    @SuppressWarnings({"unchecked"})
    public List<Order> getOrderList() {
        return (List<Order>) (List<?>) orderList;
    }

    @SuppressWarnings({"unchecked"})
    public void setOrderList(List<Order> orderList) {
        this.orderList = (List<BlazeOrder>) (List<?>) orderList;
    }

    /* Parameters */

    public Set<ParameterExpression<?>> getParameters() {
        // NOTE: we have to always visit them because it's not possible to cache that easily
        ParameterVisitor visitor = new ParameterVisitor();

        return visitor.getParameters();
    }

    /* Rendering */

    public FinalSetOperationCriteriaBuilder<T> render(CriteriaBuilder<T> cb) {
        CriteriaBuilder<T> render = lhs.getInternalQuery().render(cb);
        LeafOngoingSetOperationCriteriaBuilder<T> union = render.union();
        FinalSetOperationCriteriaBuilder<T> tFinalSetOperationCriteriaBuilder = rhs.getInternalQuery().render(union).endSet();

        RenderContextImpl context = new RenderContextImpl();

        renderOrderBy(tFinalSetOperationCriteriaBuilder, context);

        for (ImplicitParameterBinding b : context.getImplicitParameterBindings()) {
            b.bind(tFinalSetOperationCriteriaBuilder);
        }

        for (Map.Entry<ParameterExpression<?>, String> entry : context.getExplicitParameterMapping().entrySet()) {
            tFinalSetOperationCriteriaBuilder.registerCriteriaParameter(entry.getValue(), entry.getKey());
        }

        return tFinalSetOperationCriteriaBuilder;
    }

    public void renderSubquery(RenderContext context) {
        RenderContextImpl contextImpl = (RenderContextImpl) context;
        SubqueryInitiator<?> initiator = context.getSubqueryInitiator();
        SubqueryBuilder<?> cb = lhs.getInternalQuery().renderSubqueryFrom(initiator, contextImpl);
        LeafOngoingSetOperationSubqueryBuilder<?> union = cb.union();
        LeafOngoingSetOperationSubqueryBuilder<?> leafOngoingSetOperationSubqueryBuilder = rhs.getInternalQuery().renderSubqueryFrom(union, contextImpl);
        FinalSetOperationSubqueryBuilder<?> finalSetOperationSubqueryBuilder = leafOngoingSetOperationSubqueryBuilder.endSet();

        renderOrderBy(finalSetOperationSubqueryBuilder, contextImpl);

        finalSetOperationSubqueryBuilder.end();
    }

    private void renderOrderBy(OrderByBuilder<?> ob, RenderContextImpl context) {
        if (orderList == null) {
            return;
        }

        context.setClauseType(ClauseType.ORDER_BY);
        for (Order order : orderList) {
            context.getBuffer().setLength(0);
            ((AbstractSelection<?>) order.getExpression()).render(context);
            String expression = context.takeBuffer();
            Map<String, InternalQuery<?>> aliasToSubqueries = context.takeAliasToSubqueryMap();

            if (aliasToSubqueries.isEmpty()) {
                boolean nullsFirst = false;

                if (order instanceof BlazeOrder) {
                    nullsFirst = ((BlazeOrder) order).isNullsFirst();
                }

                ob.orderBy(expression, order.isAscending(), nullsFirst);
            } else {
                throw new IllegalArgumentException("Subqueries are not supported in the order by clause!");
//                MultipleSubqueryInitiator<?> initiator = ob.orderBySubqueries(expression);
//
//                for (Map.Entry<String, InternalQuery<?>> subqueryEntry : aliasToSubqueries.entrySet()) {
//                    context.pushSubqueryInitiator(initiator.with(subqueryEntry.getKey()));
//                    subqueryEntry.getValue().renderSubquery(context);
//                    context.popSubqueryInitiator();
//                }
//
//                initiator.end();
            }
        }
    }

}
