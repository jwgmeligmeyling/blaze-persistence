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

    @Entity
    @Table(name = "deeply_dependent_id_class_entity")
    @IdClass(DeeplyDependentIdClassEntityId.class)
    public static class DeeplyDependentIdClassEntity implements Serializable {
        private static final long serialVersionUID = 1L;

        private DependentIdClassEntity dependentIdClassEntity;
        private String key3;
        private Integer value;
//        private Set<DependentIdClassEntity> children = new HashSet<>();

        public DeeplyDependentIdClassEntity() {
        }

        public DeeplyDependentIdClassEntity(DependentIdClassEntity dependentIdClassEntity, String key3, Integer value) {
            this.dependentIdClassEntity = dependentIdClassEntity;
            this.key3 = key3;
            this.value = value;
        }

        @Id
        @ManyToOne
        @JoinColumns({
                @JoinColumn(name = "parentIdClassEntity", nullable = false),
                @JoinColumn(name = "key2", nullable = false)})
        public DependentIdClassEntity getDependentIdClassEntity() {
            return dependentIdClassEntity;
        }

        public void setDependentIdClassEntity(DependentIdClassEntity dependentIdClassEntity) {
            this.dependentIdClassEntity = dependentIdClassEntity;
        }

        @Id
        @Column(name = "key3", nullable = false, length = 40)
        public String getKey3() {
            return key3;
        }

        public void setKey3(String key3) {
            this.key3 = key3;
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

    public static class DeeplyDependentIdClassEntityId implements Serializable {
        private static final long serialVersionUID = 1L;

        private DependentIdClassEntityId dependentIdClassEntity;
        private String key3;

        public DeeplyDependentIdClassEntityId() {
        }

        public DeeplyDependentIdClassEntityId(DependentIdClassEntityId dependentIdClassEntity, String key3) {
            this.dependentIdClassEntity = dependentIdClassEntity;
            this.key3 = key3;
        }

        public DependentIdClassEntityId getDependentIdClassEntity() {
            return dependentIdClassEntity;
        }

        public void setDependentIdClassEntity(DependentIdClassEntityId dependentIdClassEntity) {
            this.dependentIdClassEntity = dependentIdClassEntity;
        }

        public String getKey3() {
            return key3;
        }

        public void setKey3(String key3) {
            this.key3 = key3;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DeeplyDependentIdClassEntityId that = (DeeplyDependentIdClassEntityId) o;

            if (dependentIdClassEntity != null ? !dependentIdClassEntity.equals(that.dependentIdClassEntity) : that.dependentIdClassEntity != null) {
                return false;
            }
            return key3 != null ? key3.equals(that.key3) : that.key3 == null;
        }

        @Override
        public int hashCode() {
            int result = dependentIdClassEntity != null ? dependentIdClassEntity.hashCode() : 0;
            result = 31 * result + (key3 != null ? key3.hashCode() : 0);
            return result;
        }
    }

    @EntityView(DeeplyDependentIdClassEntity.class)
    public interface DeeplyDependentIdClassEntityView {
        @IdMapping("dependentIdClassEntity")
        DependentIdClassEntity getDependentIdClassEntity();
        @IdMapping("key3")
        String getKey3();
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
                DependentIdClassEntity.class,
                DeeplyDependentIdClassEntity.class};
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
                DeeplyDependentIdClassEntity dd1 = new DeeplyDependentIdClassEntity(d1,"alpha",31);
                em.persist(dd1);
                DeeplyDependentIdClassEntity dd2 = new DeeplyDependentIdClassEntity(d1,"beta",32);
                em.persist(dd2);
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

    @Test
    public void idClassWithDeepAssociationIdTest(){
        build(DeeplyDependentIdClassEntityView.class, DependentIdClassEntityView.class, ParentIdClassEntityView.class);

        EntityViewSetting<DeeplyDependentIdClassEntityView, CriteriaBuilder<DeeplyDependentIdClassEntityView>> setting = EntityViewSetting.create(DeeplyDependentIdClassEntityView.class);
        DependentIdClassEntityId id1 = new DependentIdClassEntityId(1,"a");
        DependentIdClassEntityId id2 = new DependentIdClassEntityId(2,"b");
        DeeplyDependentIdClassEntityId idd1 = new DeeplyDependentIdClassEntityId(id1,"alpha");
        DeeplyDependentIdClassEntityId idd2 = new DeeplyDependentIdClassEntityId(id2,"beta");

        DeeplyDependentIdClassEntityView deeplyDependentIdClassEntityView1 = evm.find(em,setting,idd1);
        DeeplyDependentIdClassEntityView deeplyDependentIdClassEntityView2 = evm.find(em,setting,idd2);

        Assert.assertEquals(Integer.valueOf(21),deeplyDependentIdClassEntityView1.getDependentIdClassEntity().getValue());
        Assert.assertEquals("alpha",deeplyDependentIdClassEntityView1.getKey3());
        Assert.assertEquals(Integer.valueOf(22),deeplyDependentIdClassEntityView2.getDependentIdClassEntity().getValue());
        Assert.assertEquals("beta",deeplyDependentIdClassEntityView2.getKey3());
    }
}
