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

	private String tokenSecret;
	private String token;

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		try
		{
			Project project = ModelSupport.getModelItemProject( request );

			// initialize OAuth consumer
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
					project.getPropertyValue( "oauth_consumer_key" ),
					project.getPropertyValue( "oauth_consumer_secret" ) );

			if( StringUtils.isNullOrEmpty( token ) || StringUtils.isNullOrEmpty( tokenSecret ) )
			{
				storeToken( consumer, twitterOAuthProvider );
			}
			else
			{
				useStoredToken( consumer );
			}
			signRequest( context, consumer );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}

	private void useStoredToken( OAuthConsumer consumer )
	{
		consumer.setTokenWithSecret( token, tokenSecret );
	}

	private void storeToken( OAuthConsumer consumer, OAuthProvider provider ) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
	{
		retrieveAccessToken( consumer, provider );

		token = consumer.getToken();
		tokenSecret = consumer.getTokenSecret();
	}

	private void retrieveAccessToken( OAuthConsumer consumer, OAuthProvider provider ) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
	{
		String authUrl = provider.retrieveRequestToken( consumer, "http://localhost:8080" );
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
