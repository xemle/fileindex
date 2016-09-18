package de.silef.service.file.index;

/**
 * Created by sebastian on 18.09.16.
 */
public class IndexUpdateException extends RuntimeException {
    public IndexUpdateException(String message, Throwable exception) {
        super(message, exception);
    }
}
