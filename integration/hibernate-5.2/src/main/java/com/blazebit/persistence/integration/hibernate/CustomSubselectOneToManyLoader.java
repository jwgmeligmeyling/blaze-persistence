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

package com.blazebit.persistence.integration.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.collection.SubselectOneToManyLoader;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class CustomSubselectOneToManyLoader extends SubselectOneToManyLoader {

    private int cteParameterCount;
    private final Map<String, int[]> namedParameterLocMap;

    public CustomSubselectOneToManyLoader(QueryableCollection persister, String subquery, java.util.Collection entityKeys, QueryParameters queryParameters, Map<String, int[]> namedParameterLocMap, SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
        super(persister, subquery, entityKeys, queryParameters, namedParameterLocMap, factory, loadQueryInfluencers);
        String originalSql = queryParameters.getFilteredSQL();
        if (originalSql.startsWith("with ")) {
            StringBuilder sb = new StringBuilder(sql.length() + originalSql.length());
            int brackets = 0;
            boolean cteMode = false;
            for (int i = 0; i < originalSql.length(); i++) {
                final char c = originalSql.charAt(i);
                if (c == '(') {
                    brackets++;
                } else if (c == ')') {
                    brackets--;
                    if (brackets == 0) {
                        cteMode = !cteMode;
                    }
                } else if (c == '?') {
                    // TODO: escaped ?'s, use parser (doesn't the same apply to the brackets above?)
                    cteParameterCount++;
                }

                if (!cteMode && brackets == 0 && originalSql.regionMatches(true, i, "select ", 0, "select ".length())) {
                    break;
                }

                sb.append(c);
            }

            sb.append(sql);
            this.sql = sb.toString();
        }
        this.namedParameterLocMap = namedParameterLocMap;
    }

    @Override
    protected int bindParameterValues(PreparedStatement statement, QueryParameters queryParameters, int startIndex, SharedSessionContractImplementor session) throws SQLException {
        if (cteParameterCount == 0) {
            return super.bindParameterValues(statement, queryParameters, startIndex, session);
        }
        final Object[] values = queryParameters.getFilteredPositionalParameterValues();
        final Type[] types = queryParameters.getFilteredPositionalParameterTypes();

        // TODO: Figure out how to determine how many of the positioned parameters belong to the CTE part
        // Haven't been able to create a CTE query with positioned parameters yet...
        int ctePositionedParameterSpan = 0;
        int cteNamedParameterSpan = cteParameterCount - ctePositionedParameterSpan;

        final Object[] cteValues = Arrays.copyOfRange(values, 0, ctePositionedParameterSpan);
        final Type[] cteTypes = Arrays.copyOfRange(types, 0, ctePositionedParameterSpan);

        final Object[] remainderValues = Arrays.copyOfRange(values, ctePositionedParameterSpan, values.length);
        final Type[] remainderTypes = Arrays.copyOfRange(types, ctePositionedParameterSpan, values.length);

        Map<String, TypedValue> cteNamedParameters = new LinkedHashMap<>();
        Map<String, TypedValue> remainderNamedParameters = new LinkedHashMap<>();
        SortedMap<Integer, String> invertedLocMap = getInvertedLocMap();

        for (Map.Entry<Integer, String> entry : invertedLocMap.entrySet()) {
            String name = entry.getValue();
            TypedValue typedValue = queryParameters.getNamedParameters().get(name);
            if (entry.getKey() < cteNamedParameterSpan) {
                cteNamedParameters.put(name, typedValue);
            }
            else {
                remainderNamedParameters.put(name, typedValue);
            }
        }

        int span = 0;
        span += bindPositionalParameters(statement, cteValues, cteTypes, startIndex + span, session);
        span += bindNamedParameters(statement, cteNamedParameters, startIndex + span, session);
        span += bindPositionalParameters(statement, remainderValues, remainderTypes, startIndex + span, session);
        span += bindNamedParameters(statement, remainderNamedParameters, startIndex + span, session);
        return span;
    }

    protected int bindPositionalParameters(
            final PreparedStatement statement,
            final Object[] values,
            final Type[] types,
            final int startIndex,
            final SharedSessionContractImplementor session) throws SQLException, HibernateException {
        int span = 0;
        for ( int i = 0; i < values.length; i++ ) {
            types[i].nullSafeSet( statement, values[i], startIndex + span, session );
            span += types[i].getColumnSpan( getFactory() );
        }
        return span;
    }

    @Override
    protected int bindPositionalParameters(PreparedStatement statement, QueryParameters queryParameters, int startIndex, SharedSessionContractImplementor session) throws SQLException, HibernateException {
        // This method is unused, but mapping it to the new implementation anyway
        return bindPositionalParameters(
                statement,
                queryParameters.getFilteredPositionalParameterValues(),
                queryParameters.getFilteredPositionalParameterTypes(),
                startIndex,
                session
        );
    }

    @Override
    protected int bindNamedParameters(PreparedStatement statement, Map<String, TypedValue> namedParams, int startIndex, SharedSessionContractImplementor session) throws SQLException, HibernateException {
        int span = 0;
        if ( CollectionHelper.isEmpty( namedParams ) ) {
            return span;
        }

        for ( String name : namedParams.keySet() ) {
            TypedValue typedValue = namedParams.get( name );
            int columnSpan = typedValue.getType().getColumnSpan( getFactory() );
            // TODO figure out importance of locs and whether this "fix" has any side-effects
            int[] locs = getNamedParameterLocs( name );
            typedValue.getType().nullSafeSet( statement, typedValue.getValue(), (startIndex + span) , session );
            span += columnSpan;
        }
        return span;
    }

    private SortedMap<Integer, String> getInvertedLocMap() {
        SortedMap<Integer, String> invertedLocMap = new TreeMap<>();

        for (Map.Entry<String, int[]> entry : namedParameterLocMap.entrySet()) {
            for (int index : entry.getValue()) {
                invertedLocMap.put(index, entry.getKey());
            }
        }
        return invertedLocMap;
    }

}