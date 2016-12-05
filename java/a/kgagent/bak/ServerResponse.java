package a.kgagent;

import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

import a.kgagent.util.*;

public class ServerResponse
{
  private int code_ = 0;

  private byte[] buff_ = null;

  private String challengeB64_ = null;

  public int code() { return code_; }

  private ServerResponse() {}

  private Vector<String> message_ = new Vector<String>();

  // parse HTTP Response just checking for a 401 Unauthorized header, in which case we
  // substitute the challenge

  public ServerResponse( InputStream sstr ) throws Exception
  {
    String line = null;
    int contentlength = -1;

    BufferedReader fromServer = new BufferedReader( new InputStream(sstr) );

    while (null != (line = fromServer.readLine()))
    {
      if (0 == code_ && line.startsWith("HTTP"))
        code_ = Integer.parseInt( line.split(" ")[1] );

      if (-1 == contentlength && line.startsWith("Content-Length"))
        contentlength = Integer.parseInt( line.split(" ")[1] );

      // WWW-Authenticate ADILOS CH=<B64 string>
      if (    401 == code_
           && line.startsWith("WWW-Authenticate")
           && line.contains("ADILOS") )
        challengeB64_ = line.split(" ")[2].split("=")[1];

      message_.add( line );

      // switch from character/line to binary mode as content body may be gzipped

      if (0 == line.replaceAll("\n","").trim().length())
      {
        if (    0 < contentlength
             && (1024 * 256) > contentlength )
        {
          buff_ = new byte[ contentlength ];
          sstr.read( buff_ );
        }

        break;
      }
    }
  }

  // return http response back to the browser

  public void returnResponse( OutputStream os ) throws Exception
  {
    PrintWriter pw = new PrintWriter( os );

    for (String line : message_)
    {
      System.out.println( "Resp: " + line );
      pw.println( line );
    }

    if (    null != buff_ && 0 < buff_.length )
    {
    }

  }

  // mock a http response putting up the challenge

  public void returnChallenge( PrintWriter pw ) throws Exception
  {
    Message challenge = Message.parse( challengeB64_ );

    BufferedImage bi = QR.encode( challengeB64_, 500 ); // pixels
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bi, "png", baos );
    byte[] raw = baos.toByteArray();

    String png = a.kgagent.util.Base64.encode( raw );

    String msgbody =
      "<html>\n" +
      "<head>\n" +
      "<title>ADILOS Authentication Required</title>\n" +
      "</head>\n" +
      "</html>\n" +
      "<body>\n" +
      "<h1>ADILOS Authentication Challenge</h1>\n" +
      "\n" +
      "This agent wishes to represent you in your interaction with a " +
      "gatekeeper whose public key is:\n" +
      "<p/>\n" +
      "<b>Gatekeeper Key</b>:\n" +
      "<blockquote>\n" +
      HexString.encode( challenge.part(0).key() ) +
      "</blockquote>\n" +
      "<p/>\n" +
      "\n" +
      "<img alt=\"Challenge\" src=\"data:image/png;base64," + png + "\" />\n" +
      "\n" +
      "If you agree please sign with your keymaster and type the response (in Base64) below:\n" +
      "<p/>\n" +
      "\n" +
      "<form action=\"/login\" method=\"post\">\n" +
        "<input type=\"text\" size=\"64\" size=\"80\" " +
                "maxlength=\"256\" name=\"adilos.response\" />\n" +
        "<input type=\"submit\" value=\"Submit\" />\n" +
      "</form>\n" +
      "\n" +
      "</body>\n" +
      "</html>\n";

    String hdrs =
      "HTTP/1.1 200 OK\n" +
      "Content-Type: text/html\n" +
      "Content-Length: " + msgbody.length() + "\n" +
      "\n";

    String msg = hdrs + msgbody;

    pw.print( msg );
  }

}
