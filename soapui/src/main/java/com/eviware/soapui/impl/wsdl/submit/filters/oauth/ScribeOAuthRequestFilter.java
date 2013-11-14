package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedGetMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedPostMethod;
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

	public static final String CALLBACK_URL = "http://localhost:8080";
	public static final String PROPERTY_NAME_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
	public static final String PROPERTY_NAME_OAUTH_CONSUMER_SECRET = "oauth_consumer_secret";

	private Token accessToken;

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		try
		{
			Project project = ModelSupport.getModelItemProject( request );
			String oauthConsumerKey = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_KEY );
			String oauthConsumerSecret = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_SECRET );

			OAuthService service = new ServiceBuilder()
					.provider( TwitterApi.class ) // existing classes for common oauth providers
					.apiKey( oauthConsumerKey )
					.apiSecret( oauthConsumerSecret )
					.callback( CALLBACK_URL )
					.build();

			if( accessToken == null )
			{
				Token requestToken = service.getRequestToken();
				String authorizationCode = authorize( service, requestToken );
				accessToken = retrieveAccessToken( service, requestToken, authorizationCode );
			}

			//  no built in support for http client.
			signRequest( service, accessToken, context );

		}
		catch( Exception e )
		{
			SoapUI.logError( e, "oauth failed" );
		}
	}

	private Token retrieveAccessToken( OAuthService service, Token requestToken, String authorizationCode ) throws IOException
	{
		return service.getAccessToken( requestToken, new Verifier( authorizationCode ) );
	}

	private String authorize( OAuthService service, Token requestToken ) throws IOException
	{
		String authUrl = service.getAuthorizationUrl( requestToken );
		return askUserForCode( authUrl );
	}


	private void signRequest( OAuthService service, Token accessToken, SubmitContext context )
	{
		HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );

		Verb verb;
		if( httpRequest instanceof ExtendedPostMethod )
		{
			verb = Verb.POST;

		}
		else if( httpRequest instanceof ExtendedGetMethod )
		{
			verb = Verb.GET;
		}
		else
		{
			throw new IllegalArgumentException( "Only GET and POST implemented yet" );
		}

		OAuthRequest oAuthRequest = new OAuthRequest( verb, httpRequest.getRequestLine().getUri() );
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
		return UISupport.getDialogs().prompt( "Please enter the authorization code", "title" );
	}
}
