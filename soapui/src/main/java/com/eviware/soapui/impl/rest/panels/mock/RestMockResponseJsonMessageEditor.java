package com.eviware.soapui.impl.rest.panels.mock;

import com.eviware.soapui.impl.rest.mock.RestMockResponse;
import com.eviware.soapui.support.editor.Editor;

public class RestMockResponseJsonMessageEditor extends Editor<RestMockResponseJsonDocument>
{
	public RestMockResponseJsonMessageEditor( RestMockResponseJsonDocument document, RestMockResponse response )
	{
		super( document );

		super.addEditorView( new RestMockResponseJsonMessageEditorView( this, response ) );
	}
}
