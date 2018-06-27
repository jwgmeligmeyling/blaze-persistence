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

import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Jan-Willem Gmelig Meyling
 * @version 1.3.0
 */
public class HibernateClassFilterSupportTest extends AbstractCoreTest {

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] { Account.class };
    }

    @Before
    public void setUp() {
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                Account account1 = new Account();
                account1.setId(1L);
                account1.setActive(true);
                em.persist(account1);

                Account account2 = new Account();
                account2.setId(2L);
                account2.setActive(false);
                em.persist(account2);

                Account account3 = new Account();
                account3.setId(3L);
                account3.setActive(true);
                em.persist(account3);

                em.flush();
                em.clear();
            }
        });
    }

    @Test
    public void classFilterSupportTest() {
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                List<Account> accounts = cbf.create(em, Account.class).getResultList();
                assertEquals(3, accounts.size());

            }
        });
        
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                em
                        .unwrap(Session.class)
                        .enableFilter("activeAccount")
                        .setParameter("active", true);

                List<Account> accounts = cbf.create(em, Account.class).getResultList();
                assertEquals(2, accounts.size());
            }
        });

    }

    @Entity(name = "Account")
    @FilterDef(name="activeAccount", parameters=@ParamDef(name="active", type="boolean"))
    @Filter(name="activeAccount", condition="active = :active")
    public static class Account {

        @Id
        private Long id;

        private boolean active;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

}
