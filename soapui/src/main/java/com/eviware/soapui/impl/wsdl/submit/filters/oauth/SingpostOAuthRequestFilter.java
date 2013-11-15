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
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.http.HttpRequest;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * OAuth 1.0a three-legged flow for twitter (https://github.com/Mashape/mashape-oauth/blob/master/FLOWS.md#oauth-10a-three-legged).
 * 1. If no existing access token and access token secret, application retrieves a request token
 * 1a. The request token contains a url to which the user is redirected
 * 1b. The user grants access to the application
 * 1c. User is redirected back with request token and verifier
 * 1d. Request token and verifier and exchanged for an access token and access token secret
 * 2. The access token and the access token secret are used to sign requests
 *
 * @author Anders Jaensson
 */
public class SingpostOAuthRequestFilter extends AbstractRequestFilter
{
	private static final CommonsHttpOAuthProvider twitterOAuthProvider = new CommonsHttpOAuthProvider(
			"https://api.twitter.com/oauth/request_token",
			"https://api.twitter.com/oauth/access_token",
			"https://api.twitter.com/oauth/authorize",
			HttpClientSupport.getHttpClient() );

	private static final String CALLBACK_URL = "http://localhost:8080";
	public static final String PROPERTY_NAME_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
	public static final String PROPERTY_NAME_OAUTH_CONSUMER_SECRET = "oauth_consumer_secret";

	private String accessToken;
	private String accessTokenSecret;

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		try
		{
			OAuthConsumer consumer = createOAuthConsumer( request );

			if( StringUtils.isNullOrEmpty( accessToken ) || StringUtils.isNullOrEmpty( accessTokenSecret ) )
			{
				retrieveAccessToken( consumer, twitterOAuthProvider);
				rememberAccessToken( consumer );
			}
			else
			{
				useStoredAccessToken( consumer );
			}
			signRequest( context, consumer );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}

	private OAuthConsumer createOAuthConsumer( RestRequestInterface request )
	{
		Project project = ModelSupport.getModelItemProject( request );

		// initialize OAuth consumer
		String oauthConsumerKey = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_KEY );
		String oauthConsumerSecret = project.getPropertyValue( PROPERTY_NAME_OAUTH_CONSUMER_SECRET );

		return new CommonsHttpOAuthConsumer(
				oauthConsumerKey,
				oauthConsumerSecret );
	}

	private void useStoredAccessToken( OAuthConsumer consumer )
	{
		consumer.setTokenWithSecret( accessToken, accessTokenSecret );
	}

	private void rememberAccessToken( OAuthConsumer consumer ) throws Exception
	{
		accessToken = consumer.getToken();
		accessTokenSecret = consumer.getTokenSecret();
	}

	private void retrieveAccessToken( OAuthConsumer consumer, OAuthProvider provider ) throws Exception
	{
		String authUrl = provider.retrieveRequestToken( consumer, CALLBACK_URL );
		String code = askUserForCode( authUrl );
		provider.retrieveAccessToken( consumer, code );
	}

	private void signRequest( SubmitContext context, OAuthConsumer consumer ) throws Exception
	{
		// sign the request
		HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
		consumer.sign( httpRequest );
	}

	private String askUserForCode( String authUrl ) throws IOException
	{
		Desktop.getDesktop().browse( URI.create( authUrl ) );
		return UISupport.getDialogs().prompt( "Please enter the authorization code", "title" );
	}
}
