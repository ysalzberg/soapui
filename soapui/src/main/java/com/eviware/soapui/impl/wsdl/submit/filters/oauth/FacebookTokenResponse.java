package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.token.BasicOAuthToken;
import org.apache.oltu.oauth2.common.token.OAuthToken;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;

/**
 * @author Anders Jaensson
 */
public class FacebookTokenResponse extends OAuthAccessTokenResponse
{

	private static final String OAUTH_EXPIRES = "expires";

	public String getAccessToken()
	{
		return getParam( OAuth.OAUTH_ACCESS_TOKEN );
	}

	public Long getExpiresIn()
	{
		String value = getParam( OAUTH_EXPIRES );
		return value == null ? null : Long.valueOf( value );
	}

	// Facebook does not support refresh tokens
	public String getRefreshToken()
	{
		return null;
	}

	public String getScope()
	{
		return getParam( OAuth.OAUTH_SCOPE );
	}

	public OAuthToken getOAuthToken()
	{
		return new BasicOAuthToken( getAccessToken(), getExpiresIn(), getRefreshToken(), getScope() );
	}

	protected void setBody( String body )
	{
		this.body = body;
		parameters = OAuthUtils.decodeForm( body );
	}

	protected void setContentType( String contentType )
	{
		this.contentType = contentType;
	}

	protected void setResponseCode( int code )
	{
		this.responseCode = code;
	}
}
