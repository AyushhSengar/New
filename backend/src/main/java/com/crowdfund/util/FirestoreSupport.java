package com.crowdfund.util;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;

/** Small helpers so services never swallow Firestore errors silently. */
public final class FirestoreSupport {

    private static final Logger log = LoggerFactory.getLogger(FirestoreSupport.class);

    private FirestoreSupport() {
    }

    /** Runs an atomic Firestore transaction; AppExceptions thrown inside are passed through. */
    public static <T> T transaction(Firestore db, Transaction.Function<T> work) {

        try {
            return db.runTransaction(work).get();

        } catch (ExecutionException e) {
            throw translate(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Request interrupted. Please try again.");
        }
    }

    public static <T> T await(ApiFuture<T> future) {

        try {
            return future.get();

        } catch (ExecutionException e) {
            throw translate(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Request interrupted. Please try again.");
        }
    }

    private static RuntimeException translate(ExecutionException e) {

        Throwable cause = e;

        while (cause != null) {

            if (cause instanceof AppException appException) {
                return appException;
            }

            cause = cause.getCause();
        }

        log.error("Firestore operation failed", e);

        return new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                "The database is temporarily unavailable. Please try again.");
    }
}
