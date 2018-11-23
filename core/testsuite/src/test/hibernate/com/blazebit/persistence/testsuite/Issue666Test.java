package com.blazebit.persistence.testsuite;

import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.junit.Test;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Tuple;

public class Issue666Test extends AbstractCoreTest {

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] { BasicEntity.class, IdClassEntity.class, NestedIdClassEntity.class };
    }

    @Entity(name = "BasicEntity")
    public static class BasicEntity {
        @Id Long key1;
    }

    @Entity(name = "IdClassEntity")
    @IdClass( IdClassEntity.IdClassEntityId.class )
    public static class IdClassEntity {
        @Id @ManyToOne BasicEntity basicEntity;
        @Id Long key2;

        public static class IdClassEntityId implements Serializable {
            Long basicEntity;
            Long key2;
        }
    }

    @Entity(name = "NestedIdClassEntity")
    @IdClass( NestedIdClassEntity.NestedIdClassEntityId.class )
    public static class NestedIdClassEntity {
        @Id @ManyToOne IdClassEntity idClassEntity;
        @Id Long key3;

        public static class NestedIdClassEntityId implements Serializable {
            IdClassEntity.IdClassEntityId idClassEntity;
            Long key3;
        }
    }

    @Test
    public void metaModelInstantiationWithNestedIdClassAssociationTest() {
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                cbf.create(em, Tuple.class)
                        .from(NestedIdClassEntity.class, "a")
                        .select("a.idClassEntity.basicEntity.key1")
                        .getResultList();
            }
        });
    }

}
