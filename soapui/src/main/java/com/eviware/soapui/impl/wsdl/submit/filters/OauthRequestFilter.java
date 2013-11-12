package com.eviware.soapui.impl.wsdl.submit.filters;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.http.HttpRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Anders Jaensson
 */
public class OauthRequestFilter extends AbstractRequestFilter
{

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		Project project = ModelSupport.getModelItemProject( request );


		// initialize OAuth consumer
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				project.getPropertyValue( "oauth_consumer_key" ),
				project.getPropertyValue( "oauth_consumer_secret" ) );

		OAuthProvider provider = new CommonsHttpOAuthProvider(
				"https://api.twitter.com/oauth/request_token",
				"https://api.twitter.com/oauth/access_token",
				"https://api.twitter.com/oauth/authorize");

		try
		{
			String authUrl = provider.retrieveRequestToken( consumer, OAuth.OUT_OF_BAND );

			System.out.println("Now visit:\n" + authUrl + "\n... and grant this app authorization");
			System.out.println("Enter the verification code and hit ENTER when you're done");

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String code = br.readLine();
			provider.retrieveAccessToken( consumer, code );

			HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
			// sign the request
			consumer.sign( httpRequest );
		}
		catch( OAuthMessageSignerException e )
		{
			SoapUI.logError( e );
		}
		catch( OAuthExpectationFailedException e )
		{
			SoapUI.logError( e );
		}
		catch( OAuthCommunicationException e )
		{
			SoapUI.logError( e );
		}
		catch( OAuthNotAuthorizedException e )
		{
			SoapUI.logError( e );
		}
		catch( IOException e )
		{
			SoapUI.logError( e );
		}
	}
}
