package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.UISupport;
import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.testing.json.MockJsonFactory;
import com.google.common.base.Splitter;
import org.apache.http.HttpRequest;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Anders Jaensson
 */
public class GoogleOAuthRequestFilter extends AbstractRequestFilter
{
	private static final String PROPERTY_NAME_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
	private static final String PROPERTY_NAME_OAUTH_CONSUMER_SECRET = "oauth_consumer_secret";
	public static final String REDIRECT_URL = "http://localhost:8080/";
	private String accessToken;
	private String refreshToken;

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		Project project = ModelSupport.getModelItemProject( request );
		String oauthConsumerKey = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_KEY );
		String oauthConsumerSecret = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_SECRET );
		AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder( BearerToken.authorizationHeaderAccessMethod(),
				new NetHttpTransport(),
				new MockJsonFactory(),
				new GenericUrl( "https://graph.facebook.com/oauth/access_token" ), new ClientParametersAuthentication( oauthConsumerKey, oauthConsumerSecret ), oauthConsumerKey, "https://graph.facebook.com/oauth/authorize" )
				.build();

		String authorizationCodeRequestUrl = flow.newAuthorizationUrl().setRedirectUri( REDIRECT_URL ).build();

		try
		{
			String authorizationCode = askUserForCode( authorizationCodeRequestUrl );
			AuthorizationCodeTokenRequest authorizationCodeTokenRequest = flow.newTokenRequest( authorizationCode ).setRedirectUri( REDIRECT_URL );

			String tokenResponse = authorizationCodeTokenRequest.executeUnparsed().parseAsString();

			Map<String, String> split = Splitter.on( '&' ).withKeyValueSeparator( "=" ).split( tokenResponse );
			accessToken = split.get( "access_token" );
			refreshToken = split.get( "refresh_token" );

			Credential credential =
					new Credential( BearerToken.authorizationHeaderAccessMethod() ).setAccessToken( accessToken );
			HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
			HttpRequestFactory requestFactory = flow.getTransport().createRequestFactory( credential );

			String uri = httpRequest.getRequestLine().getUri();
			com.google.api.client.http.HttpRequest httpRequest1 = requestFactory.buildGetRequest( new GenericUrl( uri ) );

			credential.intercept( httpRequest1 );

			HttpHeaders headers = httpRequest1.getHeaders();
			for( Map.Entry<String, Object> entry : headers.entrySet() )
			{
				String value = ( ( List<String> )entry.getValue() ).get( 0 );
				httpRequest.setHeader( entry.getKey(), value );
			}

			SoapUI.log( tokenResponse );

		}
		catch( IOException e )
		{
			SoapUI.logError( e );
		}
	}


	private String askUserForCode( String authUrl ) throws IOException
	{
		Desktop.getDesktop().browse( URI.create( authUrl ) );
		return UISupport.getDialogs().prompt( "Please enter the authorization code", "title" );
	}
}
