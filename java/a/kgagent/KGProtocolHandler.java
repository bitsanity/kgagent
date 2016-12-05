package a.kgagent;
 
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import javax.imageio.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import a.kgagent.util.*;

public class KGProtocolHandler
{
  // cache of connections
  // enables multiple KG connections to remain open

  private static final Hashtable<String, Socket> conns_ =
    new Hashtable<String, Socket>();

  // cache of sessions, each with one (K,A,G) tuple indexed
  // same way as connections

  private static final Hashtable<String, KGSession> sess_ =
    new Hashtable<String, KGSession>();

  public KGProtocolHandler() {}

  public String get( KGURL url ) throws Exception
  {
    Socket sock = conns_.get( urlToIndex(url) );

    JSONObject reply = null;

    if (null == sock)
    {
      sock = new Socket( url.getHost(), url.getPort() );
      conns_.put( urlToIndex(url), sock );
      reply = doRPC( url, makeBlank() );
    }
    else
    {
      JSONObject req = makeRequest( url );
      reply = doRPC( url, req );
    }

    return handleReply( url, reply );
  }

  public String adilosResponse( KGURL url, String respB64 ) throws Exception
  {
    KGSession sess = sess_.get( urlToIndex(url) );

    if (null == sess)
      throw new Exception( "no session for " + url.toString() );

    MessagePart kp = null;
    try
    {
      // verify K signed Asig, not K as would be expected by Message

      kp = MessagePart.fromBytes( Base64.decode(respB64) );

      Secp256k1 curve = new Secp256k1();
      if (!curve.verifyECDSA( kp.sig(), SHA256.hash(sess.Asig_), kp.key() ))
        throw new Exception( "Invalid keymaster response." );
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }

    // return message to kgserver including three parts:
    //   G, Gsig           <-- stored in session
    //   A, SIG(Gsig,a)    <-- stored in session
    //   K, SIG(Asig,k)    <-- returned from keymaster

    MessagePart[] mparts = new MessagePart[]
    {
      new MessagePart( sess.G_, sess.Gsig_ ),
      new MessagePart( sess.A_, sess.Asig_ ),
      kp
    };

    Message msg = new Message( mparts );

    JSONArray params = new JSONArray();
    params.add( msg.toString() );

    JSONObject toSvr = new JSONObject();
    toSvr.put( "method", "adilos.response" );
    toSvr.put( "params", params );
    toSvr.put( "id", "null" );

    JSONObject fromSvr = doRPC( url, toSvr );
    return handleReply( url, fromSvr );
  }

  // useful for poking a server for a challenge
  private JSONObject makeBlank()
  {
    JSONArray blankArray = new JSONArray();

    JSONObject blankRequest = new JSONObject();
    blankRequest.put( "method", "request" );
    blankRequest.put( "params", blankArray );
    blankRequest.put( "id", "null" );

    return blankRequest;
  }

  private JSONObject makeRequest( KGURL url ) throws Exception
  {
    // fetch the keys for this session
    KGSession sess = sess_.get( urlToIndex(url) );
    if (null == sess)
      throw new Exception( "no session for " + url.toString() );

    // sign outgoing message with agent's key
    Secp256k1 curve = new Secp256k1();

    byte[] msg = url.getURI().getBytes();
    byte[] sig = curve.signECDSA( SHA256.hash(msg), sess.a_ );

    JSONObject soleparam = new JSONObject();
    soleparam.put( "req", Base64.encode(msg) );
    soleparam.put( "sig", Base64.encode(sig) );

    JSONArray parms = new JSONArray();
    parms.add( soleparam );

    JSONObject req = new JSONObject();
    req.put( "method", "request" );
    req.put( "params", parms );
    req.put( "id", "null" );

    return req;
  }

  private String handleReply( KGURL url, JSONObject reply ) throws Exception
  {
    // check for/handle errors first

    Object error = (Object) reply.get( "error" );
    if ( null != error && error instanceof JSONObject )
    {
      JSONObject errO = (JSONObject) error;

      String message = (String) errO.get( "message" );
      String data = (String) errO.get( "data" );

      if (null == message || null == data)
        throw new Exception( "invalid response: " + reply.toJSONString() );

      if (message.contains("adilos.challenge"))
        return challengeToHTML( url, data );
      else
        return genericError( url, errO );
    }

    // has to be a session
    KGSession sess = (KGSession) sess_.get( urlToIndex(url) );

    JSONObject res = (JSONObject) reply.get( "result" );
    String rspb64 = (String) res.get( "rsp" );
    byte[] rspraw = Base64.decode( rspb64 );
    byte[] sigraw = Base64.decode( (String)res.get("sig") );

    // always check the signature

    Secp256k1 curve = new Secp256k1();
    if (!curve.verifyECDSA( sigraw, SHA256.hash(rspraw), sess.G_ ))
      throw new Exception( "Invalid signature from gatekeeper" );

    return new String( rspraw );
  }

  private JSONObject doRPC( KGURL url, JSONObject json ) throws Exception
  {
    JSONObject fromServer = null;

    String ix = urlToIndex( url );
    Socket s = (Socket) conns_.get( ix );

    try
    {
      PrintWriter pw = new PrintWriter( s.getOutputStream(), true );
      BufferedReader br = new BufferedReader(
                            new InputStreamReader(s.getInputStream()) );

      // handshake
System.out.println( "OUT: " + json.toJSONString() );
      pw.println( json.toJSONString() );

System.out.println( "Starting read..." );

      String rsp = br.readLine();
System.out.println( "IN: " + rsp );

      JSONParser parser = new JSONParser();
      fromServer = (JSONObject) parser.parse( rsp );
    }
    catch( IOException ioe )
    {
      System.out.println( "IO fault: " + ioe.getMessage() );
      conns_.remove( ix );

      try { s.close(); } catch( Exception e ) {}
    }

    return fromServer;
  }

  private String challengeToHTML( KGURL url, String challB64 )
  throws Exception
  {
    Message msg = Message.parse( challB64 );

    // allocate a new session including provided G, our a, leave K null
    KGSession sess = new KGSession();
    sess.G_ = msg.part(0).key();
    sess.Gsig_ = msg.part(0).sig();
    sess.a_ = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG")
                              .nextBytes( sess.a_ );
    // add the A,Asig part ( where Asig = SIG(Gsig,a) )
    Secp256k1 curve = new Secp256k1();
    sess.A_ = curve.publicKeyCreate( sess.a_ );
    sess.Asig_ = curve.signECDSA( SHA256.hash(msg.part(0).sig()),
                                  sess.a_ );

    sess_.put( urlToIndex(url), sess );

    Message toKeymaster = new Message( new MessagePart[]
      { msg.part(0), new MessagePart(sess.A_,sess.Asig_) } );

    // convert wrapped challenge to image
    BufferedImage bi = QR.encode( toKeymaster.toString(), 500 ); // pixels
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bi, "png", baos );
    String pngB64 = Base64.encode( baos.toByteArray() );

    // make html page with form to submit
    String html =
      "<html>\n" +
      "<head>\n" +
      "<title>ADILOS Challenge</title>\n" +
      "</head>\n" +
      "<body>\n" +
      "<h1>Authentication Challenge</h1>\n" +

      "Request: " + url.toString() + "\n" +
      "<p/>\n" +
      "The gatekeeper at server: " + url.getHost() +
      " port: " + url.getPort() + "\n" +
      "has issued an authentication challenge.\n" +
      "<p/>\n" +
      "This browser wishes to be your kgagent and represent you to this " +
      "gatekeeper. Please sign the following challenge using your keymaster " +
      " and enter the text version of the response in the field below, " +
      "then submit.\n" +
      "<p/>\n" +
      "<img alt=\"Challenge\" src=\"data:image/png;base64," +
      pngB64 + "\" />\n" +
      "<p/>\n" +
      "<form action=\"ignored\" method=\"get\">\n" +
      "<input type=\"text\" size=\"64\" size=\"80\" " +
         "maxlength=\"256\" id=\"kgagent.response\" /><br>\n" +
      "<input id=\"kgagent.submit\" type=\"submit\" value=\"Submit\" />\n" +
      "</form>\n" +
      "<p/>\n" +
      "</body>\n" +
      "</html>\n";

    return html;
  }

  private String genericError( KGURL url, JSONObject error ) throws Exception
  {
    return
      "<html>\n" +
      "<head>\n" +
      "<title>Server Error</title>\n" +
      "<body>\n" +
      "The server at " + url.toString() + "returned an error:\n" +
      "<p/>\n" +
      "<b>code</b>: " + (String)error.get("code") + "\n" +
      "<b>message</b>: " + (String)error.get("message") + "\n" +
      "<b>data</b>: " + (String)error.get("data") + "\n" +
      "<p/>\n" +
      "</body>\n" +
      "</html>\n";
  }

  private String urlToIndex( KGURL url )
  {
    return url.getHost() + ":" + url.getPort();
  }

}
