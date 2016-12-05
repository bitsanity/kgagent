package a.kgagent.util;

import java.util.Arrays;

public class ByteOps
{
  public static byte[] xor( byte[] a, byte[] b )
  {
    if ( a.length != b.length ) return null;

    byte[] result = new byte[ a.length ];
    for ( int ii = 0; ii < result.length; ii++ )
      result[ii] = (byte)(a[ii] ^ b[ii]);

    return result;
  }

  public static byte[] concat( byte[] a, byte b )
  {
    byte[] ba = new byte[] { b };
    return concat( a, ba );
  }

  public static byte[] concat( byte[] a, byte[] b )
  {
    if (a == null) return b;
    if (b == null) return a;

    byte[] result = new byte[ a.length + b.length ];
    System.arraycopy( a, 0, result, 0,        a.length );
    System.arraycopy( b, 0, result, a.length, b.length );
    return result;
  }

  public static byte[] prepend( byte b, byte[] ba )
  {
    byte[] result = new byte[ ba.length + 1 ];

    result[0] = b;
    System.arraycopy( ba, 0, result, 1, ba.length );

    return result;
  }

  public static byte[] dropFirstByte( byte[] a )
  {
    if (null == a || 0 == a.length) return a;
    return Arrays.copyOfRange(a, 1, a.length);
  }
}
