package org.javers.model.mapping;

import org.javers.common.collections.Lists;
import org.javers.common.collections.Predicate;
import org.javers.common.validation.Validate;
import org.javers.core.exceptions.JaversException;
import org.javers.core.exceptions.JaversExceptionCode;
import org.javers.model.mapping.type.MapType;
import org.javers.model.mapping.type.TypeMapper;

import java.util.List;

/**
 * @author bartosz walacik
 */
public class ManagedClassFactory {
    private TypeMapper typeMapper;
    private PropertyScanner propertyScanner;

    public ManagedClassFactory(PropertyScanner propertyScanner, TypeMapper typeMapper) {
        Validate.argumentIsNotNull(propertyScanner);
        this.propertyScanner = propertyScanner;
        this.typeMapper = typeMapper;
    }

    @Deprecated
    public <S> Entity create(Class<S> clazz) {
       return create(new EntityDefinition(clazz));
    }

    public Entity create(EntityDefinition entityDefinition) {

        List<Property> properties = propertyScanner.scan(entityDefinition.getClazz());

        Property idProperty = null;
        if (entityDefinition.hasCustomId()){
            idProperty = findIdPropertyByName(properties, entityDefinition);
        }

        return new Entity(entityDefinition.getClazz(), onlySupported(properties), idProperty);
    }

    private Property findIdPropertyByName(List<Property> beanProperties, EntityDefinition entityDefinition) {
        for (Property property : beanProperties)  {
            if (property.getName().equals( entityDefinition.getIdPropertyName() ) ) {
                return property;
            }
        }
        throw new JaversException(JaversExceptionCode.PROPERTY_NOT_FOUND,entityDefinition.getIdPropertyName(),entityDefinition.getClazz().getName());
    }

    public ValueObject create(ValueObjectDefinition valueObjectDefinition) {
        List<Property> properties = propertyScanner.scan(valueObjectDefinition.getClazz());
        return new ValueObject(valueObjectDefinition.getClazz(), onlySupported(properties));
    }

    private List<Property> onlySupported(List<Property> properties){
        Predicate<Property> notSupported = new Predicate<Property>() {
            @Override
            public boolean apply(Property property) {
                if (property.getType() instanceof MapType) {
                   MapType mapType = (MapType)property.getType();

                   return ! typeMapper.isPrimitiveOrValue(mapType.getEntryClass());

                }
                return false;
            }
        };

        return Lists.negativeFilter(properties, notSupported);
    }


}
