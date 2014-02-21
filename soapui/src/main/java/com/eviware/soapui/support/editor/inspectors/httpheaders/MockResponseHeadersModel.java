package com.eviware.soapui.support.editor.inspectors.httpheaders;

import com.eviware.soapui.impl.wsdl.mock.WsdlMockResponse;
import com.eviware.soapui.model.mock.MockResponse;
import com.eviware.soapui.support.types.StringToStringsMap;

public class MockResponseHeadersModel extends HttpHeadersInspectorModel.AbstractHeadersModel<MockResponse>
{
	public MockResponseHeadersModel( MockResponse mockResponse )
	{
		super( false, mockResponse, WsdlMockResponse.HEADERS_PROPERTY );
	}

	public StringToStringsMap getHeaders()
	{
		return getModelItem().getResponseHeaders();
	}

	public void setHeaders( StringToStringsMap headers )
	{
		getModelItem().setResponseHeaders( headers );
	}

}