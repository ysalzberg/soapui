package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.filters.AbstractRequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
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
 * @author Anders Jaensson
 */
public class SingpostOAuthRequestFilter extends AbstractRequestFilter
{
	private static final CommonsHttpOAuthProvider twitterOAuthProvider = new CommonsHttpOAuthProvider(
			"https://api.twitter.com/oauth/request_token",
			"https://api.twitter.com/oauth/access_token",
			"https://api.twitter.com/oauth/authorize" );

	public static final String CALLBACK_URL = "http://localhost:8080";

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
				retrieveAccessToken( consumer, twitterOAuthProvider );
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
		String oauthConsumerKey = project.getPropertyValue( "oauth_consumer_key" );
		String oauthConsumerSecret = project.getPropertyValue( "oauth_consumer_secret" );

		return new CommonsHttpOAuthConsumer(
				oauthConsumerKey,
				oauthConsumerSecret );
	}

	private void useStoredAccessToken( OAuthConsumer consumer )
	{
		consumer.setTokenWithSecret( accessToken, accessTokenSecret );
	}

	private void rememberAccessToken( OAuthConsumer consumer ) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
	{
		accessToken = consumer.getToken();
		accessTokenSecret = consumer.getTokenSecret();
	}

	private void retrieveAccessToken( OAuthConsumer consumer, OAuthProvider provider ) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
	{
		String authUrl = provider.retrieveRequestToken( consumer, CALLBACK_URL );
		String code = askUserForCode( authUrl );
		provider.retrieveAccessToken( consumer, code );
	}

	private void signRequest( SubmitContext context, OAuthConsumer consumer ) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException
	{
		// sign the request
		HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
		consumer.sign( httpRequest );
	}

	private String askUserForCode( String authUrl ) throws IOException
	{
		Desktop.getDesktop().browse( URI.create( authUrl ) );
		return UISupport.getDialogs().prompt( "Please enter the code", "title" );
	}
}
