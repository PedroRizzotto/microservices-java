package br.edu.atitus.authapi.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.edu.atitus.authapi.entities.UserType;

class UserTypeConverterTest {
    private final UserTypeConverter converter = new UserTypeConverter();

    @Test
    void gravaCodigoFixo() {
        assertEquals(1, converter.convertToDatabaseColumn(UserType.Common));
        assertEquals(2, converter.convertToDatabaseColumn(UserType.Provider));
    }

    @Test
    void leCodigoFixo() {
        assertEquals(UserType.Common, converter.convertToEntityAttribute(1));
        assertEquals(UserType.Provider, converter.convertToEntityAttribute(2));
    }

    @Test
    void recusaCodigoDeAdmin() {
        assertThrows(IllegalArgumentException.class, () -> UserType.fromCode(0));
    }

    @Test
    void nuloViraNulo() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
