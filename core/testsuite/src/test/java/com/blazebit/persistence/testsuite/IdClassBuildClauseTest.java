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

package com.blazebit.persistence.testsuite;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.entity.IdClassEntity;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Christian Beikov
 * @since 1.3.0
 */
public class IdClassBuildClauseTest extends AbstractCoreTest {

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{
                IdClassEntity.class
        };
    }

    @Override
    public void setUpOnce() {
        cleanDatabase();
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {

            }
        });
    }


    @Test
    public void testBuildClause() {
        String expectedFromIdentifiableValuesQuery = "SELECT entity1.key1 FROM(2 VALUES) ( " +
                "SELECT entity2.count(*) FROM entity2 IdClassEntity myValue";
        IdClassEntity e1 = new IdClassEntity(1, "1", 1);
        IdClassEntity e2 = new IdClassEntity(2, "2", 2);
        IdClassEntity e3 = new IdClassEntity(3, "3", 3);
        IdClassEntity e4 = new IdClassEntity(4, "4", 4);
        IdClassEntity e5 = new IdClassEntity(5, "5", 5);

        List<IdClassEntity> entities = new ArrayList<>();
        List<IdClassEntity> entities2 = new ArrayList<>();
        entities.add(e1);
        entities.add(e2);
        entities2.add(e3);
        entities2.add(e4);
        entities2.add(e5);

        CriteriaBuilder<Tuple> cb = cbf.create(em, Tuple.class)
                .fromIdentifiableValues(IdClassEntity.class, "entity1", entities)
                    .selectSubquery()
                    .fromIdentifiableValues(IdClassEntity.class, "entity2", entities)
                    .select("count(*)")
                .end()
                .select("entity1.key1");

        long numberOfEntities = Long.valueOf(0);
        for(Tuple tuple : cb.getQuery().getResultList()){
            numberOfEntities = numberOfEntities + (Long) tuple.get(0);
        }
        Assert.assertTrue(numberOfEntities==4);
    }
}
