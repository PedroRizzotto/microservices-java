package br.edu.atitus.orderapi.servicesimpl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {

    private static final int CUSTOMER = 1;
    private static final int PROVIDER = 2;

    public void requireCustomer(Integer userType) {
        require(userType, CUSTOMER);
    }

    public void requireProvider(Integer userType) {
        require(userType, PROVIDER);
    }

    private void require(Integer userType, int expected) {
        if (userType == null || userType != expected)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");
    }
}
