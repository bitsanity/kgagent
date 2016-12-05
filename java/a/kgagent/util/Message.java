package a.kgagent.util;

import java.util.Arrays;
import java.util.Vector;

// a Message contains 1..n MessageParts
//
// Each part begins with an unsigned byte indicating how many more bytes
// are in that part
//
// After the length is a key that begins with 0x02, 0x03 or 0x04 from which
// we can determine the number of bytes in the key
//
// The last part is the signature which is the remaining bytes in the part
// after the key

public class Message
{
  private MessagePart[] parts_ = null;

  // empty message
  public Message() {}

  // convenience constructor for Challenge and Response
  public Message( MessagePart part )
  {
    parts_ = new MessagePart[1];
    parts_[0] = part;
  }

  public Message( MessagePart[] parts )
  {
    parts_ = parts;
  }

  public int parts()
  {
    if (null != parts_) return parts_.length;
    return 0;
  }

  public MessagePart part( int ix )
  {
    if (null != parts_ && ix < parts_.length)
      return parts_[ix];

    return null;
  }

  public String toString()
  {
    if (null == parts_ || 0 == parts_.length) return null;

    byte[] raw = new byte[0];

    for (int ii = 0; ii < parts_.length; ii++)
      raw = ByteOps.concat( raw, parts_[ii].toBytes() );

    try
    {
      return Base64.encode( raw );
    }
    catch( Exception e ) { e.printStackTrace(); }

    return null;
  }

  public static Message parse( String msg ) throws Exception
  {
    byte[] raw = Base64.decode( msg );
    byte[] wrk = raw;

    int ix = 0;
    Vector<MessagePart> resultV = new Vector<MessagePart>();

    while( true )
    {
      MessagePart part = MessagePart.fromBytes( wrk );
      resultV.add( part );

      ix += part.key().length + part.sig().length + 1;
      if (ix >= raw.length - 1) break;

      wrk = Arrays.copyOfRange( raw, ix, raw.length );
    }

    MessagePart[] result = new MessagePart[ resultV.size() ];
    resultV.toArray( result );

    // verify all signatures in the signature chain

    Secp256k1 curve = new Secp256k1();

    for (int ii = result.length - 1; ii >= 0; ii--)
    {
      if (0 == ii)
      {
        // this part is the Challenge
        if (!curve.verifyECDSA( result[0].sig(),
                                SHA256.hash( result[0].key() ),
                                result[0].key() ))
          throw new Exception( "failed sig [0]" );
      }
      else
      {
        if (!curve.verifyECDSA( result[ii].sig(),
                                SHA256.hash( result[ii-1].sig() ),
                                result[ii].key() ))
          throw new Exception( "failed sig [" + ii + "]" );
      }
    }

    return new Message( result );
  }

  public static void main( String[] args ) throws Exception
  {
    Secp256k1 curve = new Secp256k1();

    // a Gatekeeper has private key g and public key G

    byte[] g = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( g );
    byte[] G = curve.publicKeyCreate( g );

    // G signs own pubkey and makes a Message for A

    byte[] gsig = curve.signECDSA( SHA256.hash(G), g );
    MessagePart gpart = new MessagePart( G, gsig );

    // an Agent has priv key a and public key A

    byte[] a = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( a );
    byte[] A = curve.publicKeyCreate( a );

    // A signs G's signature

    byte[] asig = curve.signECDSA( SHA256.hash(gsig), a );
    MessagePart apart = new MessagePart( A, asig );

    // a keymaster has priv key k and pub key K

    byte[] k = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( k );
    byte[] K = curve.publicKeyCreate( k );

    // K signs A's signature

    byte[] ksig = curve.signECDSA( SHA256.hash(asig), k );
    MessagePart kpart = new MessagePart( K, ksig );

    // Message as sent to Agent from Gatekeeper

    Message msg = new Message( new MessagePart[] { gpart, apart, kpart } );
    String sent = msg.toString();

    // Message as received by Gatekeeper relayed by Agent from Keymaster

    Message rxed = Message.parse( sent );

    System.out.println( "Message: PASS" );
  }
}

