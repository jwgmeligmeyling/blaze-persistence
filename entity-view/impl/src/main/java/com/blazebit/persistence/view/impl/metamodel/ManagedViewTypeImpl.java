/*
 * Copyright 2014 Blazebit.
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
package com.blazebit.persistence.view.impl.metamodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.persistence.impl.expression.ExpressionFactory;
import com.blazebit.persistence.impl.expression.SyntaxErrorException;
import com.blazebit.persistence.view.MappingParameter;
import com.blazebit.persistence.view.MappingSingular;
import com.blazebit.persistence.view.MappingSubquery;
import com.blazebit.persistence.view.impl.CollectionJoinMappingGathererExpressionVisitor;
import com.blazebit.persistence.view.impl.MetamodelTargetResolvingExpressionVisitor;
import com.blazebit.persistence.view.impl.MetamodelTargetResolvingExpressionVisitor.TargetType;
import com.blazebit.persistence.view.impl.UpdatableExpressionVisitor;
import com.blazebit.persistence.view.metamodel.AttributeFilterMapping;
import com.blazebit.persistence.view.metamodel.FilterMapping;
import com.blazebit.persistence.view.metamodel.ManagedViewType;
import com.blazebit.persistence.view.metamodel.MappingConstructor;
import com.blazebit.persistence.view.metamodel.MethodAttribute;
import com.blazebit.persistence.view.metamodel.PluralAttribute;
import com.blazebit.reflection.ReflectionUtils;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
public abstract class ManagedViewTypeImpl<X> implements ManagedViewType<X> {

    protected final Class<X> javaType;
    protected final Class<?> entityClass;
    protected final Map<String, AbstractMethodAttribute<? super X, ?>> attributes;
    protected final Map<ParametersKey, MappingConstructorImpl<X>> constructors;
    protected final Map<String, MappingConstructor<X>> constructorIndex;
    protected final Map<String, AttributeFilterMapping> attributeFilters;

    @SuppressWarnings("unchecked")
    public ManagedViewTypeImpl(Class<? extends X> clazz, Class<?> entityClass, Set<Class<?>> entityViews) {
        this.javaType = (Class<X>) clazz;
        this.entityClass = entityClass;

        if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Only interfaces or abstract classes are allowed as entity views. '" + clazz.getName() + "' is neither of those.");
        }
        
        // We use a tree map to get a deterministic attribute order
        this.attributes = new TreeMap<String, AbstractMethodAttribute<? super X, ?>>();
        this.attributeFilters = new HashMap<String, AttributeFilterMapping>();
        
        for (Method method : clazz.getMethods()) {
            String attributeName = AbstractMethodAttribute.validate(this, method);

            if (attributeName != null && !attributes.containsKey(attributeName)) {
                AbstractMethodAttribute<? super X, ?> attribute = createMethodAttribute(this, method, entityViews);
                if (attribute != null) {
                    attributes.put(attribute.getName(), attribute);
                    addAttributeFilters(attribute);
                }
            }
        }
        
        this.constructors = new HashMap<ParametersKey, MappingConstructorImpl<X>>();
        this.constructorIndex = new HashMap<String, MappingConstructor<X>>();

        // TODO: This is probably not deterministic since the constructor order is not defined
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            String constructorName = MappingConstructorImpl.validate(this, constructor);
            if (constructorIndex.containsKey(constructorName)) {
                constructorName += constructorIndex.size();
            }
            MappingConstructorImpl<X> mappingConstructor = new MappingConstructorImpl<X>(this, constructorName, (Constructor<X>) constructor, entityViews);
            constructors.put(new ParametersKey(constructor.getParameterTypes()), mappingConstructor);
            constructorIndex.put(constructorName, mappingConstructor);
        }
    }
    
    public void checkAttributes(Map<Class<?>, ManagedViewType<?>> managedViews, ExpressionFactory expressionFactory, Metamodel metamodel, Set<String> errors) {
        ManagedType<?> managedType = metamodel.managedType(entityClass);
        Map<String, List<String>> collectionMappings = new HashMap<String, List<String>>();
        
        for (AbstractMethodAttribute<? super X, ?> attribute : attributes.values()) {
            String error = checkAttribute(attribute, managedType, managedViews, expressionFactory, metamodel);
            
            if (error != null) {
                errors.add(error);
            }
            
            for (String mapping : getCollectionJoinMappings(attribute, managedType, metamodel, expressionFactory)) {
            	List<String> locations = collectionMappings.get(mapping);
            	if (locations == null) {
            		locations = new ArrayList<String>(2);
            		collectionMappings.put(mapping, locations);
            	}
            	
            	locations.add("Attribute '" + attribute.getName() + "' in entity view '" + javaType.getName() + "'");
            }
        }
        
        if (!constructors.isEmpty()) {
	        for (MappingConstructorImpl<X> constructor : constructors.values()) {
	            Map<String, List<String>> constructorCollectionMappings = new HashMap<String, List<String>>();
	        	
	            for (Map.Entry<String, List<String>> entry : collectionMappings.entrySet()) {
	            	constructorCollectionMappings.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
	            }
	        	
	            constructor.checkParameters(managedType, managedViews, expressionFactory, metamodel, constructorCollectionMappings, errors);
	
	    		StringBuilder sb = new StringBuilder();
	            for (Map.Entry<String, List<String>> locationsEntry : constructorCollectionMappings.entrySet()) {
	            	List<String> locations = locationsEntry.getValue();
	            	if (locations.size() > 1) {
	            		sb.setLength(0);
	            		sb.append("Multiple usages of the mapping '" + locationsEntry.getKey() + "' in");
	            		
	            		for (String location : locations) {
	            			sb.append("\n - ");
	            			sb.append(location);
	            		}
	            		errors.add(sb.toString());
	            	}
	            }
	        }
        } else {
    		StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> locationsEntry : collectionMappings.entrySet()) {
            	List<String> locations = locationsEntry.getValue();
            	if (locations.size() > 1) {
            		sb.setLength(0);
            		sb.append("Multiple usages of the mapping '" + locationsEntry.getKey() + "' in");
            		
            		for (String location : locations) {
            			sb.append("\n - ");
            			sb.append(location);
            		}
            		errors.add(sb.toString());
            	}
            }
        }
    }
    
    private Set<String> getCollectionJoinMappings(AbstractAttribute<? super X, ?> attribute, ManagedType<?> managedType, Metamodel metamodel, ExpressionFactory expressionFactory) {
    	String expression = attribute.getMapping();
        
        if (expression == null || attribute.isQueryParameter()) {
            // Subqueries and parameters can't be checked
            return Collections.emptySet();
        }
        
    	CollectionJoinMappingGathererExpressionVisitor visitor = new CollectionJoinMappingGathererExpressionVisitor(managedType, metamodel);
        expressionFactory.createSimpleExpression(expression).accept(visitor);
        Set<String> mappings = new HashSet<String>();
        
        for (String s : visitor.getPaths()) {
        	mappings.add(s);
        }
        
        return mappings;
    }

    private String checkAttribute(AbstractMethodAttribute<? super X, ?> attribute, ManagedType<?> managedType, Map<Class<?>, ManagedViewType<?>> managedViews, ExpressionFactory expressionFactory, Metamodel metamodel) {
        String expression = attribute.getMapping();
        
        if (expression == null || attribute.isQueryParameter()) {
            // Subqueries and parameters can't be checked
            return null;
        }
        
        if (attribute.isUpdatable()) {
            UpdatableExpressionVisitor visitor = new UpdatableExpressionVisitor(entityClass);
            try {
                expressionFactory.createPathExpression(expression).accept(visitor);
                Map<Method, Class<?>[]> possibleTargets = visitor.getPossibleTargets();
                
                if (possibleTargets.size() > 1) {
                    return "Multiple possible target type for the mapping in the attribute '" + attribute.getName() + "' in entity view '" + javaType.getName() + "': " + possibleTargets;
                } else {
                    // TODO: further type checks like
                    // * collection type is same
                    // * collection value type is compatible
                }
            } catch (SyntaxErrorException ex) {
                return "Syntax error in mapping expression '" + expression + "' of attribute '" + attribute.getName() + "' of the managed entity view class '" + attribute.getDeclaringType().getJavaType().getName() + "': " + ex.getMessage();
            } catch (IllegalArgumentException ex) {
                return "There is an error for the attribute '" + attribute.getName() + "' in entity view '" + javaType.getName() + "': " + ex.getMessage();
            }
        }
        
        Class<?> expressionType = attribute.getJavaType();
        Class<?> elementType = null;
        
        if (attribute.isCollection()) {
            elementType = ((PluralAttribute<?, ?, ?>) attribute).getElementType();
        }
        
        // TODO: check if collection value types are compatible => subview is view for entity class, or class is super type of entity class
        
        // Updatable collection attributes must have the same collection type
        if (!attribute.isUpdatable() && attribute.isCollection() && !((PluralAttribute<?, ?, ?>) attribute).isIndexed() && Collection.class.isAssignableFrom(expressionType)) {
            // We can assign e.g. a Set to a List, so let's use the common supertype
            expressionType = Collection.class;
        } else if (!attribute.isCollection() && attribute.isSubview()) {
            ManagedViewType<?> subviewType = managedViews.get(expressionType);
            
            if (subviewType == null) {
                throw new IllegalStateException("Expected subview '" + expressionType.getName() + "' to exist but couldn't find it!");
            }
            
            expressionType = subviewType.getEntityClass();
        }

        MetamodelTargetResolvingExpressionVisitor visitor = new MetamodelTargetResolvingExpressionVisitor(managedType, metamodel);
        
        try {
            expressionFactory.createSimpleExpression(expression).accept(visitor);
        } catch (SyntaxErrorException ex) {
            return "Syntax error in mapping expression '" + expression + "' of attribute '" + attribute.getName() + "' of the managed entity view class '" + attribute.getDeclaringType().getJavaType().getName() + "': " + ex.getMessage();
        } catch (IllegalArgumentException ex) {
            return "An error occurred while trying to resolve the attribute '" + attribute.getName() + "' of the managed entity view class '" + attribute.getDeclaringType().getJavaType().getName() + "': " + ex.getMessage();
        }
        
        List<TargetType> possibleTargets = visitor.getPossibleTargets();
        
        if (!possibleTargets.isEmpty()) {
            boolean error = true;
            for (TargetType t : possibleTargets) {
                Class<?> possibleTargetType = t.getLeafBaseClass();
                
                // Null is the marker for ANY TYPE
                if (possibleTargetType == null || expressionType.isAssignableFrom(possibleTargetType)
                    || Map.class.isAssignableFrom(possibleTargetType) && expressionType.isAssignableFrom(t.getLeafBaseValueClass())) {
                    error = false;
                    break;
                } else if (t.hasCollectionJoin() && elementType != null && elementType.isAssignableFrom(t.getLeafBaseValueClass())) {
                	error = false;
                    break;
                }
            }
            
            if (error) {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (TargetType t : possibleTargets) {
                    sb.append(t.getLeafBaseClass().getName());
                    sb.append(", ");
                }
                
                sb.setLength(sb.length() - 2);
                sb.append(']');
                return "The resolved possible types " + sb.toString() + " are not assignable to the given expression type '" + attribute.getJavaType().getName() + "' of the expression declared by the attribute '" + attribute.getName() + "' of the managed entity view class '" + attribute.getDeclaringType().getJavaType().getName() + "'!";
            }
        }
        
        return null;
    }

    private void addAttributeFilters(AbstractMethodAttribute<? super X, ?> attribute) {
        for (Map.Entry<String, AttributeFilterMapping> entry : attribute.getFilterMappings().entrySet()) {
            String filterName = entry.getKey();
            AttributeFilterMapping filterMapping = entry.getValue();
            
            if (attributeFilters.containsKey(filterName)) {
                attributeFilters.get(filterName);
                throw new IllegalArgumentException("Illegal duplicate filter name mapping '" + filterName + "' at attribute '" + filterMapping.getDeclaringAttribute().getName() 
                    + "' of the class '" + javaType.getName() + "'! Already defined on attribute class '" + javaType.getName() + "'!");
            }
            
            attributeFilters.put(filterName, filterMapping);
        }
    }

    // If you change something here don't forget to also update MappingConstructorImpl#createMethodAttribute
    private static <X> AbstractMethodAttribute<? super X, ?> createMethodAttribute(ManagedViewType<X> viewType, Method method, Set<Class<?>> entityViews) {
        Annotation mapping = AbstractMethodAttribute.getMapping(viewType, method);
        if (mapping == null) {
            return null;
        }

        Class<?> attributeType = ReflectionUtils.getResolvedMethodReturnType(viewType.getJavaType(), method);
        
        // Force singular mapping
        if (AnnotationUtils.findAnnotation(method, MappingSingular.class) != null || mapping instanceof MappingParameter) {
            return new MethodMappingSingularAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        }

        if (Collection.class == attributeType) {
            return new MethodMappingCollectionAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        } else if (List.class == attributeType) {
            return new MethodMappingListAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        } else if (Set.class == attributeType || SortedSet.class == attributeType || NavigableSet.class == attributeType) {
            return new MethodMappingSetAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        } else if (Map.class == attributeType || SortedMap.class == attributeType || NavigableMap.class == attributeType) {
            return new MethodMappingMapAttributeImpl<X, Object, Object>(viewType, method, mapping, entityViews);
        } else if (mapping instanceof MappingSubquery) {
            return new MethodSubquerySingularAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        } else {
            return new MethodMappingSingularAttributeImpl<X, Object>(viewType, method, mapping, entityViews);
        }
    }

	@Override
    public Class<X> getJavaType() {
        return javaType;
    }

    @Override
    public Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public Set<MethodAttribute<? super X, ?>> getAttributes() {
        return new LinkedHashSet<MethodAttribute<? super X, ?>>(attributes.values());
    }

    @Override
    public MethodAttribute<? super X, ?> getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public FilterMapping<?> getFilter(String filterName) {
        return attributeFilters.get(filterName);
    }

    @Override
    public Set<FilterMapping<?>> getFilters() {
        return new HashSet<FilterMapping<?>>(attributeFilters.size());
    }

    @Override
    public AttributeFilterMapping getAttributeFilter(String filterName) {
        return attributeFilters.get(filterName);
    }

    @Override
    public Set<AttributeFilterMapping> getAttributeFilters() {
        return new HashSet<AttributeFilterMapping>(attributeFilters.values());
    }

    @Override
    public Set<MappingConstructor<X>> getConstructors() {
        return new HashSet<MappingConstructor<X>>(constructors.values());
    }

    @Override
    public MappingConstructor<X> getConstructor(Class<?>... parameterTypes) {
        return constructors.get(new ParametersKey(parameterTypes));
    }

    @Override
    public Set<String> getConstructorNames() {
        return constructorIndex.keySet();
    }

    @Override
    public MappingConstructor<X> getConstructor(String name) {
        return constructorIndex.get(name);
    }

    private static class ParametersKey {

        private final Class<?>[] parameterTypes;

        public ParametersKey(Class<?>[] parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Arrays.deepHashCode(this.parameterTypes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ParametersKey other = (ParametersKey) obj;
            if (!Arrays.deepEquals(this.parameterTypes, other.parameterTypes)) {
                return false;
            }
            return true;
        }
    }

}
