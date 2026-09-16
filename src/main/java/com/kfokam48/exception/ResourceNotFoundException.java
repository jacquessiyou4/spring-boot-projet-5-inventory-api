package com.kfokam48.exception;

/**
 * Levée lorsqu'un produit ou une alerte demandé n'existe pas → HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
