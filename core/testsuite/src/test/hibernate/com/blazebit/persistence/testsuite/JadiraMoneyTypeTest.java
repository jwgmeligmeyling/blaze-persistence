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
import com.blazebit.persistence.testsuite.entity.LongSequenceEntity;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.TypeDef;
import org.jadira.usertype.moneyandcurrency.moneta.PersistentMoneyAmountAndCurrency;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Tuple;
import java.util.List;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class JadiraMoneyTypeTest extends AbstractCoreTest {

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] { LongSequenceEntity.class, MoneyEntity.class, MoneyCte.class };
    }

    @Before
    public void setUp() throws Exception {
        CurrencyUnit eur = Monetary.getCurrency("EUR");
        MonetaryAmount monetaryAmount = Monetary.getDefaultAmountFactory().setCurrency(eur).setNumber(200).create();

        MoneyEntity moneyEntity = new MoneyEntity();
        moneyEntity.setMonetaryAmount(monetaryAmount);
        em.persist(moneyEntity);
    }

    @Test
    public void testQueryNestedProperties() {
        List<Tuple> me = cbf.create(em, Tuple.class).from(MoneyEntity.class, "me")
                .select("me.monetaryAmount.amount")
                .select("me.monetaryAmount.currencyUnit").getResultList();
        System.out.println(me.get(0).get(0) + " " + me.get(0).get(1));
    }

    @Test
    public void testCteBindNestedProperties() {
        List<MoneyCte> me = cbf.create(em, MoneyCte.class)
                .with(MoneyCte.class)
                .from(MoneyEntity.class, "me")
                .bind("id").select("me.id")
                .bind("monetaryAmount.amount")
                .select("me.monetaryAmount.amount")
                .bind("monetaryAmount.currencyUnit")
                .select("me.monetaryAmount.currencyUnit")
                .end()
                .getResultList();
        System.out.println(me.get(0).getMonetaryAmount().toString());
    }

    @Test
    public void testCteBindCompoundProperties() {
        List<MoneyCte> me = cbf.create(em, MoneyCte.class)
                .with(MoneyCte.class)
                .from(MoneyEntity.class, "me")
                .bind("id").select("me.id")
                .bind("monetaryAmount")
                .select("me.monetaryAmount")
                .end()
                .getResultList();
        System.out.println(me.get(0).getMonetaryAmount().toString());
    }

    @Entity(name = "MoneyEntity")
    @TypeDef(name = "money", typeClass = PersistentMoneyAmountAndCurrency.class, defaultForType = MonetaryAmount.class)
    public static class MoneyEntity extends LongSequenceEntity {

        private MonetaryAmount monetaryAmount;

        @Columns(columns = {
                @Column(name = "currency"),
                @Column(name = "amount"),
        })
        public MonetaryAmount getMonetaryAmount() {
            return monetaryAmount;
        }

        public void setMonetaryAmount(MonetaryAmount monetaryAmount) {
            this.monetaryAmount = monetaryAmount;
        }
    }

    @CTE
    @Entity(name = "MoneyCte")
    @TypeDef(name = "money", typeClass = PersistentMoneyAmountAndCurrency.class, defaultForType = MonetaryAmount.class)
    public static class MoneyCte extends LongSequenceEntity {

        private MonetaryAmount monetaryAmount;

        @Columns(columns = {
                @Column(name = "currency"),
                @Column(name = "amount"),
        })
        public MonetaryAmount getMonetaryAmount() {
            return monetaryAmount;
        }

        public void setMonetaryAmount(MonetaryAmount monetaryAmount) {
            this.monetaryAmount = monetaryAmount;
        }
    }
}
