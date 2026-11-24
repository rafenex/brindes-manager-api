package br.com.rafael.brindesmanager.exception;

public class PostalCodeIntegrationException extends RuntimeException {
    private final boolean timeout;

    public PostalCodeIntegrationException(boolean timeout) {
        super(timeout ? "Tempo de consulta do CEP excedido" : "Consulta de CEP indisponível");
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
