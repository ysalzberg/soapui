package com.eviware.soapui.impl.rest.panels.mock;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.mock.RestMockResponse;
import com.eviware.soapui.support.DocumentListenerAdapter;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.support.editor.EditorLocation;
import com.eviware.soapui.support.editor.EditorLocationListener;
import com.eviware.soapui.support.editor.EditorView;
import com.eviware.soapui.support.editor.views.xml.source.XmlSourceEditorView;
import com.eviware.soapui.support.swing.JTextComponentPopupMenu;
import com.eviware.soapui.support.swing.SoapUISplitPaneUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;

public class RestMockResponseJsonMessageEditorView implements EditorView<RestMockResponseJsonDocument>
{
	private RestMockResponseJsonMessageEditor editor;
	private RestMockResponse modelItem;
	private RestMockResponseJsonDocument jsonDocument;
	private JSplitPane splitter;
	private RSyntaxTextArea editArea;
	private RTextScrollPane editorScrollPane;
	private boolean updating = false;
	private JScrollPane errorScrollPane;


	public RestMockResponseJsonMessageEditorView( RestMockResponseJsonMessageEditor editor, RestMockResponse modelItem )
	{
		this.editor = editor;
		this.modelItem = modelItem;
		this.jsonDocument = new RestMockResponseJsonDocument( this.modelItem );
	}


	@Override
	public Editor<RestMockResponseJsonDocument> getEditor()
	{
		return editor;
	}

	@Override
	public String getTitle()
	{
		return "JSON";
	}

	@Override
	public JComponent getComponent()
	{
		if( splitter == null )
			buildUI();

		return splitter;
	}

	private void buildUI()
	{
		editArea = new RSyntaxTextArea( 20, 60 );

		try
		{
			Theme theme = Theme.load( RestMockResponseJsonMessageEditorView.class.getResourceAsStream( XmlSourceEditorView.RSYNTAXAREA_THEME ) );
			theme.apply( editArea );
		}
		catch( IOException e )
		{
			SoapUI.logError( e, "Could not load XML editor color theme file" );
		}

		editArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
		editArea.setFont( UISupport.getEditorFont() );
		editArea.setCodeFoldingEnabled( true );
		editArea.setAntiAliasingEnabled( true );
		editArea.setMinimumSize( new Dimension( 50, 50 ) );
		editArea.setCaretPosition( 0 );
		editArea.setEnabled( true );
		editArea.setEditable( true );
		editArea.setBorder( BorderFactory.createMatteBorder( 0, 2, 0, 0, Color.WHITE ) );



		splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT )
		{
			public void requestFocus()
			{
				SwingUtilities.invokeLater( new Runnable()
				{

					public void run()
					{
						editArea.requestFocusInWindow();
					}
				} );
			}

			public boolean hasFocus()
			{
				return editArea.hasFocus();
			}
		};

		splitter.setUI( new SoapUISplitPaneUI() );
		splitter.setDividerSize( 0 );
		splitter.setOneTouchExpandable( true );

		editArea.getDocument().addDocumentListener( new DocumentListenerAdapter()
		{

			public void update( Document document )
			{
				if( !updating && getDocument() != null )
				{
					updating = true;
					getDocument().setJson( editArea.getText() );
					updating = false;
				}
			}
		} );

		JPanel p = new JPanel( new BorderLayout() );
		editorScrollPane = new RTextScrollPane( editArea );

		JTextComponentPopupMenu.add( editArea );

		//buildPopup( editArea.getPopupMenu(), editArea );


		splitter.setTopComponent( editorScrollPane );
		splitter.setBottomComponent( errorScrollPane );
		splitter.setDividerLocation( 1.0 );
		splitter.setBorder( null );
	}

	@Override
	public boolean deactivate()
	{
		return false;
	}

	@Override
	public boolean activate( EditorLocation<RestMockResponseJsonDocument> location )
	{
		return false;
	}

	@Override
	public EditorLocation<RestMockResponseJsonDocument> getEditorLocation()
	{
		return null;
	}

	@Override
	public void setLocation( EditorLocation<RestMockResponseJsonDocument> location )
	{

	}

	@Override
	public void setDocument( RestMockResponseJsonDocument document )
	{
		this.jsonDocument = document;
		this.editArea.setText( document.getJson() );
	}

	@Override
	public RestMockResponseJsonDocument getDocument()
	{
		return this.jsonDocument;
	}

	@Override
	public void addLocationListener( EditorLocationListener<RestMockResponseJsonDocument> listener )
	{

	}

	@Override
	public void removeLocationListener( EditorLocationListener<RestMockResponseJsonDocument> listener )
	{

	}

	@Override
	public void release()
	{

	}

	@Override
	public void setEditable( boolean enabled )
	{

	}

	@Override
	public String getViewId()
	{
		return null;
	}

	@Override
	public void requestFocus()
	{

	}

	@Override
	public void locationChanged( EditorLocation<RestMockResponseJsonDocument> location )
	{

	}

	@Override
	public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener )
	{

	}

	@Override
	public void addPropertyChangeListener( PropertyChangeListener listener )
	{

	}

	@Override
	public void removePropertyChangeListener( PropertyChangeListener listener )
	{

	}

	@Override
	public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener )
	{

	}
}
