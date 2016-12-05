package a.kgagent.util;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.*;
import com.google.zxing.qrcode.decoder.*;

public class QR
{
  public static BufferedImage encode( String message, int size )
  throws Exception
  {
    QRCodeWriter writer = new QRCodeWriter();

    Hashtable<EncodeHintType, ErrorCorrectionLevel> hints =
      new Hashtable<EncodeHintType, ErrorCorrectionLevel>();

    hints.put( EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M );

    BitMatrix matrix =
      writer.encode( message , BarcodeFormat.QR_CODE, size, size, hints );

    BufferedImage image =
      new BufferedImage( size, size, BufferedImage.TYPE_INT_RGB );
    image.createGraphics();

    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setColor( Color.WHITE );
    graphics.fillRect( 0, 0, size, size );
    graphics.setColor(Color.BLACK);

    for (int ii = 0; ii < size; ii++)
    {
      for (int jj = 0; jj < size; jj++)
        if (matrix.get(ii, jj))
          graphics.fillRect( ii, jj, 1, 1 );
    }
    return image;
  }
}
