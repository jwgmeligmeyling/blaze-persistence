/*
 * Copyright 2014 - 2020 Blazebit.
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

import com.blazebit.persistence.CTE;
import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import java.util.List;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class Issue1186Test extends AbstractCoreTest {

    private B instance;

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{ A.class, B.class };
    }

    @Override
    protected void setUpOnce() {
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                instance = new B();
                em.persist(instance);
            }
        });
    }

    @Test
    public void subselectEntityTest() {
        CriteriaBuilder<A> cb = cbf.create(em, A.class)
                .with(A.class)
                    .from(B.class, "instance")
                    .bind("b.id").select("instance.id")
                .end()
                .from(A.class, "a");

        List<A> resultList = cb.getResultList();
    }

    @CTE
    @Entity(name = "A")
    public static class A {

        @Id
        @Column
        private Long id;

        @ManyToOne
        @PrimaryKeyJoinColumn
        private B b;

    }

    @Entity(name = "B")
    public static class B {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

    }
}
