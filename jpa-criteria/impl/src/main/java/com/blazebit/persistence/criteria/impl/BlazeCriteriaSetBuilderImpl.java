package com.blazebit.persistence.criteria.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.FinalSetOperationCriteriaBuilder;
import com.blazebit.persistence.criteria.BlazeCriteriaBuilder;
import com.blazebit.persistence.criteria.BlazeCriteriaSetQuery;
import com.blazebit.persistence.criteria.BlazeOrder;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Order;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BlazeCriteriaSetBuilderImpl<T> implements BlazeCriteriaSetQuery<T> {


    private final BlazeCriteriaBuilderImpl criteriaBuilder;
    private final Class<T> returnType;
    private final InternalSetQuery<T> query;

    public BlazeCriteriaSetBuilderImpl(BlazeCriteriaBuilderImpl criteriaBuilder, Class<T> returnType, BlazeCriteriaQueryImpl<T> a, BlazeCriteriaQueryImpl<T> b) {
        this.criteriaBuilder = criteriaBuilder;
        this.returnType = returnType;
        this.query =  new InternalSetQuery<T>(this, criteriaBuilder, a, b);
    }

    @Override
    public FinalSetOperationCriteriaBuilder<T> createCriteriaBuilder(EntityManager entityManager) {
        CriteriaBuilder<T> cb = criteriaBuilder.getCriteriaBuilderFactory().create(entityManager, returnType);
        return query.render(cb);
    }

    @Override
    public BlazeCriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    @Override
    public List<BlazeOrder> getBlazeOrderList() {
        return query.getBlazeOrderList();
    }

    @Override
    public BlazeCriteriaSetQuery<T> orderBy(Order... orders) {
        if (orders == null || orders.length == 0) {
            query.setOrderList(Collections.EMPTY_LIST);
        } else {
            query.setOrderList(Arrays.asList(orders));
        }

        return this;
    }

    @Override
    public BlazeCriteriaSetQuery<T> orderBy(List<Order> orderList) {
        query.setOrderList(orderList);
        return this;
    }

    @Override
    public Class<T> getResultType() {
        return returnType;
    }


}
