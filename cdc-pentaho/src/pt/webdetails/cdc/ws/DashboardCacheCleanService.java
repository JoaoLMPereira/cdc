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

package pt.webdetails.cdc.ws;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;

public class DashboardCacheCleanService {

  private static Log logger = LogFactory.getLog( DashboardCacheCleanService.class );

  /**
   * Find all .cdfde files in solution
   *
   * @return All .cdfde files in solution.
   */
  public static String getDashboardsList() {

    try {
      File baseDir = new File( PentahoSystem.getApplicationContext().getSolutionPath( "" ) );

      List<String> files = findDashboardsRecursively( "/", baseDir );
      return Result.getOK( files.toArray( new String[files.size()] ) ).toString();
    } catch ( Exception e ) {
      return Result.getFromException( e ).toString();
    }

  }

  /**
   * Clear all CDA data sources referenced by a given dashboard.
   *
   * @param dashboard The full path (from solution) of the dashboard
   * @return Number of entries cleared from cache.
   */
  public String clearDashboard( String dashboard ) {

    SecurityAssertions.assertIsAdmin();

    InterPluginCall listCdaSources = new InterPluginCall( InterPluginCall.CDE, "listcdasources" );
    listCdaSources.putParameter( "dashboard", dashboard );

    try {
      JSONArray results = new JSONArray( listCdaSources.call() );

      InterPluginCall cacheMonitorCall = new InterPluginCall( InterPluginCall.CDA, "cacheMonitor" );
      cacheMonitorCall.putParameter( "method", "removeAll" );

      for ( int i = 0; i < results.length(); i++ ) {
        JSONObject dataSource = (JSONObject) results.get( i );
        cacheMonitorCall.putParameter( "cdaSettingsId", dataSource.getString( "cdaSettingsId" ) );
        if ( dataSource.has( "dataAccessId" ) ) {
          cacheMonitorCall.putParameter( "dataAccessId", dataSource.getString( "dataAccessId" ) );
        } else {
          cacheMonitorCall.putParameter( "dataAccessId", null );
        }

        JSONObject itemsCleared = new JSONObject( cacheMonitorCall.callInPluginClassLoader() );
        if ( StringUtils.equalsIgnoreCase( itemsCleared.getString( "status" ), "ok" ) ) {
          int numCleared = itemsCleared.getInt( "result" );
          dataSource.put( "cleared", numCleared );
        }
      }
      return Result.getOK( results ).toString();
    } catch ( JSONException e ) {
      return Result.getFromException( e ).toString();
    }

  }

  private static List<String> findDashboardsRecursively( String baseDir, File dir ) {

    ArrayList<String> files = new ArrayList<String>();
    // skip system file (templates)
    if ( baseDir.equals( "/system/" ) ) {
      return files;
    }

    if ( dir.isDirectory() ) {
      try {
        for ( File file : dir.listFiles() ) {
          if ( file.isFile() && StringUtils.endsWith( file.getName(), ".cdfde" ) ) {
            files.add( baseDir + file.getName() );
          } else if ( file.isDirectory() ) {
            files.addAll( findDashboardsRecursively( baseDir + file.getName() + "/", file ) );
          }
        }
      } catch ( SecurityException e ) {
        logger.error( "Access denied for " + dir.getAbsolutePath() );
      }
    }
    return files;

  }

}
