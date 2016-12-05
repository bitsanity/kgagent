package a.kgagent;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;

import a.kgagent.util.*;

public class ADILOSFetcher
{
  private byte[] a_; // this agent's private key
  private String adilosResponseB64_;

  public ADILOSFetcher() throws Exception
  {
    adilosResponseB64_ = null;

    a_ = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( a_ );
  }

  public void setAuthorization( String respB64 )
  {
    adilosResponseB64_ = respB64;
  }

  public String fetch( String urlString ) throws Exception
  {
    URL url = new URL( urlString );
    HttpURLConnection cx = (HttpURLConnection) url.openConnection();

    if (null != adilosResponseB64_)
    {
      cx.setRequestProperty( "Authorization",
                             "ADILOS " + adilosResponseB64_ );
    }

    cx.setRequestMethod( "GET" );
    cx.setReadTimeout( 5 * 1000 ); // 5 seconds, in milliseconds
    cx.setFollowRedirects( true );
    cx.connect();

    int code = cx.getResponseCode();

System.out.println( "Code: " + code );

    BufferedReader br = null;
    String challenge = null;

    // if code 401 and we try to get the stream the toolkit throws IOException
    try
    {
      br = new BufferedReader( new InputStreamReader(cx.getInputStream()) );
    }
    catch( IOException e )
    {
      if (HttpURLConnection.HTTP_UNAUTHORIZED == cx.getResponseCode())
      {
        String hdr = cx.getHeaderField( 1 );

        if ( hdr.toUpperCase().contains("ADILOS") )
        {
          challenge = hdr.split(" ")[1]
                         .split("=",2)[1]
                         .replace( "\"", "" )
                         .trim();

          return handleChallenge( url, challenge );
        }
      }
      else
      {
        e.printStackTrace();
        throw e;
      }
    }

    StringBuilder buff = new StringBuilder();

    String line = null;

    while( null != (line = br.readLine()) )
      buff.append( line + "\n" );

    br.close();

    return buff.toString();
  }

  private String handleChallenge( URL url, String chall ) throws Exception
  {
    Message incoming = Message.parse( chall );

    byte[] G = incoming.part(0).key();
    byte[] Gsig = incoming.part(0).sig();

    Secp256k1 curve = new Secp256k1();
    byte[] A = curve.publicKeyCreate( a_ );
    byte[] Asig = curve.signECDSA( SHA256.hash(Gsig), a_ );

    Message outgoing = new Message(
      new MessagePart[] { new MessagePart(G, Gsig),
                          new MessagePart(A, Asig) } );

    BufferedImage bi = QR.encode( outgoing.toString(), 500 );
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bi, "png", baos );
    byte[] raw = baos.toByteArray();
    String png = "data:image/png;base64," + Base64.encode( raw );

    String html =
      "<html><body>\n" +
      "<b>Agent:</b> " + HexString.encode(A) + "\n" +
      "<p/>" +
      "<b>Gatekeeper:</b> " + HexString.encode(G) + "\n" +
      "<p/>\n" +
      "<img alt=\"Embedded Image\" src=\"" + png + "\" />\n" +
      "<p/>\n" +
      "<b>Response:</b><p />\n" +
      "<form action=\"" + url.getHost() + "/adilos.login\" method=\"post\">\n" +
        "<input type=\"text\" name=\"adilos.response\" size=\"64\" /><p />\n" +
        "<input type=\"submit\" value=\"Submit\" />\n" +
      "</form>\n" +
      "</body></html>\n";

    return html;
  }
}

