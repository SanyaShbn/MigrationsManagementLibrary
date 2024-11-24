package exception;

/** *
 * This custom exception is used for eliminating the possibility of different users
 * running migrations simultaneously.
 * */
public class LockException extends RuntimeException {
    public LockException(String message) {
        super(message);
    }
}

