package org.tursunkulov.exception;

import lombok.experimental.StandardException;

@StandardException
public class UserNotFoundException extends Exception {
  public UserNotFoundException() {}
}
