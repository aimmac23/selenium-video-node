package com.aimmac23.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

import java.io.IOException;
import java.net.ProxySelector;
import java.util.concurrent.TimeUnit;

public class HttpClientFactory
{
   private final CloseableHttpClient httpClient;

   private static final int THREE_HOUR_TIMEOUT_MS = 3 * 60 * 60 * 1000;
   private static final int TWO_MINUTE_TIMEOUT_MS = 2 * 60 * 1000;

   private final HttpClientConnectionManager connectionManager = getConnectionManager();

   public HttpClientFactory()
   {
      SocketConfig socketConfig =
         SocketConfig.custom()
            .setSoReuseAddress( true )
            .setSoTimeout( THREE_HOUR_TIMEOUT_MS )
            .build();

      RequestConfig requestConfig =
         RequestConfig.custom()
            .setStaleConnectionCheckEnabled( true )
            .setConnectTimeout( TWO_MINUTE_TIMEOUT_MS )
            .setSocketTimeout( THREE_HOUR_TIMEOUT_MS )
            .build();

      HttpRoutePlanner routePlanner =
         new SystemDefaultRoutePlanner( new DefaultSchemePortResolver(), ProxySelector.getDefault() );

      HttpClientBuilder builder =
         HttpClientBuilder.create()
            .setConnectionManager( getConnectionManager() )
            .setDefaultSocketConfig( socketConfig )
            .setDefaultRequestConfig( requestConfig )
            .setRoutePlanner( routePlanner );

      httpClient = builder.build();
   }


   protected static HttpClientConnectionManager getConnectionManager()
   {
      Registry< ConnectionSocketFactory > socketFactoryRegistry =
         RegistryBuilder.< ConnectionSocketFactory >create()
           .register( "http", PlainConnectionSocketFactory.getSocketFactory() )
           .register( "https", SSLConnectionSocketFactory.getSocketFactory() )
           .build();

      PoolingHttpClientConnectionManager poolingManager =
         new PoolingHttpClientConnectionManager( socketFactoryRegistry );

      poolingManager.setMaxTotal( 2000 );
      poolingManager.setDefaultMaxPerRoute( 2000 );

      return poolingManager;
   }

   public HttpClient getHttpClient()
   {
      return httpClient;
   }

   public void close()
   {
      try
      {
         httpClient.close();
      }
      catch ( IOException ioe )
      {
         throw new RuntimeException( ioe );
      }

      connectionManager.shutdown();
   }

   void closeIdleConnections()
   {
      connectionManager.closeIdleConnections( 0, TimeUnit.SECONDS );
   }
}
