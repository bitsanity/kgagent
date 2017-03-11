package a.kgagent.util;

import java.util.*;

public class ECKeyPair
{
  private byte[] pvt_;

  public ECKeyPair( byte[] pvt ) throws Exception
  {
    if (null == pvt || 32 != pvt.length)
      throw new Exception( "Invalid pvt key length" );

    pvt_ = Arrays.copyOfRange( pvt, 0, pvt.length );
  }

  public static ECKeyPair makeNew() throws Exception
  {
    byte[] key = new byte[32];
    new Random().nextBytes( key );
    Secp256k1 crypto = new Secp256k1();
    if (!crypto.privateKeyIsValid(key)) throw new Exception( "invalid key" );
    return new ECKeyPair( key );
  }

  public byte[] publickey() throws Exception
  {
    Secp256k1 crypto = new Secp256k1();
    return crypto.publicKeyCreate( pvt_ );
  }

  public byte[] privatekey() throws Exception
  {
    return pvt_;
  }
}
