package a.kgagent.util;

import java.util.Arrays;

// a MessagePart is a byte sequence:
//
// length : an unsigned byte indicating the length of the part, exclusive
//
// key : an array of bytes beginning with a 0x02 0x03 or 0x04 to indicate
//       the key length
//
// sig : an ECDSA signature of variable length
//

public class MessagePart
{
  private byte[] key_;
  private byte[] sig_;

  public byte[] key() { return key_; }
  public byte[] sig() { return sig_; }

  private MessagePart() {}

  public MessagePart( byte[] key, byte[] sig ) throws Exception
  {
    if (null == key || key.length < 33 || key.length > 65)
      throw new Exception( "Invalid key, length: " + key.length );

    if (null == sig || sig.length < 64 || sig.length > 72)
      throw new Exception( "Invalid sig length: " + sig.length );

    key_ = key;
    sig_ = sig;
  }

  public static MessagePart fromBytes( byte[] raw ) throws Exception
  {
    byte[] key = null;
    byte[] sig = null;

    int len = (int) raw[0] & 0xFF;

    if ( (byte)0x02 == raw[1] || (byte)0x03 == raw[1] )
    {
      key = Arrays.copyOfRange( raw, 1, 34 );
      sig = Arrays.copyOfRange( raw, 34, len + 1 );
    }

    if ( (byte)0x04 == raw[1] )
    {
      key = Arrays.copyOfRange( raw, 1, 66 );
      sig = Arrays.copyOfRange( raw, 66, len + 1 );
    }

    return new MessagePart( key, sig );
  }

  public byte[] toBytes()
  {
    byte len = (byte) ((key_.length + sig_.length) & 0xFF);
    byte[] result = ByteOps.concat( key_, sig_ );
    result = ByteOps.prepend( len, result );

    return result;
  }

  public static void main( String[] args ) throws Exception
  {
    Secp256k1 curve = new Secp256k1();

    byte[] privkey = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( privkey );

    byte[] pubkey = curve.publicKeyCreate( privkey );
    byte[] signature = curve.signECDSA( SHA256.hash(pubkey), privkey );

    MessagePart pt = new MessagePart( pubkey, signature );
    byte[] ptb = pt.toBytes();

    MessagePart pt2 = MessagePart.fromBytes( ptb );
    System.out.println( "PASS: " + HexString.encode( ptb ) );
  }
}

