package a.kgagent;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientRequest
{
  private Vector<String> message_ = new Vector<String>();

  private int port_ = 80;
  private String host_ = null;
  private String adilosToken_ = null;

  public String host() { return host_; }
  public int port() { return port_; }

  private ClientRequest() {}

  public ClientRequest( Socket cli ) throws Exception
  {
    byte[] buff = new byte[1];

    int count = 0;

    while (null != (line = fromClient.readLine()))
    {
      // HTTP/1.1 http://www.initech.com:8000/uri GET
      if ( line.contains("HTTP") )
      {
        String hport = line.split(" ")[1]
                           .split( "/+" )[1];

        String[] parts = hport.split( ":" );
        host_ = parts[0];

        if (parts.length == 2)
          port_ = Integer.parseInt( parts[1] );
      }

      // Host: www.initech.com
      // Host: www.initech.com:8080

      if (line.startsWith("Host"))
      {
        String hostpart = line.split( " " )[1];

        if ( -1 != hostpart.indexOf( ':' ))
        {
          host_ = hostpart.split(":")[0];
          port_ = Integer.parseInt( hostpart.split(":")[1] );
        }
        else
        {
          host_ = hostpart;
          port_ = 80;
        }
      }

      message_.add( line );

      // Empty line is end-of-message marker, unless ...

      if (0 == line.replaceAll("\n","").length())
      {
        // in POST there is a message-body containing params after the empty line

        if (message_.elementAt(0).contains("POST"))
        {
          String mbody = fromClient.readLine();

          if (null != mbody && mbody.contains("adilos.response="))
            adilosToken_ = mbody.split("=")[1];

          message_.add( mbody );
        }

        break;
      }

      count++;
    }
  }

  public void sendRequest( Socket webserver ) throws Exception
  {
    System.out.println( "sending" );

    if (null != adilosToken_)
    {
      if (message_.elementAt(0).contains("GET"))
        message_.add( message_.size() - 1, "Authorization: adilos " + adilosToken_ );
      else // POST
        message_.add( message_.size() - 2, "Authorization: adilos " + adilosToken_ );
    }

    for (String line : message_)
    {
      System.out.println( "Req: " + line );
      pw.println( line );
    }
  }

}
