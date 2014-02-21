package com.eviware.soapui.impl.wsdl.submit.filters;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.OAuth2Profile;
import com.eviware.soapui.impl.rest.OAuth2ProfileContainer;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.actions.oauth.OAuth2ClientFacade;
import com.eviware.soapui.impl.rest.actions.oauth.OAuth2Exception;
import com.eviware.soapui.impl.rest.actions.oauth.OltuOAuth2ClientFacade;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.TimeUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

import static com.eviware.soapui.config.CredentialsConfig.AuthType.O_AUTH_2;

public class OAuth2RequestFilter extends AbstractRequestFilter
{
	protected final static Logger log = Logger.getLogger( OAuth2RequestFilter.class );

	@Override
	public void filterRestRequest( SubmitContext context, RestRequestInterface request )
	{

		HttpRequestBase httpMethod = ( HttpRequestBase )context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );

		OAuth2ProfileContainer profileContainer = request.getResource().getService().getProject()
				.getOAuth2ProfileContainer();

		if( !profileContainer.getOAuth2ProfileList().isEmpty() && O_AUTH_2.toString().equals( request.getAuthType() ) )
		{
			OAuth2Profile profile = profileContainer.getOAuth2ProfileList().get( 0 );
			if( StringUtils.isNullOrEmpty( profile.getAccessToken() ) )
			{
				return;
			}
			OAuth2ClientFacade oAuth2Client = getOAuth2ClientFacade();

			if( profile.shouldRefreshAccessTokenAutomatically() && accessTokenIsExpired( profile ))
			{
				refreshAccessToken( profile, oAuth2Client );
			}
			oAuth2Client.applyAccessToken( profile, httpMethod, request.getRequestContent() );
		}
	}

	protected OAuth2ClientFacade getOAuth2ClientFacade()
	{
		return new OltuOAuth2ClientFacade();
	}

	private boolean accessTokenIsExpired( OAuth2Profile profile )
	{
		long currentTime = TimeUtils.getCurrentTimeInSeconds();
		long issuedTime = profile.getAccessTokenIssuedTime();
		long expirationTime = profile.getAccessTokenExpirationTime();

		if( issuedTime <= 0 || expirationTime <= 0 )
		{
			return false;
		}

		return expirationTime < currentTime - issuedTime;
	}

	private void refreshAccessToken( OAuth2Profile profile, OAuth2ClientFacade oAuth2Client )
	{
		try
		{
			log.info( "The access token has expired, trying to refresh it." );

			oAuth2Client.refreshAccessToken( profile );

			log.info( "The access token has been refreshed successfully." );
		}
		catch( Exception e )
		{
			//Propogate it up so that it is shown as a failure message in test case log
			throw new RuntimeException( "Unable to refresh expired access token.", e );
		}
	}
}
