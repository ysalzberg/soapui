package com.eviware.soapui.impl.wsdl.submit.filters;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpRequest;

/**
 * @author Anders Jaensson
 */
public class OauthRequestProvidedAccessTokenFilter extends AbstractRequestFilter
{
	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{
		Project project = ModelSupport.getModelItemProject( request );

		try
		{
			// initialize OAuth consumer
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
					project.getPropertyValue( "oauth_consumer_key" ),
					project.getPropertyValue( "oauth_consumer_secret" ) );

			consumer.setTokenWithSecret(
					project.getPropertyValue( "oauth_access_token" ),
					project.getPropertyValue( "oauth_access_token_secret" ) );

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
	}
}
