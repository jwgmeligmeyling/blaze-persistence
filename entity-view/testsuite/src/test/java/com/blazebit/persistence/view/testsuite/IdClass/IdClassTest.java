package com.blazebit.persistence.view.testsuite.IdClass;


import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.entity.IdClassEntity;
import com.blazebit.persistence.testsuite.entity.IdClassEntityId;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.EntityViewSetting;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.testsuite.AbstractEntityViewTest;
import javafx.scene.Parent;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IdClassTest extends AbstractEntityViewTest {
    @EntityView(IdClassEntity.class)
    public interface IdClassEntityView {
        @IdMapping("key1")
        Integer getKey1();
        @IdMapping("key2")
        String getKey2();
    }

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{
                IdClassEntity.class};
    }

    @Override
    public void setUpOnce(){
        cleanDatabase();
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                IdClassEntity e1 = new IdClassEntity(4,"c",4);
                em.persist(e1);
                em.flush();
                IdClassEntity e2 = new IdClassEntity(5,"d",5);
                em.persist(e2);
            }
        });
    }

    @Test
    public void idClassFindTest(){
        build(IdClassEntityView.class);

        EntityViewSetting<IdClassEntityView, CriteriaBuilder<IdClassEntityView>> setting = EntityViewSetting.create(IdClassEntityView.class);
        IdClassEntityId id1 = new IdClassEntityId(4,"c");
        IdClassEntityView ev1 = evm.find(em,setting,id1);
        Assert.assertEquals(Integer.valueOf(4),ev1.getKey1());
        Assert.assertEquals("c",ev1.getKey2());
    }

}
