package a.kgagent.util;

public class Secp256k1
{
  static
  {
    System.loadLibrary( "secp256k1" );
    System.loadLibrary( "kgagent" );
  }

  public Secp256k1()
  {
    int res = this.resetContext();
  }

  // inits data structure of precomputed tables for speed.
  // only needs to be done once but multiple times does not hurt
  private native int resetContext();

  public native boolean privateKeyIsValid( byte[] in_seckey );

  public native byte[] publicKeyCreate( byte[] in_seckey );

  // Q * N
  public native byte[] publicKeyMult( byte[] in_pubkey, byte[] in_tweak );

  public native byte[] signECDSA( byte[] hash32, byte[] in_seckey );

  public native boolean
  verifyECDSA( byte[] signature, byte[] hash32, byte[] in_pubkey );
}
