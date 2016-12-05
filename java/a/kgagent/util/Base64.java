package a.kgagent.util;

import javax.xml.bind.DatatypeConverter;

public class Base64
{
  public static final String DEFAULT_ENCODING = "UTF-8";

  public static String encode( String src ) throws Exception
  {
    return DatatypeConverter.printBase64Binary(
             src.getBytes(DEFAULT_ENCODING) );
  }

  public static String encode( byte[] bytes ) throws Exception
  {
    return DatatypeConverter.printBase64Binary(bytes);
  }

  public static byte[] decode( String src ) throws Exception
  {
    return DatatypeConverter.parseBase64Binary( src );
  }

  public static void main( String[] args ) throws Exception
  {
    if (null == args || 1 != args.length)
    {
      System.out.println( "usage: <B64>" );
      return;
    }

    byte[] unenc = Base64.decode( args[0] );

    String asStr = new String( unenc, DEFAULT_ENCODING );

    System.out.println( asStr );
  }
}
