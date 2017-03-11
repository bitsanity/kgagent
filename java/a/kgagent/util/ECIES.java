package a.kgagent.util;

import java.util.Arrays;

import java.security.SecureRandom;

// -----------------------------------------------------------------------------
// Sources/references:
//
// http://www.johannes-bauer.com/compsci/ecc/
// http://www.shoup.net/papers/iso-2_1.pdf
// https://en.wikipedia.org/wiki/Integrated_Encryption_Scheme
// -----------------------------------------------------------------------------

public class ECIES
{
  public ECIES( byte[] myPrivateKey,
                byte[] othersPublicKey )
  {
    myPrivKey_ = myPrivateKey;
    otherPubKey_ = othersPublicKey;
  }

  public String encrypt( byte[] red ) throws Exception
  {
    Secp256k1 curve = new Secp256k1();

    // generate r, a new 32-byte random number
    ECKeyPair ec = ECKeyPair.makeNew();
    byte[] R = ec.publickey();

    // Shared Secret S = Px where P = (Px, Py) = Qb . r

    byte[] P = curve.publicKeyMult( otherPubKey_, ec.privatekey() );
    P = ByteOps.dropFirstByte( P ); // minus the leading 0x04

    // S = Px used for symmetric encryption.
    byte[] S = Arrays.copyOfRange( P, 0, 32 );

    // symmetrically encrypt red message using S as shared secret

    // first convert to text and pad with spaces to length of multiple of 16
    // bytes

    StringBuilder cenc = new StringBuilder( Base64.encode(red) );

    int padcount = 16 - cenc.length() % 16;

    for ( int ii = 0; ii < padcount; ii++ )
      cenc.append( ' ' );

    // encrypt bytes of B64-encoded string
    byte[] c = new AES256( S ).encrypt( cenc.toString().getBytes("UTF-8") );

    // d = sign( HASH(c), myprivatekey )

    // message = R + c

    StringBuffer sb = new StringBuffer();

    sb.append( Base64.encode(R) )
      .append( FLDSEP )
      .append( Base64.encode(c) );

    return sb.toString();
  }

  public byte[] decrypt( String black ) throws Exception
  {
    // parse message into R, c parts

    String[] parts = black.split( FLDSEP );
    if (    null == parts
         || 2 != parts.length )
      return null;

    byte[] R = Base64.decode( parts[0] ); // ephemeral, shared public key
    byte[] c = Base64.decode( parts[1] ); // encoded message

    // derive S = Px where P = (Px, Py) = R . kb

    Secp256k1 curve = new Secp256k1();

    byte[] P = curve.publicKeyMult( R, myPrivKey_ );
    byte[] S = Arrays.copyOfRange( P, 1, 33 ); // ignore 0x04 byte, take Px only

    // decrypt c

    byte[] red = new AES256( S ).decrypt( c );

    // now we have a space-padded B64-encoded string to convert back to bytes
    String redStr = new String( red, "UTF-8" );

    red = Base64.decode( redStr.trim() );

    return red;
  }

  private byte[] myPrivKey_;
  private byte[] otherPubKey_;

  // separates fields of B64-encoded message
  public static final String FLDSEP = ":";

  // Test --------------------------------------------------------------------
  public static void main( String[] args ) throws Exception
  {
    Secp256k1 curve = new Secp256k1();

    // Alice's keys
    ECKeyPair alice = ECKeyPair.makeNew();
    byte[] Qa = alice.publickey();

    // Bob's keys
    ECKeyPair bob = ECKeyPair.makeNew();
    byte[] Qb =  bob.publickey();

    // Alice prepares to send ...
    ECIES aliceEndpoint = new ECIES( alice.privatekey(), Qb );

    // ... a cosmic-top-secret message
    String message = "Hello Bob";

    String forBob = aliceEndpoint.encrypt( message.getBytes("UTF-8") );

    // Alice sends to Bob, now Bob decrypts, knowing Alice's public key ...
    ECIES bobEndpoint = new ECIES( bob.privatekey(), Qa );

    String red = new String( bobEndpoint.decrypt(forBob), "UTF-8" );

    if ( !red.equals(message) )
      throw new Exception( "ECIES.main(): FAIL" );

    System.out.println( "ECIES: PASS" );
  }
}
