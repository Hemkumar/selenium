/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.internal;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.Lock;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Implements {@link org.openqa.selenium.internal.Lock} via an implementation that uses a well-known server socket.
 * 
 * @author gregory.block@gmail.com (Gregory Block)
 */
public class SocketLock implements Lock {
  public static final int DEFAULT_PORT = 7055;
  private static final long DELAY_BETWEEN_SOCKET_CHECKS = 100;

  private static final InetSocketAddress localhost = new InetSocketAddress("localhost", DEFAULT_PORT - 1);
  
  private final Socket lockSocket;
  private final InetSocketAddress address;

  /**
   * Constructs a new SocketLock using the default port.  Attempts to lock the
   * lock will block until the default port becomes free.
   */
  public SocketLock() {
    this(localhost);
  }

  /**
   * Constructs a new SocketLock.  Attempts to lock the lock will attempt to acquire the
   * specified port number, and wait for it to become free.
   *
   * @param lockPort the port number to lock
   */
  public SocketLock(int lockPort) {
    this(new InetSocketAddress("localhost", lockPort));
  }

  /**
   * Constructs a new SocketLock.  Attempts to lock the lock will attempt to acquire the
   * specified port number, and wait for it to become free.
   *
   * @param address The port to lock.
   */
  public SocketLock(InetSocketAddress address) {
    this.lockSocket = new Socket();
    this.address = address;
  }

  /**
   * @inheritDoc
   */
  public void lock(long timeoutInMillis) throws WebDriverException {

    // Calculate the 'exit time' for our wait loop.
    long maxWait = System.currentTimeMillis() + timeoutInMillis;

    // Attempt to acquire the lock until something goes wrong or we run out of time.
    do {
      try {
        if (isLockFree(address))
          return;
        Thread.sleep(DELAY_BETWEEN_SOCKET_CHECKS);
      } catch (InterruptedException e) {
        throw new WebDriverException(e);
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    } while (System.currentTimeMillis() < maxWait);

    throw new WebDriverException(
        String.format("Unable to bind to locking port %d within %d ms", address.getPort(), timeoutInMillis));
  }

  /**
   * @inheritDoc
   */
  public void unlock() {
    try {
      if (lockSocket.isBound()) lockSocket.close();
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }
  
  /**
   * Test to see if the lock is free.  Returns instantaneously.
   * 
   * @param address the address to attempt to bind to
   * @return true if the lock is locked; false if it is not
   * @throws IOException if something goes catastrophically wrong with the socket
   */
  private boolean isLockFree(InetSocketAddress address) throws IOException {
    try {
      lockSocket.bind(address);
      return true;
    } catch (BindException e) {
      return false;
    }
  }
}
