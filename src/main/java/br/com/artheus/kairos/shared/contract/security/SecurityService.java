package br.com.artheus.kairos.shared.contract.security;

public interface SecurityService {
    String getCurrentUserId();

    boolean isAdmin();
}