package a.kgagent.util;

import java.security.MessageDigest;

public class SHA256
{
  public static byte[] hash( byte[] src ) throws Exception
  {
    MessageDigest md = MessageDigest.getInstance( SHANAME );
    md.update( src );
    return md.digest();
  }

  public static byte[] doubleHash( byte[] src ) throws Exception
  {
    return hash( hash(src) );
  }

  final private static String SHANAME = "SHA-256";
}
