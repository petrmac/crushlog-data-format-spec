package app.crushlog.cldf.qr.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A functional result type that represents either a successful value or an error. This provides a
 * type-safe way to handle operations that may fail without throwing exceptions.
 *
 * @param <T> The type of the success value
 * @param <E> The type of the error value
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

  /** Create a successful result. */
  static <T, E> Result<T, E> success(T value) {
    return new Success<>(value);
  }

  /** Create a failed result. */
  static <T, E> Result<T, E> failure(E error) {
    return new Failure<>(error);
  }

  /** Execute a function that may throw and wrap the result. */
  static <T> Result<T, String> tryExecute(Supplier<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Exception e) {
      return failure(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }
  }

  /** Execute a function that may throw and wrap the result with a custom error mapper. */
  static <T, E> Result<T, E> tryExecute(Supplier<T> supplier, Function<Exception, E> errorMapper) {
    try {
      return success(supplier.get());
    } catch (Exception e) {
      return failure(errorMapper.apply(e));
    }
  }

  /** Check if this is a successful result. */
  boolean isSuccess();

  /** Check if this is a failed result. */
  boolean isFailure();

  /** Get the success value if present. */
  Optional<T> getSuccess();

  /** Get the error value if present. */
  Optional<E> getError();

  /** Map the success value to a new value. */
  <U> Result<U, E> map(Function<T, U> mapper);

  /** FlatMap the success value to a new Result. */
  <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);

  /** Map the error value to a new error. */
  <F> Result<T, F> mapError(Function<E, F> mapper);

  /** Get the success value or a default if this is a failure. */
  T orElse(T defaultValue);

  /** Get the success value or compute a default if this is a failure. */
  T orElseGet(Supplier<T> supplier);

  /** Get the success value or throw an exception if this is a failure. */
  T orElseThrow();

  /** Get the success value or throw a custom exception if this is a failure. */
  <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) throws X;

  /** Execute a consumer if this is a success. */
  Result<T, E> ifSuccess(Consumer<T> consumer);

  /** Execute a consumer if this is a failure. */
  Result<T, E> ifFailure(Consumer<E> consumer);

  /**
   * Filter the success value with a predicate. If the predicate fails, convert to a failure with
   * the given error.
   */
  Result<T, E> filter(Predicate<T> predicate, E error);

  /** Recover from a failure by providing an alternative value. */
  Result<T, E> recover(Function<E, T> recovery);

  /** Recover from a failure by providing an alternative Result. */
  Result<T, E> recoverWith(Function<E, Result<T, E>> recovery);

  /** Success implementation. */
  record Success<T, E>(T value) implements Result<T, E> {
    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public Optional<T> getSuccess() {
      return Optional.of(value);
    }

    @Override
    public Optional<E> getError() {
      return Optional.empty();
    }

    @Override
    public <U> Result<U, E> map(Function<T, U> mapper) {
      return new Success<>(mapper.apply(value));
    }

    @Override
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
      return mapper.apply(value);
    }

    @Override
    public <F> Result<T, F> mapError(Function<E, F> mapper) {
      return new Success<>(value);
    }

    @Override
    public T orElse(T defaultValue) {
      return value;
    }

    @Override
    public T orElseGet(Supplier<T> supplier) {
      return value;
    }

    @Override
    public T orElseThrow() {
      return value;
    }

    @Override
    public <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) {
      return value;
    }

    @Override
    public Result<T, E> ifSuccess(Consumer<T> consumer) {
      consumer.accept(value);
      return this;
    }

    @Override
    public Result<T, E> ifFailure(Consumer<E> consumer) {
      return this;
    }

    @Override
    public Result<T, E> filter(Predicate<T> predicate, E error) {
      return predicate.test(value) ? this : new Failure<>(error);
    }

    @Override
    public Result<T, E> recover(Function<E, T> recovery) {
      return this;
    }

    @Override
    public Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
      return this;
    }
  }

  /** Failure implementation. */
  record Failure<T, E>(E error) implements Result<T, E> {
    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public Optional<T> getSuccess() {
      return Optional.empty();
    }

    @Override
    public Optional<E> getError() {
      return Optional.of(error);
    }

    @Override
    public <U> Result<U, E> map(Function<T, U> mapper) {
      return new Failure<>(error);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
      return new Failure<>(error);
    }

    @Override
    public <F> Result<T, F> mapError(Function<E, F> mapper) {
      return new Failure<>(mapper.apply(error));
    }

    @Override
    public T orElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T orElseGet(Supplier<T> supplier) {
      return supplier.get();
    }

    @Override
    public T orElseThrow() {
      throw new NoSuchElementException("Result is a failure: " + error);
    }

    @Override
    public <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) throws X {
      throw exceptionSupplier.get();
    }

    @Override
    public Result<T, E> ifSuccess(Consumer<T> consumer) {
      return this;
    }

    @Override
    public Result<T, E> ifFailure(Consumer<E> consumer) {
      consumer.accept(error);
      return this;
    }

    @Override
    public Result<T, E> filter(Predicate<T> predicate, E error) {
      return this;
    }

    @Override
    public Result<T, E> recover(Function<E, T> recovery) {
      return new Success<>(recovery.apply(error));
    }

    @Override
    public Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
      return recovery.apply(error);
    }
  }
}
