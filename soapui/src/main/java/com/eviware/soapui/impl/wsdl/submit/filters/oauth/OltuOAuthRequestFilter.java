package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.UISupport;
import org.apache.http.HttpRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.httpclient4.HttpClient4;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * @author Anders Jaensson
 */
public class OltuOAuthRequestFilter extends AbstractRequestFilter
{

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		Project project = ModelSupport.getModelItemProject( request );

		try
		{

			// Authorize
			OAuthClientRequest r = OAuthClientRequest
					.authorizationProvider( OAuthProviderType.FACEBOOK )
					.setClientId( project.getPropertyValue( "oauth_consumer_key" ) )
					.setRedirectURI( "http://localhost:8080/" )
					.buildQueryMessage();

			String code = askUserForCode( r.getLocationUri() );

			// exchange authorization code for access token
			r = OAuthClientRequest
					.tokenProvider( OAuthProviderType.FACEBOOK )
					.setGrantType( GrantType.AUTHORIZATION_CODE )
					.setClientId( project.getPropertyValue( "oauth_consumer_key" ) )
					.setClientSecret( project.getPropertyValue( "oauth_consumer_secret" ) )
					.setRedirectURI( "http://localhost:8080/" )
					.setCode( code )
					.buildBodyMessage();

			OAuthClient oAuthClient = new OAuthClient( new HttpClient4( HttpClientSupport.getHttpClient() ) );

			GitHubTokenResponse oAuthResponse = oAuthClient.accessToken( r, GitHubTokenResponse.class );

			// The access token should be stored somewhere and used until it expires
			String accessToken = oAuthResponse.getAccessToken();
			SoapUI.log( String.format( "Access Token: %s, Expires in: %s", accessToken, oAuthResponse.getExpiresIn() ) );

			// sign the request using the access token. No real support for httpclient
			HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
			applyOauthHeaders( accessToken, httpRequest );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}

	private void applyOauthHeaders( String accessToken, HttpRequest httpRequest ) throws OAuthSystemException
	{
		String uri = httpRequest.getRequestLine().getUri();
		OAuthClientRequest req = new OAuthBearerClientRequest( uri ).setAccessToken( accessToken ).buildHeaderMessage();

		Map<String, String> headers = req.getHeaders();

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
