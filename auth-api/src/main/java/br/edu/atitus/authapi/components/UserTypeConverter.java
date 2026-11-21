package br.edu.atitus.authapi.components;

import br.edu.atitus.authapi.entities.UserType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserType type) {
        return type == null ? null : type.getCode();
    }

    @Override
    public UserType convertToEntityAttribute(Integer code) {
        return code == null ? null : UserType.fromCode(code);
    }
}
