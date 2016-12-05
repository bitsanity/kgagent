package a.kgagent;

import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

import a.kgagent.util.*;

public class ProxyWorker extends Thread
{
  private Socket  client_ = null;
  private Socket  server_ = null;
  private int     contentLength_ = 0;
  private boolean chunked_ = false;
  private byte[]  messageBody_ = null;
  private String  host_ = null;
  private int     port_ = 80;
  private int     code_ = 0;
  private String  challengeB64_ = null;

  // HTTP standard states:
  // ... headers must be ASCII
  private static final String ASCII = "US-ASCII";

  // ... headers must end in CRLF
  private static final String CRLF = "\r\n";
  private static byte CR = (byte) 0x0D & 0xFF;
  private static byte LF = (byte) 0x0A & 0xFF;

  // ... field names are case-INsensitive
  private static final String ADILOS   = new String( "ADILOS" );
  private static final String X_PARM   = new String( "X-ADILOS" );
  private static final String C401     = new String( "401" );
  private static final String CHUNKED  = new String( "CHUNKED" );
  private static final String CLENG    = new String( "CONTENT-LENGTH" );
  private static final String GET      = new String( "GET" );
  private static final String HOST     = new String( "HOST" );
  private static final String HTTP11   = new String( "HTTP/1.1" );
  private static final String POST     = new String( "POST" );
  private static final String XFER_ENC = new String( "Transfer-Encoding:" );

  // the form param name we watch for when client makes response
  private static final String A_PARM = new String( "adilos.response" );

  // cache of hostname --> response mappings
  private static final Hashtable<String, String> sessions_ = new Hashtable<String,String>();

  // data structure containing the headers
  private final Vector<String> headers_ = new Vector<String>();

  public ProxyWorker( Socket sclient ) throws Exception
  {
    client_ = sclient;
  }

  public void run()
  {
    try { go(); }
    catch( Exception e ) { e.printStackTrace(); }

    try {
      server_.close();
      client_.close();
    }
    catch( Exception e ) { e.printStackTrace(); }
  }

  private void go() throws Exception
  {
    InputStream fromClient = client_.getInputStream();
    OutputStream toClient = client_.getOutputStream();

    OutputStream toServer = null;
    InputStream fromServer = null;

    while( true )
    {
      System.out.println( "Reading headers from client..." );
      readHeaders( fromClient );

      System.out.println( "Reading message body from client..." );
      readMessageBody( fromClient );

      if (null == server_)
      {
        System.out.println( "Connecting to Host: " + host_ + " on port: " + port_ );

        server_ = new Socket( host_, port_ );
        toServer = server_.getOutputStream();
        fromServer = server_.getInputStream();
      }

      System.out.println( "Writing headers to server..." );
      writeHeaders( toServer );

      System.out.println( "Writing message body to server..." );
      writeMessageBody( toServer );

      // ================
      headers_.clear();
      contentLength_ = 0;
      messageBody_ = null;
      chunked_ = false;
      // ================

      System.out.println( "Reading headers from server..." );
      readHeaders( fromServer );

      if (401 == code_)
      {
        makeChallenge( toClient );
        continue;
      }

      System.out.println( "Reading body from server..." );
      readMessageBody( fromServer );

      System.out.println( "Writing headers to client..." );
      writeHeaders( toClient );

      System.out.println( "Writing body to client..." );
      writeMessageBody( toClient );

      // ================
      headers_.clear();
      contentLength_ = 0;
      messageBody_ = null;
      chunked_ = false;
      // ================

    }
  }

  private void readHeaders( InputStream is ) throws Exception
  {
    String hdr = null;
    while ( true )
    {
      if (null == (hdr = readHeader(is))) return;

      headers_.add( hdr );

      // the empty line signals end of headers
      if ( 0 == hdr.replaceAll("\\s+\n", "").length() )
      {
        if (headers_.elementAt(0).contains(POST))
        {
          // unless POST: next line is message body with params so read/send as a header
          String params = readHeader( is );

          if (params.contains(A_PARM))
            sessions_.put( host_, params.split("=")[1].trim() );

          messageBody_ = null;
        }

        break;
      }

      // X-ADILOS: <challenge in B64>
      if (hdr.contains(X_PARM))
        challengeB64_ = hdr.split(": ")[1].trim();
    }
  }

  private String readHeader( InputStream is ) throws Exception
  {
    StringBuilder buff = new StringBuilder();

    byte[] b = new byte[1];

    char c;

    while( true )
    {
      if ( -1 != is.read(b) )
      {
        c = (char)( b[0] & 0xFF );

        buff.append( c );

        if ('\n' == c)
          break;
      }
      else
        break;
    }

    String hdr = buff.toString();

System.out.print( hdr );

    String hdrupper = hdr.toUpperCase();

    // HTTP/1.1 browsers using HTTP proxy MUST specify host[:port]/uri in
    // the Request-Line header e.g.
    //
    //     GET http://host[:port]/uri HTTP/1.1
    //
    if ( hdrupper.startsWith(GET) || hdrupper.startsWith(POST) )
    {
      String meat = hdr.split("\\s+")[1].split( "/+" )[1];

      String[] parts = meat.split( ":" );
      host_ = parts[0];
      if (parts.length >= 2)
        port_ = Integer.parseInt( parts[1] );
    }

    // HTTP/1.1 200 OK
    if ( hdrupper.startsWith(HTTP11) )
    {
      String meat = hdr.split("\\s+")[1];
      code_ = Integer.parseInt( meat );
    }

    // Transfer-Encoding: chunked
    if ( hdrupper.startsWith(XFER_ENC) && hdrupper.contains(CHUNKED) )
      chunked_ = true;

    // Content-Length: 12345 (note whitespace after "fieldname:" is optional)

    if ( hdrupper.startsWith(CLENG) )
      contentLength_ = Integer.parseInt( hdr.split(":")[1].trim() );

    return hdr;
  }

  private void readMessageBody( InputStream is ) throws Exception
  {
    if (chunked_)
    {
      readChunks( is );
      return;
    }

    if (0 >= contentLength_) return;

System.out.println( "readMessageBody: contentLength_ = " + contentLength_ );

    messageBody_ = new byte[ contentLength_ ];

    int ix = 0;
    byte[] in = new byte[1];

    while (ix < contentLength_)
    {
      is.read( in );
      messageBody_[ ix++ ] = in[0];
    }
  }

  // Aggregate chunks into one message body then replace the Transfer-Encoding
  // header with Content-Length
  //
  // Transfer-Encoding: chunked
  //
  // <hex-length in ascii>\r\n
  // <hex-length sequence of bytes>\r\n
  // <0>\r\n
  // <trailer, possibly empty>
  // \r\n
  private void readChunks( InputStream is ) throws Exception
  {
    byte[] result = new byte[0];

    while( true )
    {
      byte[] hlen = readLine( is, 8 );
      String hs = HexString.encode( hlen );
      int chunklen = Integer.parseInt( hs, 16 );
System.out.println( "readChunks: chunklen = " + chunklen );
      byte[] data = readLine( is, chunklen );

      if (    2 == data.length
           && data[0] == CR
           && data[1] == LF )
        break;

      result = ByteOps.concat( result, data );
    }

    contentLength_ = result.length;
    messageBody_ = result;

    for (String header : headers_)
      if (header.startsWith(XFER_ENC))
      {
        headers_.remove( header );
        break;
      }

    headers_.add( "Content-Length: " + contentLength_ );
  }

  // return a byte array containing the bytes read up to and including a '\n'
  // but no more than maximum length
  private byte[] readLine( InputStream is, int maxlen ) throws Exception
  {
    int nread = 0;
    byte[] line = new byte[ maxlen ];

    byte[] b = new byte[1];
    while( nread < maxlen )
    {
      is.read( b );
      line[ nread ] = b[0];

      // 0x0A is '\n' in ASCII, java bytes are signed
      if (    b[0] == LF
           || nread >= maxlen )
        break;

      nread++;
    }

    byte[] result = new byte[ nread ];
    for( int ii = 0; ii < result.length; ii++)
      result[ii] = line[ii];

    return result;
  }

  private void writeHeaders( OutputStream os ) throws Exception
  {
    byte[] line = null;

    byte[] out = new byte[1];

    for( String hdr : headers_ )
    {
      line = hdr.getBytes( ASCII );

      for( int ii = 0; ii < line.length; ii++ )
      {
        out[0] = line[ii];
        os.write( out );
      }
    }
  }

  private void writeMessageBody( OutputStream os ) throws Exception
  {
    if (0 >= contentLength_) return;
    os.write( messageBody_ );
  }

  private void makeChallenge( OutputStream os ) throws Exception
  {
    Message challenge = Message.parse( challengeB64_ );

    BufferedImage bi = QR.encode( challengeB64_, 500 ); // pixels
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bi, "png", baos );
    byte[] raw = baos.toByteArray();

    String png = a.kgagent.util.Base64.encode( raw );

    String msg =
      "<html>" + CRLF +
      "<head>" + CRLF +
      "<title>ADILOS Authentication Required</title>" + CRLF +
      "</head>" + CRLF +
      "</html>" + CRLF +
      "<body>" + CRLF +
      "<h1>ADILOS Authentication Challenge</h1>" + CRLF +
      "" + CRLF +
      "This agent wishes to represent you in your interaction with a " +
      "gatekeeper whose public key is:" + CRLF +
      "<p/>" + CRLF +
      "<b>Gatekeeper Key</b>:" + CRLF +
      "<blockquote>" + CRLF +
      HexString.encode( challenge.part(0).key() ) +
      "</blockquote>" + CRLF +
      "<p/>" + CRLF +
      CRLF +
      "<img alt=\"Challenge\" src=\"data:image/png;base64," + png + "\" />" + CRLF +
      CRLF +
      "If you agree please sign with your keymaster and type the response (Base64) below:" + CRLF +
      "<p/>" + CRLF +
      CRLF +
      "<form action=\"/login\" method=\"post\">" + CRLF +
        "<input type=\"text\" size=\"64\" size=\"80\" " +
                "maxlength=\"256\" name=\"" + A_PARM + "\" />" + CRLF +
        "<input type=\"submit\" value=\"Submit\" />" + CRLF +
      "</form>" + CRLF +
      "</body>" + CRLF +
      "</html>" + CRLF;

    String hdrs =
      "HTTP/1.1 200 OK" + CRLF +
      "Content-Type: text/html" + CRLF +
      "Content-Length: " + msg.length() + CRLF +
      CRLF;

    String full = new String( (hdrs + msg).getBytes(ASCII), ASCII );

    os.write( full.getBytes() );
  }
}
