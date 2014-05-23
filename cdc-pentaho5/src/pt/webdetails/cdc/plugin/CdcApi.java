/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cdc.plugin;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONException;
import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.ws.MondrianCacheCleanService;
import pt.webdetails.cpf.plugincall.api.IPluginCall;
import pt.webdetails.cpf.PluginEnvironment;
import pt.webdetails.cpf.VersionChecker;
import pt.webdetails.cpf.plugin.CorePlugin;
import pt.webdetails.cpf.plugincall.base.CallParameters;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Path("cdc/api")
public class CdcApi {

  private static final long serialVersionUID = 1L;
  private static final Log logger = LogFactory.getLog( CdcApi.class );

  private static final String UI_PATH = "cdc/presentation/";

  public String getPluginName() {
    return "cdc";
  }

  @GET
  @Path("/ping")
  public String ping() {
    return "Pong: CDC successfully registered with the platform";
  }

  //TODO: testing
  @GET
  @Path( "/start" )
  public String start( @Context HttpServletResponse response ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"start\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    HazelcastManager.INSTANCE.init();
    return "OK?";
  }

  @GET
  @Path("/stop")
  public String stop( @Context HttpServletResponse response ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"stop\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    HazelcastManager.INSTANCE.tearDown();
    return "OK?";
  }

  @GET
  @Path("/recoverMondrianCache")
  public String recoverMondrianCache( @Context HttpServletResponse response ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"recoverMondrianCache\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    MondrianCacheCleanService.loadMondrianCatalogs();
    int cnt = HazelcastManager.INSTANCE.reloadMondrianCache();
    return "reload: " + cnt;
  }

  @GET
  @Path("/reloadMondrianCache")
  public String reloadMondrianCache( @Context HttpServletResponse response ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"reloadMondrianCache\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    int cnt = HazelcastManager.INSTANCE.reloadMondrianCache();
    return "reload: " + cnt;
  }

  @GET
  @Path("/home")
  public String home( @Context HttpServletRequest request ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"home\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    Map<String, Object> params = getRenderRequestParameters( "cdcHome.wcdf", request );
    return renderInCde( params );
  }

  @GET
  @Path("/clusterInfo")
  public String clusterInfo( @Context HttpServletRequest request ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"clusterInfo\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    Map<String, Object> params = getRenderRequestParameters( "cdcClusterInfo.wcdf", request );
    return renderInCde( params );
  }

  @GET
  @Path("/cacheInfo")
  public String cacheInfo( @Context HttpServletRequest request ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"cacheInfo\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    Map<String, Object> params = getRenderRequestParameters( "cdcCacheInfo.wcdf", request );
    return renderInCde( params );
  }

  @GET
  @Path("/settings")
  public String settings( @Context HttpServletRequest request ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"settings\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    Map<String, Object> params = getRenderRequestParameters( "cdcSettings.wcdf", request );
    return renderInCde( params );
  }

  @GET
  @Path( "/cacheClean" )
  public String cacheClean( @Context HttpServletRequest request ) throws Exception {
    if ( !CdcUtil.isCurrentUserAdmin() ) {
      logger.warn( "\"cacheClean\" endpoint called by a non admin user. Aborting" );
      return "User has no access to this endpoint";
    }
    Map<String, Object> params = getRenderRequestParameters( "cdcCacheClean.wcdf", request );
    return renderInCde( params );
  }

  @GET
  @Path("/about")
  public String about( @Context HttpServletRequest request ) throws Exception {
    return renderInCde( getRenderRequestParameters( "cdcAbout.wcdf", request ) );
  }

  @GET
  @Path( "/checkVersion" )
  public String checkVersion() throws IOException, JSONException {
    return getVersionChecker().checkVersion().toJSON().toString();
  }

  @GET
  @Path("/getVersion")
  public String getVersion( @Context HttpServletResponse response ) throws IOException, JSONException {
    return getVersionChecker().getVersion();
  }

  /**
   * Set up parameters to render a dashboard from the presentation layer
   *
   * @param dashboardName
   * @return
   */
  private Map<String, Object> getRenderRequestParameters( String dashboardName, HttpServletRequest request ) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put( "solution", "system" );
    params.put( "path", UI_PATH );
    params.put( "file", dashboardName );
    params.put( "bypassCache", "true" );
    params.put( "absolute", "false" );
    params.put( "inferScheme", "false" );

    //add request parameters
    @SuppressWarnings("unchecked")//should always be String
        Enumeration<String> originalParams = request.getParameterNames();
    // Iterate and put the values there
    while ( originalParams.hasMoreElements() ) {
      String originalParam = originalParams.nextElement();
      params.put( originalParam, request.getParameter( originalParam ) );
    }

    return params;
  }

  /**
   * Display a CDE dashboard
   *
   * @param params
   * @throws IOException
   */
  private String renderInCde( Map<String, Object> params ) throws Exception {
    CallParameters parameters = new CallParameters();
    Iterator<String> it = params.keySet().iterator();
    while ( it.hasNext() ) {
      String key = it.next();
      Object value = params.get( key );
      parameters.put( key, value.toString() );
    }
    IPluginCall pluginCall = PluginEnvironment.env().getPluginCall( CorePlugin.CDE.getId(), "renderer", "render" );
    String returnVal = pluginCall.call( parameters.getParameters() );
    return returnVal;
  }

  public VersionChecker getVersionChecker() {

    return new VersionChecker( CdcConfig.getConfig() ) {

      @Override
      protected String getVersionCheckUrl( VersionChecker.Branch branch ) {
        switch ( branch ) {
          case TRUNK:
            return " http://ci.pentaho.com/job/pentaho-cdc/lastSuccessfulBuild/artifact/cdc-pentaho5/dist/marketplace.xml";
          case STABLE:
            return "http://ci.analytical-labs"
                + ".com/job/Webdetails-CDC-Release/lastSuccessfulBuild/artifact/dist/marketplace.xml";
          default:
            return null;
        }

      }

    };
  }

}
