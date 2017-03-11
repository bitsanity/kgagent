package a.kgagent.util;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES256
{
  public AES256( byte[] aeskey ) throws Exception
  {
    sks_ = new SecretKeySpec( aeskey, KEYSPEC );
  }

  public byte[] encrypt( byte[] red16 ) throws Exception
  {
    Cipher cipher = Cipher.getInstance( CIPHER );
    cipher.init( Cipher.ENCRYPT_MODE, sks_ );
    return cipher.doFinal( red16 );
  }

  public byte[] decrypt( byte[] blk16 ) throws Exception
  {
    Cipher cipher = Cipher.getInstance( CIPHER );
    cipher.init( Cipher.DECRYPT_MODE, sks_ );
    return cipher.doFinal( blk16 );
  }

  private SecretKeySpec sks_;
  private static final String CIPHER = "AES/ECB/NoPadding";
  private static final String KEYSPEC = "AES";

  // Tests -------------------------------------------------------------------
  public static void main(String args[]) throws Exception
  {
    // Symmetric test
    String redText = "Hello World     "; // 16 bytes
    String password = "123456";

    AES256 aes = new AES256( SHA256.hash(password.getBytes()) );

    byte[] blk = aes.encrypt( redText.getBytes("UTF-8") );

    String red = new String( aes.decrypt(blk), "UTF-8" );

    // encrypt a specified hex-encoded red message and print black version
    if (0 < args.length)
    {
      aes = new AES256( password.getBytes("UTF-8") );
      blk = HexString.decode( args[0] );
      red = new String( aes.decrypt(blk), "UTF-8" );
      System.out.println( red );
      return;
    }

    // http://www.inconteam.com/software-development/41-encryption/55-aes-test-vectors
    // AES ECB 256-bit encryption mode
    // Encryption key:
    //   603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4
    // Initialization vector:
    //   not necessary

    byte[] key =
      HexString.decode( "603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4" );

    String[] inputs = new String[] {
                        "6bc1bee22e409f96e93d7e117393172a",
                        "ae2d8a571e03ac9c9eb76fac45af8e51",
                        "30c81c46a35ce411e5fbc1191a0a52ef",
                        "f69f2445df4f9b17ad2b417be66c3710" };

    String[] expecteds = new String[] {
                        "f3eed1bdb5d2a03c064b5a7e3db181f8",
                        "591ccb10d410ed26dc5ba74a31362870",
                        "b6ed21b99ca6f4f9f153e7b1beafed1d",
                        "23304b7a39f9f3ff067d8d8f9e24ecc7" };

    aes = new AES256( key );

    for ( int ii = 0; ii < inputs.length; ii++ )
    {
      byte[] inhex = HexString.decode( inputs[ii] );
      byte[] exhex = HexString.decode( expecteds[ii] );
      byte[] outhex = aes.encrypt( inhex );

      if (!Arrays.equals(exhex, outhex))
        throw new Exception( "AES256.main(): FAIL\n\texpected: " +
                             expecteds[ii] +
                             "\n\treceived: " +
                             HexString.encode(outhex) );
    } // end for
  }
}
