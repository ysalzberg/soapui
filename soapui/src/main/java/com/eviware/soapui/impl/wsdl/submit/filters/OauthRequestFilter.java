package com.eviware.soapui.impl.wsdl.submit.filters;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import org.apache.http.HttpRequest;

import java.awt.*;
import java.net.URI;

/**
 * @author Anders Jaensson
 */
public class OauthRequestFilter extends AbstractRequestFilter
{

	private String tokenSecret;
	private String token;

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
				"https://api.twitter.com/oauth/authorize" );

		try
		{
			if( StringUtils.isNullOrEmpty( token ) || StringUtils.isNullOrEmpty( tokenSecret ) )
			{
				String authUrl = provider.retrieveRequestToken( consumer, OAuth.OUT_OF_BAND );
				Desktop.getDesktop().browse( URI.create( authUrl ) );
				String code = UISupport.getDialogs().prompt( "Please enter the code", "title" );
				provider.retrieveAccessToken( consumer, code );

				token = consumer.getToken();
				tokenSecret = consumer.getTokenSecret();
			}
			else
			{
				consumer.setTokenWithSecret( token, tokenSecret );
			}
			HttpRequest httpRequest = ( HttpRequest )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );
			// sign the request
			consumer.sign( httpRequest );
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}
	}
}
