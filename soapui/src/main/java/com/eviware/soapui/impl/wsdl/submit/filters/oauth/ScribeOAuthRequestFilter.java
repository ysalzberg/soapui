package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.UISupport;
import org.apache.http.HttpRequest;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * @author Anders Jaensson
 */
public class ScribeOAuthRequestFilter extends AbstractRequestFilter
{

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		try
		{
			Project project = ModelSupport.getModelItemProject( request );

			OAuthService service = new ServiceBuilder()
					.provider( TwitterApi.class ) // FIXME: existing classes for common auth providers
					.apiKey( project.getPropertyValue( "oauth_consumer_key" ) )
					.apiSecret( project.getPropertyValue( "oauth_consumer_secret" ) )
					.callback( "http://localhost:8080" )
					.build();

			Token requestToken = service.getRequestToken();

			String authUrl = service.getAuthorizationUrl( requestToken );

			String code = askUserForCode( authUrl );

			Verifier v = new Verifier( code );
			Token accessToken = service.getAccessToken( requestToken, v );

			// FIXME: no built in support for http client.
			HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );

			applyOauthHeaders( service, accessToken, httpRequest );

		}
		catch( Exception e )
		{
			SoapUI.logError( e, "oauth failed" );
		}
	}


	private void applyOauthHeaders( OAuthService service, Token accessToken, HttpRequest httpRequest )
	{
		OAuthRequest oAuthRequest = new OAuthRequest( Verb.GET, httpRequest.getRequestLine().getUri() );
		service.signRequest( accessToken, oAuthRequest );

		Map<String, String> headers = oAuthRequest.getHeaders();

		for( Map.Entry<String, String> stringStringEntry : headers.entrySet() )
		{
			httpRequest.setHeader( stringStringEntry.getKey(), stringStringEntry.getValue() );
		}
	}

	private String askUserForCode( String authUrl ) throws IOException
	{
		Desktop.getDesktop().browse( URI.create( authUrl ) );
		return UISupport.getDialogs().prompt( "Enter code", "enter code" );
	}
}
