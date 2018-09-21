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

public class IdClassWithAssociationTest extends AbstractEntityViewTest {

    @Entity
    @Table(name = "dependent_id_class_entity")
    @IdClass(DependentIdClassEntityId.class)
    public static class DependentIdClassEntity implements Serializable {
        private static final long serialVersionUID = 1L;

        private ParentIdClassEntity parentIdClassEntity;
        private String key2;
        private Integer value;
//        private Set<DependentIdClassEntity> children = new HashSet<>();

        public DependentIdClassEntity() {
        }

        public DependentIdClassEntity(ParentIdClassEntity parentIdClassEntity, String key2, Integer value) {
            this.parentIdClassEntity = parentIdClassEntity;
            this.key2 = key2;
            this.value = value;
        }

        @Id
        @ManyToOne
        @JoinColumn(name = "parentIdClassEntity", nullable = false)
        public ParentIdClassEntity getParentIdClassEntity() {
            return parentIdClassEntity;
        }

        public void setParentIdClassEntity(ParentIdClassEntity parentIdClassEntity) {
            this.parentIdClassEntity = parentIdClassEntity;
        }

        @Id
        @Column(name = "key2", nullable = false, length = 40)
        public String getKey2() {
            return key2;
        }

        public void setKey2(String key2) {
            this.key2 = key2;
        }

        @Basic(optional = false)
        @Column(name = "value", nullable = false)
        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

//        @ManyToMany
//        @JoinTable(name = "dependent_id_class_entity_children", joinColumns = {
//                @JoinColumn(name = "child_parentIdClassEntity", nullable = false, referencedColumnName = "parentIdClassEntity"),
//                @JoinColumn(name = "child_key2", nullable = false, referencedColumnName = "key2")
//        }, inverseJoinColumns = {
//                @JoinColumn(name = "parent_parentIdClassEntity", nullable = false, referencedColumnName = "parentIdClassEntity"),
//                @JoinColumn(name = "parent_key2", nullable = false, referencedColumnName = "key2")
//        })
//        public Set<DependentIdClassEntity> getChildren() {
//            return children;
//        }
//
//        public void setChildren(Set<DependentIdClassEntity> children) {
//            this.children = children;
//        }
    }

    public static class DependentIdClassEntityId implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer parentIdClassEntity;
        private String key2;

        public DependentIdClassEntityId() {
        }

        public DependentIdClassEntityId(Integer parentIdClassEntity, String key2) {
            this.parentIdClassEntity = parentIdClassEntity;
            this.key2 = key2;
        }

        public Integer getParentIdClassEntity() {
            return parentIdClassEntity;
        }

        public void setParentIdClassEntity(Integer parentIdClassEntity) {
            this.parentIdClassEntity = parentIdClassEntity;
        }

        public String getKey2() {
            return key2;
        }

        public void setKey2(String key2) {
            this.key2 = key2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DependentIdClassEntityId that = (DependentIdClassEntityId) o;

            if (parentIdClassEntity != null ? !parentIdClassEntity.equals(that.parentIdClassEntity) : that.parentIdClassEntity != null) {
                return false;
            }
            return key2 != null ? key2.equals(that.key2) : that.key2 == null;
        }

        @Override
        public int hashCode() {
            int result = parentIdClassEntity != null ? parentIdClassEntity.hashCode() : 0;
            result = 31 * result + (key2 != null ? key2.hashCode() : 0);
            return result;
        }
    }

    @Entity
    @Table(name = "parent_id_class_entity")
    public static class ParentIdClassEntity implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer parentIdClassEntityId;
        private Integer value;
//        private Set<ParentIdClassEntity> children = new HashSet<>();

        public ParentIdClassEntity() {
        }

        public ParentIdClassEntity(Integer parentIdClassEntityId, Integer value) {
            this.parentIdClassEntityId = parentIdClassEntityId;
            this.value = value;
        }

        @Id
        @Column(name = "parentIdClassEntityId", nullable = false)
        public Integer getParentIdClassEntityId() {
            return parentIdClassEntityId;
        }

        public void setParentIdClassEntityId(Integer parentIdClassEntityId) {
            this.parentIdClassEntityId = parentIdClassEntityId;
        }

        @Basic(optional = false)
        @Column(name = "value", nullable = false)
        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

//        @ManyToMany
//        @JoinTable(name = "parent_id_class_entity_children", joinColumns = {
//                @JoinColumn(name = "child_parentIdClassEntityId", nullable = false, referencedColumnName = "parentIdClassEntityId"),
//        }, inverseJoinColumns = {
//                @JoinColumn(name = "parent_parentIdClassEntityId", nullable = false, referencedColumnName = "parentIdClassEntityId"),
//        })
//        public Set<ParentIdClassEntity> getChildren() {
//            return children;
//        }
//
//        public void setChildren(Set<ParentIdClassEntity> children) {
//            this.children = children;
//        }
    }

    @EntityView(DependentIdClassEntity.class)
    public interface DependentIdClassEntityView {
        @IdMapping("parentIdClassEntity")
            //Should I use an Integer as specified by the DependentIdClassEntityId, or should I use the ParentIdClassEntity? Only time will tell.
        ParentIdClassEntity getParentIdClassEntity();
        @IdMapping("key2")
        String getKey2();
    }

    @EntityView(ParentIdClassEntity.class)
    public interface ParentIdClassEntityView {
        @IdMapping("parentIdClassEntityId")
        Integer getParentIdClassEntityId();
    }

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{
                IdClassEntity.class,
                ParentIdClassEntity.class,
                DependentIdClassEntity.class};
    }

    @Override
    public void setUpOnce(){
        cleanDatabase();
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                ParentIdClassEntity p1 = new ParentIdClassEntity(1,11);
                em.persist(p1);
                ParentIdClassEntity p2 = new ParentIdClassEntity(2,12);
                em.persist(p2);
                DependentIdClassEntity d1 = new DependentIdClassEntity(p1,"a",21);
                em.persist(d1);
                DependentIdClassEntity d2 = new DependentIdClassEntity(p2,"b",22);
                em.persist(d2);
                em.flush();
            }
        });
    }

    @Test
    public void idClassWithAssociationIdTest(){
        build(DependentIdClassEntityView.class, ParentIdClassEntityView.class);

        EntityViewSetting<DependentIdClassEntityView, CriteriaBuilder<DependentIdClassEntityView>> setting = EntityViewSetting.create(DependentIdClassEntityView.class);
        DependentIdClassEntityId id1 = new DependentIdClassEntityId(1,"a");
        DependentIdClassEntityId id2 = new DependentIdClassEntityId(2,"b");

        DependentIdClassEntityView dependentIdClassEntityView1 = evm.find(em,setting,id1);
        DependentIdClassEntityView dependentIdClassEntityView2 = evm.find(em,setting,id2);

        Assert.assertEquals(Integer.valueOf(11),dependentIdClassEntityView1.getParentIdClassEntity().getValue());
        Assert.assertEquals("a",dependentIdClassEntityView1.getKey2());
        Assert.assertEquals(Integer.valueOf(12),dependentIdClassEntityView2.getParentIdClassEntity().getValue());
        Assert.assertEquals("b",dependentIdClassEntityView2.getKey2());
    }
}
