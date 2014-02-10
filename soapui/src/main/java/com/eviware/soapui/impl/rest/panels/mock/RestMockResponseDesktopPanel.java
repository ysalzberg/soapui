package com.eviware.soapui.impl.rest.panels.mock;

import com.eviware.soapui.impl.rest.mock.RestMockResponse;
import com.eviware.soapui.impl.wsdl.panels.mockoperation.MockResponseXmlDocument;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.ui.support.AbstractMockResponseDesktopPanel;

public class RestMockResponseDesktopPanel extends
		AbstractMockResponseDesktopPanel<RestMockResponse, RestMockResponse>
{
	public RestMockResponseDesktopPanel( RestMockResponse mockResponse )
	{
		super( mockResponse );

		init( mockResponse );
	}

	protected Editor<?> buildResponseEditor()
	{
		RestMockResponse mockResponse = getMockResponse();
		if( mockResponse.isJson())
		{
			return  new RestMockResponseJsonMessageEditor( new RestMockResponseJsonDocument( mockResponse ), mockResponse );
		}
		return new MockResponseMessageEditor( new MockResponseXmlDocument( mockResponse ) );
	}
}
