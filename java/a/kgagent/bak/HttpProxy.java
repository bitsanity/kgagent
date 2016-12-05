package a.kgagent;

import java.net.*;
import java.io.*;

// A sample kgagent that operates as a http proxy. It catches 401 Unauthorized
// challenges and returns a substitute login form to the browser. When the user
// completes the challenge it grabs the response and stores it, subbing it in
// to all future requests to the same host

public class HttpProxy
{
  public HttpProxy( int locport ) throws Exception
  {
    ServerSocket ss = new ServerSocket( locport );

    boolean keepGoing = true;
    while( keepGoing )
      new ProxyWorker( ss.accept() ).start();

    ss.close();
  }

  public static void main( String[] args ) throws Exception
  {
    if (1 != args.length)
    {
      System.out.println( "HttpProxy <port>" );
      return;
    }

    HttpProxy px = new HttpProxy( Integer.parseInt(args[0]) );
  }
}

