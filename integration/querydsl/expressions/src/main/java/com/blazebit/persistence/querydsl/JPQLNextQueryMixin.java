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

package com.blazebit.persistence.querydsl;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.JoinFlag;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.support.ReplaceVisitor;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.jpa.JPAListAccessVisitor;
import com.querydsl.jpa.JPAMapAccessVisitor;
import com.querydsl.jpa.JPAQueryMixin;

import java.util.Collections;

/**
 * {@code JPQLQueryMixin} extends {@link JPAQueryMixin} to remove a few AST replacements that are
 * undesirable for stock Blaze-Persistence features (i.e. array index).
 *
 * @param <T> query type
 * @author Jan-Willem Gmelig Meyling
 * @since 1.6.7
 */
public class JPQLNextQueryMixin<T> extends JPAQueryMixin<T> {

    /**
     * Lateral join flag.
     * Used internally for implementing {@link #lateral()}.
     */
    public static final JoinFlag LATERAL = new JoinFlag("LATERAL", JoinFlag.Position.BEFORE_TARGET);

    /**
     * Default join flag.
     */
    public static final JoinFlag DEFAULT = new JoinFlag("DEFAULT");

    public JPQLNextQueryMixin() {
        this(null, new DefaultQueryMetadata());
    }

    public JPQLNextQueryMixin(QueryMetadata metadata) {
        this(null, metadata);
    }

    public JPQLNextQueryMixin(T self, QueryMetadata metadata) {
        super(self, metadata);
        this.mapAccessVisitor = new JPQLMapAccessVisitor(metadata);
        this.listAccessVisitor = new JPQLListAccessVistor(metadata);
        this.replaceVisitor = new ReplaceVisitor<Void>() {
            @Override
            public Expression<?> visit(SubQueryExpression<?> expr, Void context) {
                return expr;
            }
        };
    }

    public T defaultJoin() {
        addJoinFlag(DEFAULT);
        return getSelf();
    }

    public T lateral() {
        addJoinFlag(LATERAL);
        return getSelf();
    }

    /**
     /**
     * Bypass the logic in {@code JPAMapAccessVisitor} to replace array expressions with joins,
     * as Blaze-Persistence has built-in support for array expressions (and this way utilizes
     * existing default joins and does not clash with generated aliases).
     *
     * @author Jan-Willem Gmelig Meyling
     * @since 1.6.7
     */
    public class JPQLMapAccessVisitor extends JPAMapAccessVisitor {

        JPQLMapAccessVisitor(QueryMetadata metadata) {
            super(metadata, Collections.<Expression<?>, Path<?>> emptyMap());
        }

        @Override
        public Expression<?> visit(Path<?> expr, Void context) {
            // Call replaceVisitor instead of super, as we need super.super.
            return replaceVisitor.visit(expr, context);
        }

    }
    /**
     * Bypass the logic in {@code JPAListAccessVisitor} to replace array expressions with joins,
     * as Blaze-Persistence has built-in support for array expressions (and this way utilizes
     * existing default joins and does not clash with generated aliases).
     *
     * @author Jan-Willem Gmelig Meyling
     * @since 1.6.7
     */
    public class JPQLListAccessVistor extends JPAListAccessVisitor {

        JPQLListAccessVistor(QueryMetadata metadata) {
            super(metadata, Collections.<Expression<?>, Path<?>> emptyMap());
        }


        @Override
        public Expression<?> visit(Path<?> expr, Void context) {
            // Call replaceVisitor instead of super, as we need super.super.
            return replaceVisitor.visit(expr, context);
        }

    }

}
