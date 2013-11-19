/*
 * SoapUI, copyright (C) 2004-2013 smartbear.com
 *
 * SoapUI is free software; you can redistribute it and/or modify it under the
 * terms of version 2.1 of the GNU Lesser General Public License as published by
 * the Free Software Foundation.
 *
 * SoapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.submit.filters.oauth;

import com.eviware.soapui.impl.wsdl.mock.WsdlMockRequest;
import com.google.common.base.Splitter;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Prakash
 * Date: 2013-11-18
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public class OltuAuthorizationServer
{
	public void accessToken( HttpServletRequest request, HttpServletResponse response )
	{

		OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl( new MD5Generator() );

		try
		{
			OAuthTokenRequest oauthRequest = new OAuthTokenRequest( request );
			// some code

			String accessToken = oauthIssuerImpl.accessToken();
			String refreshToken = oauthIssuerImpl.refreshToken();

			// some code


			OAuthResponse r = OAuthASResponse
					.tokenResponse( HttpServletResponse.SC_OK )
					.setAccessToken( accessToken )
					.setExpiresIn( "3600" )
					.setRefreshToken( refreshToken )
					.buildJSONMessage();

			response.setStatus( r.getResponseStatus() );
			PrintWriter pw = response.getWriter();
			pw.print( r.getBody() );
			pw.flush();
			pw.close();

			//if something goes wrong
		}
		catch( OAuthProblemException ex )
		{

			try
			{
				OAuthResponse r = OAuthResponse
						.errorResponse( 401 )
						.error( ex )
						.buildJSONMessage();
				response.setStatus( r.getResponseStatus() );

				PrintWriter pw = response.getWriter();
				pw.print( r.getBody() );
				pw.flush();
				pw.close();

				response.sendError( 401 );
			}
			catch( Exception e )
			{
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	protected void authorize( HttpServletRequest request, HttpServletResponse response )
			throws ServletException, IOException
	{

		try
		{
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest( request );
			//build OAuth response
			OAuthResponse resp = OAuthASResponse
					.authorizationResponse( request, HttpServletResponse.SC_FOUND )
					.setCode( "code" )
					.location( oauthRequest.getRedirectURI() )
					.buildQueryMessage();

			response.sendRedirect( resp.getLocationUri() );

			//if something goes wrong
		}
		catch( OAuthProblemException ex )
		{
			OAuthResponse resp = null;
			try
			{
				resp = OAuthASResponse
						.errorResponse( HttpServletResponse.SC_FOUND )
						.error( ex )
						.location( ex.getRedirectUri() )
						.buildQueryMessage();
			}
			catch( OAuthSystemException e )
			{
				e.printStackTrace();
				return;
			}

			response.sendRedirect( resp.getLocationUri() );
		}
		catch( OAuthSystemException e )
		{
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	public void temp()
	{
		/*WsdlMockRequest mockRequest= null;

		String authCode = "123authorizationcode123";
		String  accessToken = "123acesstoken123";
		String refreshToken = "123refreshtoken123";
		String uri = mockRequest.getHttpRequest().getRequestURI();
		if(uri.contains("code="))
		{
			String  queryString= mockRequest.getHttpRequest().getQueryString();
			log.info(queryString)
			Map<String, String> split = Splitter.on( '&' ).withKeyValueSeparator( "=" ).split( queryString );


			//mockRunner.mockService.project.setPropertyValue("code", split.get("code"))
			//log.info("Code from project property: " + mockRunner.mockService.project.getPropertyValue("code"));
			log.info("Code: " + split.get("code"));
		} else if(uri.contains("/authorize"))
	{
		log.info("Inside authorization") ;
		try {
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest( mockRequest.getHttpRequest());
			//build OAuth response
			OAuthResponse resp = OAuthASResponse
					.authorizationResponse(mockRequest.getHttpRequest(), HttpServletResponse.SC_FOUND)
					.setCode(authCode)
					.location(oauthRequest.getRedirectURI())
					.buildQueryMessage();

			mockRequest.getHttpResponse().sendRedirect(resp.getLocationUri());

			//if something goes wrong
		} catch(OAuthProblemException ex) {

			try
			{
				OAuthResponse resp = OAuthASResponse
						.errorResponse( HttpServletResponse.SC_FOUND )
						.error( ex )
						.location( ex.getRedirectUri() )
						.buildQueryMessage();
				mockRequest.getHttpResponse().sendRedirect( resp.getLocationUri() );
			}
			catch( Exception e )
			{
				log.info(e);
			}
		}
		catch( Exception e )
		{
			log.info(e);
		}
	} else if(uri.contains("/access_token"))
	{
		log.info("Inside access token")
		try {
			OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
			OAuthResponse r = OAuthASResponse
					.tokenResponse(HttpServletResponse.SC_OK)
					.setAccessToken(accessToken)
					.setExpiresIn("3600")
					.setRefreshToken(refreshToken)
					.buildJSONMessage();

			mockRequest.getHttpResponse().setStatus(r.getResponseStatus());
			PrintWriter pw = mockRequest.getHttpResponse().getWriter();
			pw.print(r.getBody());
			pw.flush();
			pw.close();

			//if something goes wrong
		} catch(OAuthProblemException ex) {

			try
			{
				OAuthResponse r = OAuthResponse
						.errorResponse( 401 )
						.error(ex)
						.buildJSONMessage();
				mockRequest.getHttpResponse().setStatus(r.getResponseStatus());

				PrintWriter pw = mockRequest.getHttpResponse().getWriter();
				pw.print(r.getBody());
				pw.flush();
				pw.close();

				mockRequest.getHttpResponse().sendError(401);
			}
			catch( Exception e )
			{
				log.info(e);
			}
		}
		catch( Exception e )
		{
			log.info(e);
		}
	}
      */
	}
}
