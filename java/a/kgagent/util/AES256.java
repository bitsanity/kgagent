package a.kgagent.util;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.*;

public class AES256
{
  public AES256( byte[] aeskey ) throws Exception
  {
    sks_ = new SecretKeySpec( aeskey, KEYSPEC );
  }

  public byte[] encrypt( byte[] red ) throws Exception
  {
    assert( null != red && 0 == red.length % 16 && 0 < red.length );

    byte[] iv = new byte[16]; // must be 128 bits
    new SecureRandom().nextBytes( iv );
    IvParameterSpec ivps = new IvParameterSpec(iv);

    Cipher cipher = Cipher.getInstance( CIPHER );
    cipher.init( Cipher.ENCRYPT_MODE, sks_, ivps );

    return ByteOps.concat( iv, cipher.doFinal(red) );
  }

  public byte[] decrypt( byte[] blk ) throws Exception
  {
    assert( null != blk && blk.length > 16 );

    byte[] iv = Arrays.copyOfRange( blk, 0, 16 );
    IvParameterSpec ivps = new IvParameterSpec(iv);

    Cipher cipher = Cipher.getInstance( CIPHER );
    cipher.init( Cipher.DECRYPT_MODE, sks_, ivps );

    byte[] data = Arrays.copyOfRange( blk, 16, blk.length );

    assert( 0 == data.length % 16 );
    return cipher.doFinal( data );
  }

  private SecretKeySpec sks_;
  private static final String CIPHER = "AES/CBC/NoPadding";
  private static final String KEYSPEC = "AES";

  // Tests -------------------------------------------------------------------
  public static void main(String args[]) throws Exception
  {
    // Symmetric test
    String redText = "Hello World     Hello World     "; // multiple of 16 bytes
    String password = "123456";

    AES256 aes = new AES256( SHA256.hash(password.getBytes()) );

    byte[] blk = aes.encrypt( redText.getBytes("UTF-8") );
    assert( !blk.equals(redText.getBytes("UTF-8")) );

    String red = new String( aes.decrypt(blk), "UTF-8" );

    assert( red.equals(redText) );
  }
}
