package app.crushlog.cldf.clid;

/**
 * Exception thrown when CLID generation fails due to system issues. This is typically thrown when
 * required cryptographic algorithms are not available.
 */
public class CLIDGenerationException extends RuntimeException {

  /**
   * Constructs a new CLID generation exception with the specified detail message.
   *
   * @param message the detail message
   */
  public CLIDGenerationException(String message) {
    super(message);
  }

  /**
   * Constructs a new CLID generation exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public CLIDGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
