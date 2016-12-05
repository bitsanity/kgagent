package a.kgagent;
 
// URL Format: kg:host:port:uri

public class KGURL
{
  private String host_ = null;
  private int    port_ = 0;
  private String uri_  = null;

  public String getHost() { return host_; }
  public int    getPort() { return port_; }

  public String getURI() { return uri_; }
  public void setURI( String uri )
  {
    uri_ = uri;
  }

  private KGURL() {}

  public KGURL( String host, int port, String uri )
  {
    host_ = host;
    port_ = port;
    uri_ = uri;
  }

  public static KGURL parse( String s ) throws Exception
  {
    if (null == s || 0 == s.length() || !s.startsWith("kg:"))
      throw new Exception( "Invalid URL: " + s );

    int c1 = s.indexOf( ':' );
    int c2 = s.indexOf( ':', c1 + 1 );
    int c3 = s.indexOf( ':', c2 + 1 );

    KGURL result = new KGURL();
    result.host_ = s.substring( c1 + 1, c2 );
    result.port_ = Integer.parseInt( s.substring(c2 + 1, c3) );
    result.uri_ = s.substring( c3 + 1 );

    return result;
  }

  public String toString()
  {
    return "kg:" +
           ((null != host_)? host_ : "." ) + ":" +
           ((0 != port_)? port_ : "." ) + ":" +
           ((null != uri_)? uri_ : "." );
  }

  public static void main( String[] args ) throws Exception
  {
    String url = "kg:hostname:8000:default?name=value&something:something";

    KGURL kg = parse( url );

    System.out.println( kg.toString() );
  }

}
