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


package com.blazebit.persistence.querydsl.experimental

import com.blazebit.persistence.*
import com.mysema.query.jpa.JPQLSerializer
import com.mysema.query.jpa.JPQLTemplates
import com.mysema.query.types.Expression
import com.mysema.query.types.Path
import com.blazebit.persistence.querydsl.Counter

/*
 * TODO:
 *  These methods require access to the ParameterHolder, which is not always the result of the
 *  builder step (i.e. WhereOrBuilder). Probably we need to pass along some state in order to make that happen.
 *
 *  These extension methods allow one to mix Blaze Persist expressions / builder functions with QSQL expressions, ie.
 *      where(some.path).between("alias.x").and(some.otherPath)
 *
 *  Currently one could only do:
 *      where(some.path.between(alias.x).and(some.otherPath))
 *
 *  Currently, only path and other simple expressions will work, constants / parameters will
 *  not work.
 */

/**
 * Starts a RestrictionBuilder for a where predicate with the given expression as left hand expression.
 *
 * @param path The left hand expression for a where predicate
 * @return The restriction builder for the given expression
 */
fun <A> A.where(path: Path<*>) : RestrictionBuilder<A> where A : BaseWhereBuilder<A> {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(path)
    return this.where(jpqlQueryFragment)
}

/**
 * Starts a RestrictionBuilder for a where predicate with the given expression as left hand expression.
 *
 * @param path The left hand expression for a where predicate
 * @return The restriction builder for the given expression
 */
fun <A> A.having(path: Path<*>) : RestrictionBuilder<A> where A : BaseHavingBuilder<A> {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(path)
    return this.having(jpqlQueryFragment)
}

/**
 * Finishes the EQ predicate and adds it to the parent predicate container represented by the type T.
 * The predicate checks if the left hand side is equal to the given expression.
 *
 * @param expression The expression on the right hand side
 * @return The parent predicate container builder
 */
fun <A : RestrictionBuilder<T>, T> A.eqExpression(expression: Expression<*>) : T {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.eqExpression(jpqlQueryFragment)
}

/**
 * Finishes the NEQ predicate and adds it to the parent predicate container represented by the type T.
 * The predicate checks if the left hand side is equal to the given expression.
 *
 * @param expression The expression on the right hand side
 * @return The parent predicate container builder
 */
fun <A : RestrictionBuilder<T>, T> A.notEqExpression(expression: Expression<*>) : T {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.notEqExpression(jpqlQueryFragment)
}

/**
 * Starts a builder for a between predicate with lower bound expression.
 *
 * @param expression The between start expression
 * @return The BetweenBuilder
 */
fun <A : RestrictionBuilder<T>, T> A.betweenExpression(expression: Expression<*>) : BetweenBuilder<T> {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.betweenExpression(jpqlQueryFragment)
}

/**
 * Starts a builder for a not between predicate with lower bound expression.
 *
 * @param expression The between start expression
 * @return The BetweenBuilder
 */
fun <A : RestrictionBuilder<T>, T> A.notBetweenExpression(expression: Expression<*>) : BetweenBuilder<T> {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.notBetweenExpression(jpqlQueryFragment)
}

/**
 * Constructs a between predicate with an expression as upper bound.
 *
 * @param expression The upper bound expression
 * @return The parent predicate container builder
 */
fun <A> BetweenBuilder<A>.andExpression(expression: Expression<*>) : A {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.andExpression(jpqlQueryFragment)
}

/**
 * Constructs a between predicate with an expression as upper bound.
 *
 * @param expression The upper bound expression
 * @return The parent predicate container builder
 */
fun <A> BinaryPredicateBuilder<A>.andExpression(expression: Expression<*>) : A {
    val (jpqlQueryFragment, parameters) = parseExpressionAndBindParameters(expression)
    return this.expression(jpqlQueryFragment)
}

private fun Any.parseExpressionAndBindParameters(expression : Expression<*>) : Pair<String, HashMap<String, Any>> {
    val ser = JPQLSerializer(JPQLTemplates.DEFAULT)
    expression.accept(ser, null)
    var jpqlQueryFragment = ser.toString()

    val parameters = HashMap<String, Any>()
    for ((constant, label) in ser.constantToLabel) {
        val parameterName = "qsl_param_${Counter.getCount()}"
        parameters[parameterName] = constant
        jpqlQueryFragment = jpqlQueryFragment.replace("?$label", ":$parameterName")
    }

    return Pair(jpqlQueryFragment, parameters)
}
