package com.blazebit.persistence.criteria;

public interface BlazeSetOperationCriteria<X> {

    public X union();

    public X unionAll();

    public X intersect();

    public X intersectAll();

    public X except();

    public X exceptAll();

}
