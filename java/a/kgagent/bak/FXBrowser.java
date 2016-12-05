package a.kgagent;
 
import javafx.application.Application;
import javafx.beans.value.*;
import javafx.concurrent.Worker.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.*;
import javafx.stage.Stage;
 
import org.w3c.dom.*;
import org.w3c.dom.events.*;
import org.w3c.dom.html.*;

public class FXBrowser extends Application
{
  private final WebView wv_ = new WebView();
  private final TextField urlField_ = new TextField();
  private final KGProtocolHandler kg_ = new KGProtocolHandler();

  public static void main( String[] args ) {
    launch( args );
  }

  @Override
  public void start( Stage primaryStage )
  {
    primaryStage.setTitle( "ADILOS kgagent" );

    GridPane grid = new GridPane();
    grid.setAlignment( Pos.CENTER );
    grid.setHgap( 10 );
    grid.setVgap( 10 );
    grid.setPadding( new Insets(10, 10, 10, 10) );

    RowConstraints r1 = new RowConstraints();
    RowConstraints r2 = new RowConstraints();
    r2.setVgrow( Priority.ALWAYS );
    grid.getRowConstraints().addAll( r1, r2 );

    ColumnConstraints c1 = new ColumnConstraints();
    ColumnConstraints c2 = new ColumnConstraints();
    c2.setHgrow( Priority.ALWAYS );
    grid.getColumnConstraints().addAll( c1, c2 );

    Label urlLabel = new Label( "URL:" );

    grid.add( urlLabel, 0, 0 );
    grid.add( urlField_, 1, 0 );

    grid.add( wv_, 0, 1, 2, 1 );
    wv_.getEngine().setJavaScriptEnabled( true );

    listenUrlField();
    listenHyperclicks();
    listenFormSubmissions();

    Scene scene = new Scene( grid, 640, 480 );
    primaryStage.setScene( scene );

    BorderPane root = new BorderPane();
    root.setCenter( grid );

    primaryStage.setScene( new Scene(root, 640, 480) );
    primaryStage.show();
  }

  private void loadFrom( String url )
  {
    try
    {
      if (url.startsWith("kg:"))
      {
        KGURL kurl = KGURL.parse( url );
        String html = kg_.get( kurl );
        wv_.getEngine().loadContent( html ); // does not run scripts
        return;
      }

      // else assume http
      wv_.getEngine().load( url ); // runs scripts
    }
    catch( Exception e )
    {
      // todo: load an error page
      e.printStackTrace();
    }
  }

  // action fires when user hits <Enter> in the url field

  private void listenUrlField()
  {
    urlField_.setOnAction( new EventHandler<ActionEvent>()
    {
      public void handle( ActionEvent evt )
      {
        String newURL = urlField_.getText();

        // default to http if scheme not specified
        if (    null != newURL
             && !newURL.startsWith("kg:")
             && !newURL.startsWith("http:")
             && !newURL.startsWith("https:") )
        {
          newURL = "http://" + newURL;
          if (!newURL.endsWith("/")) newURL = newURL + "/";
        }

        urlField_.setText( newURL );
        loadFrom( newURL );
      }
    } );
  }

  // respond to load events by going through the DOM and assigning a listener
  // to all hyperlink

  private void listenHyperclicks()
  {
    wv_.getEngine().getLoadWorker().stateProperty().addListener(
      new ChangeListener<State>() {
        public void changed( ObservableValue ov,
                             State oldState,
                             State newState )
        {
          if (newState == State.SUCCEEDED)
          {
            EventListener listener = new EventListener() {
                public void handleEvent( org.w3c.dom.events.Event ev )
                {
                  ev.preventDefault();

                  if (ev.getType().equals("click"))
                  {
                    String path =
                      ((org.w3c.dom.Element)ev.getCurrentTarget())
                        .getAttribute( "href" );

                    // handle relative links
                    if (     null != path
                          && !path.toUpperCase().contains("HTTP")
                          && !path.startsWith("kg:")
                       )
                    {
                      String prev = urlField_.getText().toUpperCase();

                      // http default behavior is to simply append the
                      // path to the page URL
                      if (prev.startsWith("HTTP"))
                        path = urlField_.getText() + path;

                      // kg overwrites old resource name with new resource
                      // name
                      if (prev.startsWith("KG:"))
                      {
                        try {
                          KGURL kg = KGURL.parse( urlField_.getText() );
                          kg.setURI( path );
                          path = kg.toString();
                        }
                        catch( Exception e ) { }
                      }
                    }

                    loadFrom( path );
                  }
                }
            };

            Document doc = wv_.getEngine().getDocument();
            NodeList ays = doc.getElementsByTagName( "a" );

            for( int ii = 0; ii < ays.getLength(); ii++ )
              ((org.w3c.dom.events.EventTarget)ays.item(ii))
                .addEventListener( "click", listener, false );
          }
        }
      } );
  }

  // see https://community.oracle.com/thread/2510161
  private void listenFormSubmissions()
  {
    wv_.getEngine().documentProperty().addListener(
      new ChangeListener<Document>()
      {
        public void changed( ObservableValue<? extends Document> ov,
                             Document olddoc,
                             Document newdoc )
        {
System.out.println( "changed: " + ov.toString() );
          if (    newdoc != null
               && 0 < newdoc.getElementsByTagName("form").getLength() )
          {
            HTMLFormElement form =
              (HTMLFormElement)newdoc.getElementsByTagName( "form" ).item(0);

            String action = null;
            if (null != form)
              action = form.getAttribute( "action" );

            if ( null != action && action.equalsIgnoreCase("kg:kgagent") )
            {
              // expect agent to have only one input in the form, assume
              // textfield containing the response in Base64 same as that
              // in keymaster's QR coded response

              NodeList nodes = form.getElementsByTagName( "input" );
              HTMLInputElement rspField = (HTMLInputElement)nodes.item( 0 );

              if ( null != rspField && null != rspField.getValue() )
              {
                String inputname = rspField.getAttribute( "name" );

                if (    null != inputname
                     && inputname.equalsIgnoreCase("adilos.response") )
                {
                  String rspb64 = rspField.getValue();
                  String html = "<html><h1>ERROR</h1></html>";

                  try {
                    html =
                      kg_.adilosResponse( KGURL.parse(rspField.getValue()),
                                          rspb64 );
                  }
                  catch( Exception e ) { }

                  wv_.getEngine().loadContent( html );
                }
              }
            }
          }
        }
    } );
  }
}
