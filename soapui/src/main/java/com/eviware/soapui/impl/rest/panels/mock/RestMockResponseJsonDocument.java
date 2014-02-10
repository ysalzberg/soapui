package com.eviware.soapui.impl.rest.panels.mock;

import com.eviware.soapui.impl.wsdl.mock.WsdlMockResponse;
import com.eviware.soapui.model.mock.MockResponse;
import com.eviware.soapui.support.editor.xml.support.AbstractEditorDocument;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class RestMockResponseJsonDocument extends AbstractEditorDocument implements PropertyChangeListener
{
	private final MockResponse mockResponse;

	public RestMockResponseJsonDocument( MockResponse response )
	{
		this.mockResponse = response;

		mockResponse.addPropertyChangeListener( WsdlMockResponse.RESPONSE_CONTENT_PROPERTY, this );
	}

	public String getJson()
	{
		return mockResponse.getResponseContent();
	}

	public void setJson( String xml )
	{
		mockResponse.setResponseContent( xml );
	}

	public void propertyChange( PropertyChangeEvent arg0 )
	{
		fireXmlChanged( ( String )arg0.getOldValue(), ( String )arg0.getNewValue() );
	}

	@Override
	public void release()
	{
		mockResponse.removePropertyChangeListener( WsdlMockResponse.RESPONSE_CONTENT_PROPERTY, this );
	}
}
