package org.javers.model.mapping;

import org.javers.common.reflection.ReflectionUtil;
import org.javers.model.mapping.type.JaversType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.Id;
import java.lang.reflect.Field;

import static org.javers.common.validation.Validate.argumentIsNotNull;

/**
 * Immutable
 *
 * @author Pawel Cierpiatka <pawel.cierpiatka@gmail.com>
 */
public class FieldProperty implements Property {

    private final Field field;
    private final JaversType javersType;

    public FieldProperty(Field field, JaversType javersType) {

        argumentIsNotNull(field, "field should not be null!");
        argumentIsNotNull(javersType, "javersType should not be null!");

        this.field = field;
        this.javersType = javersType;
    }

    @Override
    public boolean isId() {
        return field.isAnnotationPresent(Id.class);
    }

    @Override
    public Object get(Object target) {
        return ReflectionUtil.invokeFieldEvenIfPrivate(field, target);
    }

    @Override
    public boolean isNull(Object target) {
        return get(target) == null;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public JaversType getType() {
        return javersType;
    }

    @Override
    public void setValue(Object value) {
        throw new NotImplementedException();
    }
}