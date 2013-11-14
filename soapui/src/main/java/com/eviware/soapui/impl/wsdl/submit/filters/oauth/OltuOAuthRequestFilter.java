package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import org.apache.http.HttpRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
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

	public static final String CALLBACK_URL = "http://localhost:8080/";

	private String accessToken;


	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{

		try
		{
			Project project = ModelSupport.getModelItemProject( request );
			String oauthConsumerKey = project.getPropertyValue( "oauth_consumer_key" );
			String oauthConsumerSecret = project.getPropertyValue( "oauth_consumer_secret" );

			// Authorize
			if ( StringUtils.isNullOrEmpty( accessToken )){
				String authorizationCode = authorize( oauthConsumerKey );

				// get access token
				accessToken = retrieveAccessToken( oauthConsumerKey, oauthConsumerSecret, authorizationCode );
			}

			// sign the request using the access token
			signRequest( accessToken, context );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}

	private String authorize( String oauthConsumerKey ) throws OAuthSystemException, IOException
	{
		String authUrl = createAuthUrl( oauthConsumerKey );
		return askUserForCode( authUrl );
	}

	private String retrieveAccessToken( String oauthConsumerKey, String oauthConsumerSecret, String code ) throws OAuthSystemException, OAuthProblemException
	{

		OAuthClientRequest accessTokenRequest = OAuthClientRequest
				.tokenProvider( OAuthProviderType.FACEBOOK )
				.setGrantType( GrantType.AUTHORIZATION_CODE )
				.setClientId( oauthConsumerKey )
				.setClientSecret( oauthConsumerSecret )
				.setRedirectURI( CALLBACK_URL )
				.setCode( code )
				.buildBodyMessage();

		OAuthClient oAuthClient = new OAuthClient( new HttpClient4( HttpClientSupport.getHttpClient() ) );

		GitHubTokenResponse accessTokenResponse = oAuthClient.accessToken( accessTokenRequest, GitHubTokenResponse.class );

		// The access token should be stored somewhere and used until it expires
		String accessToken = accessTokenResponse.getAccessToken();
		SoapUI.log( String.format( "Access Token: %s, Expires in: %s", accessToken, accessTokenResponse.getExpiresIn() ) );
		return accessToken;
	}

	private String createAuthUrl( String oauthConsumerKey ) throws OAuthSystemException
	{
		return OAuthClientRequest
				.authorizationProvider( OAuthProviderType.FACEBOOK )
				.setClientId( oauthConsumerKey )
				.setRedirectURI( CALLBACK_URL )
				.buildQueryMessage().getLocationUri();
	}

	private void signRequest( String accessToken, SubmitContext context ) throws OAuthSystemException
	{
		HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
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
		return UISupport.getDialogs().prompt( "Please enter the authorization code", "title" );
	}
}
