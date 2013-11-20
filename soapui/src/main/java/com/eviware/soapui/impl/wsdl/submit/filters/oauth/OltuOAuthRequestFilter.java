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
import org.apache.http.HttpRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.token.BasicOAuthToken;
import org.apache.oltu.oauth2.common.token.OAuthToken;
import org.apache.oltu.oauth2.httpclient4.HttpClient4;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * OAuth 2.0 three-legged flow for Facebook (https://github.com/Mashape/mashape-oauth/blob/master/FLOWS.md#oauth-2-three-legged).
 * 1. If no existing access token, user is sent to facebook for authorization
 * 1a. User logs in to facebook and grants access to the application
 * 1b. User is redirected back to CALLBACK_URL with the authorization code as a query parameter
 * 1c. The authorization code is exchanged for an access token
 * 2. The access token is used to sign the request
 *
 * @author Anders Jaensson
 */
public class OltuOAuthRequestFilter extends AbstractRequestFilter
{

	private static final String PROPERTY_NAME_API_KEY = "oauth_consumer_key";
	private static final String PROPERTY_NAME_API_SECRET = "oauth_consumer_secret";
	private static final OAuthProviderType provider = OAuthProviderType.GOOGLE;
	public static final String CALLBACK_URL = "http://localhost:8080/";
	public static final String GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";

	private OAuthToken token;
	private Project project;

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{

		try
		{
			project = ModelSupport.getModelItemProject( request );
			String apiKey = project.getPropertyValue( PROPERTY_NAME_API_KEY );
			String apiSecret = project.getPropertyValue( PROPERTY_NAME_API_SECRET );

			if( token == null || StringUtils.isNullOrEmpty( token.getAccessToken() ) )
			{
				String authorizationCode = authorize( apiKey );
				token = retrieveAccessToken( apiKey, apiSecret, authorizationCode );
			}
			else
			{
				token = retrieveAccessTokenUsingRefreshToken( apiKey, apiSecret );
			}

			// sign the request using the access token
			signRequest( token, context );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}

	private String authorize( String oauthConsumerKey ) throws Exception
	{
		String authUrl = createAuthUrl( oauthConsumerKey );
		return waitForAuthorizationCode( authUrl );
	}

	private OAuthToken retrieveAccessTokenUsingRefreshToken( String apiKey, String apiSecret ) throws Exception
	{
		OAuthClientRequest accessTokenRequest = OAuthClientRequest
				.tokenProvider( provider )
//				.tokenLocation( "http://localhost:8080/access_token" )
				.setGrantType( GrantType.REFRESH_TOKEN )
				.setClientId( apiKey )
				.setClientSecret( apiSecret )
				.setRefreshToken( token.getRefreshToken() )
				.buildBodyMessage();

		OAuthClient oAuthClient = new OAuthClient( new HttpClient4( HttpClientSupport.getHttpClient() ) );

		OAuthToken accessToken = oAuthClient.accessToken( accessTokenRequest, OAuthJSONAccessTokenResponse.class ).getOAuthToken();

		return new BasicOAuthToken( accessToken.getAccessToken(), accessToken.getExpiresIn(), token.getRefreshToken(), accessToken.getScope() );
	}

	private OAuthToken retrieveAccessToken( String apiKey, String apiSecret, String code ) throws Exception
	{

		OAuthClientRequest accessTokenRequest = OAuthClientRequest
				.tokenProvider( provider )
//				.tokenLocation( "http://localhost:8080/access_token" )
				.setGrantType( GrantType.AUTHORIZATION_CODE )
				.setClientId( apiKey )
				.setClientSecret( apiSecret )
				.setRedirectURI( CALLBACK_URL )
				.setCode( code )
				.buildBodyMessage();

		OAuthClient oAuthClient = new OAuthClient( new HttpClient4( HttpClientSupport.getHttpClient() ) );

		// facebook and github do not return json and have their own response handlers
//		OAuthToken token = oAuthClient.accessToken( accessTokenRequest, FacebookTokenResponse.class ).getOAuthToken();

		OAuthToken token = oAuthClient.accessToken( accessTokenRequest, OAuthJSONAccessTokenResponse.class ).getOAuthToken();
		String accessToken = token.getAccessToken();

		SoapUI.log( String.format( "Access Token: %s, Expires in: %s", accessToken, token.getExpiresIn() ) );
		SoapUI.log( String.format( "Refresh Token: %s", token.getRefreshToken() ) );

		return token;
	}

	private String createAuthUrl( String apiKey ) throws Exception
	{
		return OAuthClientRequest
				.authorizationProvider( provider )
//				.authorizationLocation( "http://localhost:8080/authorize" )
				.setClientId( apiKey )
				.setRedirectURI( CALLBACK_URL )
				.setResponseType( "code" )
				.setScope( GOOGLE_DRIVE_SCOPE )
				.buildQueryMessage().getLocationUri();
	}

	private void signRequest( OAuthToken token, SubmitContext context ) throws Exception
	{
		HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
		String uri = httpRequest.getRequestLine().getUri();
		OAuthClientRequest req = new OAuthBearerClientRequest( uri ).setAccessToken( token.getAccessToken() ).buildHeaderMessage();

		Map<String, String> headers = req.getHeaders();

		for( Map.Entry<String, String> stringStringEntry : headers.entrySet() )
		{
			httpRequest.setHeader( stringStringEntry.getKey(), stringStringEntry.getValue() );
		}
	}

	private String waitForAuthorizationCode( String authUrl ) throws IOException
	{
		Desktop.getDesktop().browse( URI.create( authUrl ) );
		String code = project.getPropertyValue( "code" );
		long startTime = System.currentTimeMillis();
		while( code ==null && System.currentTimeMillis()-startTime < 30000)
		{
			code = project.getPropertyValue( "code" );
		}
		project.setPropertyValue( "code", null );
		return code;
	}
}
