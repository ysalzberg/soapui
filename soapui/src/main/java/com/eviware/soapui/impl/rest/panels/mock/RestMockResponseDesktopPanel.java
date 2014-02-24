package com.eviware.soapui.impl.rest.panels.mock;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.mock.RestMockResponse;
import com.eviware.soapui.impl.wsdl.panels.mockoperation.MockResponseXmlDocument;
import com.eviware.soapui.model.mock.MockResponse;
import com.eviware.soapui.support.MediaTypeComboBox;
import com.eviware.soapui.support.editor.inspectors.httpheaders.HttpHeadersInspector;
import com.eviware.soapui.support.editor.inspectors.httpheaders.MockResponseHeadersModel;
import com.eviware.soapui.ui.support.AbstractMockResponseDesktopPanel;
import org.apache.commons.httpclient.HttpStatus;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.util.Vector;

public class RestMockResponseDesktopPanel extends
		AbstractMockResponseDesktopPanel<RestMockResponse, MockResponse>
{

	public RestMockResponseDesktopPanel( MockResponse mockResponse )
	{
		super( ( RestMockResponse )mockResponse );

		init( mockResponse );
	}

	public JComponent addTopEditorPanel()
	{
		JPanel topEditorPanel = new JPanel();
		topEditorPanel.setLayout( new BoxLayout( topEditorPanel, BoxLayout.Y_AXIS ) );

		topEditorPanel.add( createLabelPanel() );
		topEditorPanel.add( createHeaderInspector() );
		topEditorPanel.add( Box.createVerticalStrut( 5 ) );
		topEditorPanel.add( createHttpStatusPanel() );
		topEditorPanel.add( Box.createVerticalStrut( 5 ) );
		topEditorPanel.add( createMediaTypeCombo() );


		return topEditorPanel;
	}

	private JComponent createLabelPanel()
	{
		return createPanelWithLabel( "Headers: ", Box.createHorizontalGlue() );
	}

	private JComponent createHttpStatusPanel()
	{
		return createPanelWithLabel( "Http Status Code: ", createStatusCodeCombo() );
	}

	protected MockResponseMessageEditor buildResponseEditor()
	{
		MockResponseXmlDocument documentContent = new MockResponseXmlDocument( getMockResponse() );
		MockResponseMessageEditor mockResponseMessageEditor = new MockResponseMessageEditor( documentContent );
		setMediaType( mockResponseMessageEditor.getInputArea(), getModelItem().getMediaType() );
		return mockResponseMessageEditor;
	}

	public void setMediaType( RSyntaxTextArea inputArea, String mediaType )
	{
		if( mediaType.contains( "json" ) )
		{
			inputArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT );
		}
		else if( mediaType.contains( "xml" ) )
		{
			inputArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
		}
		else
		{
			inputArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_NONE );
		}

	}


	private JComponent createMediaTypeCombo()
	{
		MediaTypeComboBox mediaTypeComboBox = new MediaTypeComboBox( this.getModelItem() );
		mediaTypeComboBox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( ItemEvent e )
			{
				setMediaType( getResponseEditor().getInputArea(), e.getItem().toString() );
			}
		} );
		return createPanelWithLabel( "Media type: ", mediaTypeComboBox );
	}

	private JComponent createPanelWithLabel( String labelText, Component rightSideComponent )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.X_AXIS ) );

		panel.add( new JLabel( labelText ) );
		panel.add( rightSideComponent );
		panel.add( Box.createHorizontalGlue() );

		return panel;
	}

	private JComboBox createStatusCodeCombo()
	{
		ComboBoxModel httpStatusCodeComboBoxModel = new HttpStatusCodeComboBoxModel();

		final JComboBox statusCodeCombo = new JComboBox( httpStatusCodeComboBoxModel );

		statusCodeCombo.setSelectedItem( CompleteHttpStatus.from( getModelItem().getResponseHttpStatus() ) );
		statusCodeCombo.setToolTipText( "Set desired HTTP status code" );
		statusCodeCombo.addItemListener( new ItemListener()
		{
			public void itemStateChanged( ItemEvent e )
			{
				getModelItem().setResponseHttpStatus( ( ( CompleteHttpStatus )statusCodeCombo.getSelectedItem() ).getStatusCode() );
			}
		} );
		return statusCodeCombo;
	}

	private JComponent createHeaderInspector()
	{
		MockResponseHeadersModel model = new MockResponseHeadersModel( getModelItem() );
		HttpHeadersInspector inspector = new HttpHeadersInspector( model );

		JComponent component = inspector.getComponent();
		return component;
	}

}

class CompleteHttpStatus
{
	private int statusCode;
	private String description;

	private CompleteHttpStatus( int statusCode )
	{
		this.statusCode = statusCode;
		this.description = HttpStatus.getStatusText( statusCode );
	}

	public static CompleteHttpStatus from( int statusCode )
	{
		return new CompleteHttpStatus( statusCode );
	}

	public int getStatusCode()
	{
		return statusCode;
	}

	@Override
	public String toString()
	{
		return "" + statusCode + " - " + description;
	}

	@Override
	public boolean equals( Object object )
	{
		return ( ( CompleteHttpStatus )object ).statusCode == statusCode;

	}
}

class HttpStatusCodeComboBoxModel extends DefaultComboBoxModel
{
	private static Vector<CompleteHttpStatus> LIST_OF_CODES = new Vector<CompleteHttpStatus>();

	static
	{
		final String statusCodePrefix = "SC_";

		for( Field statusCodeField : HttpStatus.class.getDeclaredFields() )
		{
			try
			{
				if( statusCodeField.getName().startsWith( statusCodePrefix ) )
				{
					LIST_OF_CODES.add( CompleteHttpStatus.from( statusCodeField.getInt( null ) ) );
				}
			}
			catch( IllegalAccessException e )
			{
				SoapUI.logError( e );
			}
		}
	}

	public HttpStatusCodeComboBoxModel()
	{
		super( LIST_OF_CODES );
	}
}
