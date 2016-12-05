package a.kgagent;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

import a.kgagent.util.*;

public class Browser extends JPanel
                     implements HyperlinkListener, ActionListener
{
  private JTextField url_;
  private JEditorPane ed_;

  public Browser()
  {
    this.setLayout( new BorderLayout() );

    this.add( url_ = new JTextField("   "), BorderLayout.PAGE_START );
    url_.addActionListener( this );

    JScrollPane sp = new JScrollPane( ed_ = new JEditorPane() );
    sp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
    sp.setPreferredSize( new Dimension(800, 600) );
    sp.setMinimumSize( new Dimension(100, 100) );

    this.add( sp, BorderLayout.CENTER );

    ed_.setEditable( false );
    ed_.setContentType( "text/html" );

    ed_.addHyperlinkListener( this );

    ((HTMLEditorKit)ed_.getEditorKitForContentType( "text/html" ))
                       .setAutoFormSubmission( false ); 
  }

  public void actionPerformed( ActionEvent e )
  {
    try
    {
      URL url = new URL( url_.getText() );
      fetch( true, url_.getText(), null );
    }
    catch( Throwable t )
    {
      t.printStackTrace();
    }
  }

  private void fetch( boolean isGet, String url, String postData )
  throws Exception
  {
    ADILOSFetcher fetcher = new ADILOSFetcher();
    String html = null;

    Thread t = new Thread( new Runnable() {
      public void run()
      {
        try {
          String html = fetcher.doGet( url );

          SwingUtilities.invokeLater( new Runnable() {
            public void run() {
              HTMLEditorKit kit = (HTMLEditorKit) ed_.getEditorKit();
              Document doc = ed_.getDocument();
              StringReader rdr = new StringReader( html );
              try
              {
                kit.read( rdr, doc, 0 );
              }
              catch( Exception e )
              {
                e.printStackTrace();
              }
            }
          } );
        }
        catch( Exception e ) {
          e.printStackTrace();
        }
      }
    } );
    t.start();
  }

  public void hyperlinkUpdate( HyperlinkEvent e )
  {
    if (e instanceof FormSubmitEvent)
    {
      String contents = ( (FormSubmitEvent)e ).getData();

      try
      {
        URL url = new URL( url_.getText() );
        fetch( false, url_.getText(), contents );
      }
      catch( Throwable t )
      {
        t.printStackTrace();
      }
      return;
    }

    if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
      return;

    if (e instanceof HTMLFrameHyperlinkEvent)
    {
      HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
      HTMLDocument doc = (HTMLDocument)ed_.getDocument();
      doc.processHTMLFrameHyperlinkEvent( evt );
    }
    else
    {
      try
      {
        ed_.setPage( e.getURL() );
      } catch (Throwable t)
      {
        t.printStackTrace();
      }
    }
  }

  public static void main( String[] args ) throws Exception
  {
    JFrame jf = new JFrame( "kgagent" );
    jf.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

    jf.getContentPane().add( new Browser() );

    jf.pack();
    jf.setVisible( true );
  }
}

