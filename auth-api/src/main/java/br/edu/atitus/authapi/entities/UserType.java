package br.edu.atitus.authapi.entities;

public enum UserType {
    Common(1),
    Provider(2);

    private final int code;

    UserType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserType fromCode(int code) {
        for (UserType type : values()) {
            if (type.code == code)
                return type;
        }
        throw new IllegalArgumentException("Codigo de tipo de usuario desconhecido: " + code);
    }
}
